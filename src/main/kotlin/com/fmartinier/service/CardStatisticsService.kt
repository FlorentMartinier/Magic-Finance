package com.fmartinier.service

import com.fmartinier.domain.Card
import com.fmartinier.domain.PriceHistory
import com.fmartinier.repository.CardRepository
import com.fmartinier.repository.PriceHistoryRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import kotlin.math.sqrt

@Service
class CardStatisticsService(
    private val cardRepository: CardRepository,
    private val priceHistoryRepository: PriceHistoryRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val BATCH_SIZE = 1000
    }

    /**
     * Calcule et met à jour les indicateurs de toutes les cartes par lots de 1000.
     */
    fun calculateMetricsForAllCards() {
        logger.info("Starting global statistics and metrics calculation...")
        val startTime = System.currentTimeMillis()

        // 1. Récupération de tous les IDs de cartes de la BDD pour éviter de charger toutes les entités en mémoire
        // Si ton projet grandit, tu pourras paginer cette sélection.
        val allCardIds = cardRepository.findAll().map { it.scryfallId }
        logger.info("Found ${allCardIds.size} cards to analyze.")

        // 2. Découpage en lots de 1000
        val batches = allCardIds.chunked(BATCH_SIZE)
        var processedCount = 0

        for (batch in batches) {
            try {
                processBatch(batch)
                processedCount += batch.size
                if (processedCount % 5000 == 0 || processedCount == allCardIds.size) {
                    logger.info("Calculated metrics for $processedCount/${allCardIds.size} cards...")
                }
            } catch (e: Exception) {
                logger.error("Error calculating statistics for batch starting with ID ${batch.firstOrNull()}: ${e.message}", e)
            }
        }

        val duration = (System.currentTimeMillis() - startTime) / 1000
        logger.info("Metrics calculation completed in $duration seconds!")
    }

    @Transactional
    protected fun processBatch(scryfallIds: List<String>) {
        val today = LocalDate.now()
        val thirtyDaysAgo = today.minusDays(30)

        // 1. SELECT unique pour récupérer les 1000 cartes du lot
        val cardsMap = cardRepository.findAllById(scryfallIds).associateBy { it.scryfallId }

        // 2. SELECT unique pour récupérer l'historique des 30 derniers jours de ces 1000 cartes d'un coup
        val histories = priceHistoryRepository.findByCardScryfallIdInAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
            scryfallIds, thirtyDaysAgo
        )

        // 3. Regroupement des historiques par carte en mémoire (Map<ScryfallId, List<PriceHistory>>)
        val historyGroupedByCard = histories.groupBy { it.card.scryfallId }

        val cardsToSave = mutableListOf<Card>()

        for ((scryfallId, card) in cardsMap) {
            val cardHistory = historyGroupedByCard[scryfallId] ?: emptyList()
            if (cardHistory.isEmpty()) continue

            // On filtre pour analyser en priorité les prix non-foil (ou foil s'il n'y a que ça)
            val nonFoilPrices = cardHistory.filter { !it.isFoil }
            val pricesToAnalyze = nonFoilPrices.ifEmpty { cardHistory }

            val currentPrice = card.currentPrice ?: continue

            // Calculs locaux en mémoire
            card.priceChange7d = calculatePercentageChange(currentPrice, today.minusDays(7), pricesToAnalyze)
            card.priceChange30d = calculatePercentageChange(currentPrice, today.minusDays(30), pricesToAnalyze)
            card.volatilityScore = calculateVolatilityScore(pricesToAnalyze)

            cardsToSave.add(card)
        }

        // 4. Envoi des mises à jour en un seul lot d'écriture
        if (cardsToSave.isNotEmpty()) {
            cardRepository.saveAll(cardsToSave)
        }
    }

    private fun calculatePercentageChange(
        currentPrice: BigDecimal,
        targetDate: LocalDate,
        history: List<PriceHistory>
    ): BigDecimal {
        val historicalPrice = history
            .filter { !it.priceDate.isAfter(targetDate) }
            .maxByOrNull { it.priceDate }?.priceEur
            ?: history.firstOrNull()?.priceEur
            ?: return BigDecimal.ZERO

        if (historicalPrice.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO

        return currentPrice.subtract(historicalPrice)
            .divide(historicalPrice, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal("100"))
            .setScale(2, RoundingMode.HALF_UP)
    }

    private fun calculateVolatilityScore(history: List<PriceHistory>): BigDecimal {
        if (history.size < 2) return BigDecimal.ZERO

        val doublePrices = history.map { it.priceEur.toDouble() }
        val mean = doublePrices.average()
        val sumOfSquaredDifferences = doublePrices.sumOf { (it - mean) * (it - mean) }
        val standardDeviation = sqrt(sumOfSquaredDifferences / (history.size - 1))

        return BigDecimal(standardDeviation).setScale(4, RoundingMode.HALF_UP)
    }
}