package com.fmartinier.service

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.ObjectMapper
import com.fmartinier.domain.Card
import com.fmartinier.domain.Price
import com.fmartinier.dto.ScryfallCardDto
import com.fmartinier.repository.CardRepository
import com.fmartinier.repository.PriceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import java.math.BigDecimal
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDate

@Service
class ScryfallStreamService(
    private val cardRepository: CardRepository,
    private val priceRepository: PriceRepository,
    private val cardStatisticsService: CardStatisticsService,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    // On instancie un client HTTP Java standard et performant
    private val httpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL) // Très important pour suivre les redirections de Scryfall vers ses CDN
        .build()

    companion object {
        private const val BATCH_SIZE = 1000
    }

    fun syncCardsAndPrices(bulkJsonUrl: String) {
        logger.info("Starting optimized batch synchronization from URL: $bulkJsonUrl")
        val startTime = System.currentTimeMillis()

        val request = HttpRequest.newBuilder()
            .uri(URI.create(bulkJsonUrl))
            .version(HttpClient.Version.HTTP_1_1) // <--- Force HTTP/1.1 pour éviter le RST_STREAM
            .header("User-Agent", "MagicFinance/1.0.0 (florent.martinier@free.fr; GitHub Portfolio)")
            .header("Accept", "application/json")
            .GET()
            .build()

        // On demande au HttpClient de nous retourner directement le body sous forme d'InputStream
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())

        if (response.statusCode() == 200) {
            response.body().use { inputStream ->
                parseAndStoreJsonStream(inputStream)
            }
        } else {
            throw IllegalStateException("Failed to download bulk file. HTTP Status: ${response.statusCode()}")
        }

        val duration = (System.currentTimeMillis() - startTime) / 1000
        logger.info("Synchronization completed in $duration seconds!")

        cardStatisticsService.calculateMetricsForAllCards()
    }

    private fun parseAndStoreJsonStream(inputStream: InputStream) {
        val jsonFactory = JsonFactory()
        val parser = jsonFactory.createParser(inputStream)
        parser.codec = objectMapper

        if (parser.nextToken() != JsonToken.START_ARRAY) {
            throw IllegalStateException("Expected JSON to start with an Array")
        }

        val currentBatch = mutableListOf<ScryfallCardDto>()
        var totalProcessed = 0

        while (parser.nextToken() != JsonToken.END_ARRAY) {
            if (parser.currentToken == JsonToken.START_OBJECT) {
                try {
                    val cardDto = parser.readValueAs(ScryfallCardDto::class.java)
                    currentBatch.add(cardDto)
                    totalProcessed++

                    // Quand le batch est plein, on l'envoie en BDD et on vide la liste
                    if (currentBatch.size >= BATCH_SIZE) {
                        processAndSaveBatch(currentBatch)
                        currentBatch.clear()
                        logger.info("Processed $totalProcessed cards...")
                    }
                } catch (e: Exception) {
                    logger.error("Error parsing card at index $totalProcessed: ${e.message}")
                }
            }
        }

        // Très important : traiter le dernier batch résiduel (qui fait souvent moins de 1000 éléments)
        if (currentBatch.isNotEmpty()) {
            processAndSaveBatch(currentBatch)
            logger.info("Processed remaining ${currentBatch.size} cards. Total: $totalProcessed")
        }

        parser.close()
    }

    @Transactional
    protected fun processAndSaveBatch(dtos: List<ScryfallCardDto>) {
        val today = LocalDate.now()
        val scryfallIds = dtos.map { it.id }

        val minimumPriceThreshold = BigDecimal("1.00")

        // 1. Récupération en une seule requête des cartes existantes
        val existingCardsMap = cardRepository.findAllById(scryfallIds)
            .associateBy { it.scryfallId }

        // 2. Récupération en une seule requête des historiques de prix du jour
        // Comme nous avons fusionné les prix, on a au maximum 1 ligne "Price" par carte pour aujourd'hui
        val existingHistoriesMap = priceRepository.findByCardScryfallIdInAndUpdatedAt(scryfallIds, today)
            .associateBy { it.card.scryfallId }

        val cardsToSave = mutableListOf<Card>()
        val historiesToSave = mutableListOf<Price>()

        for (dto in dtos) {
            val pricesDto = dto.prices

            // --- Extraction et filtrage des prix EUR (Normal, Foil, Etched) ---
            val rawEur = pricesDto?.eur?.toBigDecimalOrNull()
            val priceEur = if (rawEur != null && rawEur >= minimumPriceThreshold) rawEur else null

            val rawFoilEur = pricesDto?.eurFoil?.toBigDecimalOrNull()
            val priceFoilEur = if (rawFoilEur != null && rawFoilEur >= minimumPriceThreshold) rawFoilEur else null

            val rawEtchedEur = pricesDto?.eurEtched?.toBigDecimalOrNull()
            val priceEtchedEur = if (rawEtchedEur != null && rawEtchedEur >= minimumPriceThreshold) rawEtchedEur else null

            // --- Extraction et filtrage des prix USD (Normal, Foil, Etched) ---
            val rawUsd = pricesDto?.usd?.toBigDecimalOrNull()
            val priceUsd = if (rawUsd != null && rawUsd >= minimumPriceThreshold) rawUsd else null

            val rawFoilUsd = pricesDto?.usdFoil?.toBigDecimalOrNull()
            val priceFoilUsd = if (rawFoilUsd != null && rawFoilUsd >= minimumPriceThreshold) rawFoilUsd else null

            val rawEtchedUsd = pricesDto?.usdEtched?.toBigDecimalOrNull()
            val priceEtchedUsd = if (rawEtchedUsd != null && rawEtchedUsd >= minimumPriceThreshold) rawEtchedUsd else null

            // Détermination s'il y a au moins un prix valide à suivre
            val hasAnyValidPrice = priceEur != null || priceFoilEur != null || priceEtchedEur != null ||
                    priceUsd != null || priceFoilUsd != null || priceEtchedUsd != null

            // --- Gestion de l'entité Card ---
            val card = existingCardsMap[dto.id] ?: Card(
                scryfallId = dto.id,
                name = dto.name,
                imageUri = dto.imageUris?.normal
            )

            card.name = dto.name

            // Calcul optionnel des métriques globales (Min/Max basé par exemple sur le prix normal en EUR)
            val currentReferencePrice = priceEur ?: priceFoilEur
            if (currentReferencePrice != null) {
                card.currentPrice = currentReferencePrice
                if (card.maxPrice == null || currentReferencePrice > card.maxPrice) {
                    card.maxPrice = currentReferencePrice
                    card.maxPriceDate = today
                }
                if (card.minPrice == null || currentReferencePrice < card.minPrice) {
                    card.minPrice = currentReferencePrice
                    card.minPriceDate = today
                }
            }

            if (existingCardsMap[dto.id] != card) {
                cardsToSave.add(card)
            }

            // --- Gestion de l'entité Price (Fusionnée et unique par carte pour ce jour) ---
            if (hasAnyValidPrice) {
                val existingPrice = existingHistoriesMap[card.scryfallId]

                if (existingPrice == null) {
                    // Créer uniquement si l'historique n'existe pas déjà
                    historiesToSave.add(
                        Price(
                            card = card,
                            updatedAt = today,
                            priceEur = priceEur,
                            priceUsd = priceUsd,
                            priceFoilEur = priceFoilEur,
                            priceFoilUsd = priceFoilUsd,
                            priceEtchedEur = priceEtchedEur,
                            priceEtchedUsd = priceEtchedUsd
                        )
                    )
                }
            }
        }

        // 3. Sauvegarde groupée ultra-rapide
        if (cardsToSave.isNotEmpty()) {
            cardRepository.saveAll(cardsToSave)
        }
        if (historiesToSave.isNotEmpty()) {
            priceRepository.saveAll(historiesToSave)
        }
    }
}