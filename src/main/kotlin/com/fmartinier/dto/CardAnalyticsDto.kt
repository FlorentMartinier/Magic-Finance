package com.fmartinier.dto

data class CardAnalyticsDto(
    val scryfallId: String,
    val currentPrice: Double,
    val change24h: Double,       // Pourcentage, ex: +5.2 ou -3.1
    val change7d: Double,
    val movingAverage30d: Double,
    val volatility: Double,       // Écart-type / mesure de risque
    val allTimeHigh: Double,
    val allTimeLow: Double,
    val recommendation: RecommendationDto,
    val priceHistory: List<PricePointDto>,
    val purchaseUris: ScryfallPurchaseUris,
    )