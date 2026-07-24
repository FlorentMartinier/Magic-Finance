package com.fmartinier.dto

data class CardSummaryDto(
    val scryfallId: String,
    val name: String,
    val currentPrice: Double,
    val priceChangePercentage: Double,
    val volatility: Double,
)