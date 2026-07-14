package com.fmartinier.service

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.ObjectMapper
import com.fmartinier.domain.Card
import com.fmartinier.domain.PriceHistory
import com.fmartinier.dto.ScryfallCardDto
import com.fmartinier.repository.CardRepository
import com.fmartinier.repository.PriceHistoryRepository
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
import java.time.OffsetDateTime

@Service
class ScryfallStreamService(
    private val cardRepository: CardRepository,
    private val priceHistoryRepository: PriceHistoryRepository,
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

        // Seuil minimum de 1.00 €
        val minimumPriceThreshold = BigDecimal("1.00")

        // 1. Récupération en une seule requête des cartes existantes
        val existingCardsMap = cardRepository.findAllById(scryfallIds)
            .associateBy { it.scryfallId }

        // 2. Récupération en une seule requête des historiques du jour
        val existingHistories = priceHistoryRepository.findByCardScryfallIdInAndPriceDate(scryfallIds, today)
        val existingHistoriesKeys = existingHistories
            .map { "${it.card.scryfallId}_${it.isFoil}" }
            .toSet()

        val cardsToSave = mutableListOf<Card>()
        val historiesToSave = mutableListOf<PriceHistory>()

        for (dto in dtos) {
            // Extraction et filtrage immédiat si inférieur à 1€
            val rawNormalPrice = dto.prices?.eur?.toBigDecimalOrNull()
            val normalPrice = if (rawNormalPrice != null && rawNormalPrice >= minimumPriceThreshold) rawNormalPrice else null

            val rawFoilPrice = dto.prices?.eurFoil?.toBigDecimalOrNull()
            val foilPrice = if (rawFoilPrice != null && rawFoilPrice >= minimumPriceThreshold) rawFoilPrice else null

            // Si la carte n'a aucun prix supérieur ou égal à 1€, on peut choisir de ne pas la suivre en BDD
            // ou simplement de mettre à jour ses infos de base sans prix. Ici, on met à jour ses infos de base.
            val currentPrice = normalPrice ?: foilPrice

            val card = existingCardsMap[dto.id] ?: Card(
                scryfallId = dto.id,
                nameEn = dto.nameEn,
                nameFr = dto.nameFr,
                imageUrl = dto.imageUris?.normal
            )

            card.nameEn = dto.nameEn
            card.nameFr = dto.nameFr
            card.imageUrl = dto.imageUris?.normal
            card.updatedAt = OffsetDateTime.now()

            if (currentPrice != null) {
                card.currentPrice = currentPrice
                if (card.maxPrice == null || currentPrice > card.maxPrice) {
                    card.maxPrice = currentPrice
                    card.maxPriceDate = today
                }
                if (card.minPrice == null || currentPrice < card.minPrice) {
                    card.minPrice = currentPrice
                    card.minPriceDate = today
                }
            }

            cardsToSave.add(card)

            // Sauvegarde de l'historique uniquement pour les prix valides (déjà filtrés à >= 1€)
            if (normalPrice != null && !existingHistoriesKeys.contains("${card.scryfallId}_false")) {
                historiesToSave.add(
                    PriceHistory(card = card, isFoil = false, priceDate = today, priceEur = normalPrice)
                )
            }
            if (foilPrice != null && !existingHistoriesKeys.contains("${card.scryfallId}_true")) {
                historiesToSave.add(
                    PriceHistory(card = card, isFoil = true, priceDate = today, priceEur = foilPrice)
                )
            }
        }

        // 3. Sauvegarde groupée
        if (cardsToSave.isNotEmpty()) {
            cardRepository.saveAll(cardsToSave)
        }
        if (historiesToSave.isNotEmpty()) {
            priceHistoryRepository.saveAll(historiesToSave)
        }
    }
}