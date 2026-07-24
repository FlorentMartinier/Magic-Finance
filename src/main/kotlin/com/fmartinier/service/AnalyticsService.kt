package com.fmartinier.service

import com.fmartinier.dto.CardAnalyticsDto
import com.fmartinier.dto.CardSummaryDto
import com.fmartinier.dto.DashboardAnalyticsDto
import com.fmartinier.dto.PricePointDto
import com.fmartinier.dto.RecommendationDto
import com.fmartinier.repository.CardSummaryProjection
import com.fmartinier.repository.PriceRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import kotlin.math.abs

@Service
class AnalyticsService(
    private val priceRepository: PriceRepository,
    private val scryfallService: ScryfallService,
) {
    fun getCardAnalytics(scryfallId: String): CardAnalyticsDto {
        // 1. Appel SQL unique pour récuperer toutes les métriques de la carte
        val stats = priceRepository.findAnalyticsByScryfallId(scryfallId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune donnée de prix trouvée pour cette carte")

        // 2. Récupération de l'historique pour le graphique
        val history = priceRepository.findPriceHistoryByScryfallId(scryfallId)
            .map { PricePointDto(date = it.getDate(), price = it.getPriceEur() ?: 0.0) }

        val current = stats.getCurrentPrice() ?: 0.0
        val p24h = stats.getPrice24hAgo() ?: current
        val p7d = stats.getPrice7dAgo() ?: current
        val ma30 = stats.getMovingAverage30d() ?: current

        // 3. Calcul des variations en pourcentage (%)
        val change24h = calculatePercentageChange(current, p24h)
        val change7d = calculatePercentageChange(current, p7d)

        // 4. Génération de la recommandation d'achat/vente
        val recommendation = evaluateSignal(current, ma30, change24h)

        return CardAnalyticsDto(
            scryfallId = scryfallId,
            currentPrice = current,
            change24h = change24h,
            change7d = change7d,
            movingAverage30d = ma30,
            volatility = stats.getVolatility() ?: 0.0,
            allTimeHigh = stats.getAllTimeHigh() ?: current,
            allTimeLow = stats.getAllTimeLow() ?: current,
            recommendation = recommendation,
            priceHistory = history,
            purchaseUris = scryfallService.getPurchaseUris(scryfallId)
        )
    }

    private fun calculatePercentageChange(current: Double, previous: Double): Double {
        if (previous == 0.0) return 0.0
        val change = ((current - previous) / previous) * 100
        return "%.2f".format(change).replace(',', '.').toDouble()
    }

    private fun evaluateSignal(currentPrice: Double, ma30: Double, change24h: Double): RecommendationDto {
        if (ma30 == 0.0) {
            return RecommendationDto("HOLD", "WEAK", "Données insuffisantes pour établir une recommandation.")
        }

        val diffPercentage = ((currentPrice - ma30) / ma30) * 100

        return when {
            diffPercentage <= -10.0 -> RecommendationDto(
                action = "BUY",
                signalStrength = if (diffPercentage <= -20.0) "STRONG" else "MEDIUM",
                reason = "Le prix actuel est à ${"%.1f".format(abs(diffPercentage))}% sous la moyenne mobile à 30 jours."
            )

            diffPercentage >= 15.0 -> RecommendationDto(
                action = "SELL",
                signalStrength = if (diffPercentage >= 30.0) "STRONG" else "MEDIUM",
                reason = "Le prix actuel est à ${"%.1f".format(diffPercentage)}% au-dessus de sa moyenne 30j (Risque de correction)."
            )

            else -> RecommendationDto(
                action = "HOLD",
                signalStrength = "WEAK",
                reason = "Le prix oscille dans sa zone de valeur habituelle."
            )
        }
    }

    @Cacheable("dashboardCache")
    fun getDashboardData(): DashboardAnalyticsDto {
        // 1. Récupération parallèle/séquentielle optimisée par BDD
        val gainers = priceRepository.findTopPriceChanges(sortAsc = false)
            .map { it.toCardSummaryDto() }

        val losers = priceRepository.findTopPriceChanges(sortAsc = true)
            .map { it.toCardSummaryDto() }

        val volatileCards = priceRepository.findMostVolatileCards()
            .map { it.toCardSummaryDto() }

        val marketIndex = priceRepository.findMarketIndexHistory()
            .map { PricePointDto(date = it.getDate(), price = roundTwoDecimals(it.getPriceEur() ?: 0.0)) }

        return DashboardAnalyticsDto(
            topGainers = gainers,
            topLosers = losers,
            mostVolatile = volatileCards,
            marketIndexHistory = marketIndex
        )
    }

    // Helper d'extension pour convertir la projection en DTO propre
    private fun CardSummaryProjection.toCardSummaryDto(): CardSummaryDto {
        return CardSummaryDto(
            scryfallId = getScryfallId(),
            name = getName(),
            currentPrice = roundTwoDecimals(getCurrentPrice()),
            priceChangePercentage = roundTwoDecimals(getPriceChangePercentage()),
            volatility = roundTwoDecimals(getVolatility())
        )
    }

    private fun roundTwoDecimals(value: Double): Double {
        return "%.2f".format(value).replace(',', '.').toDoubleOrNull() ?: value
    }
}