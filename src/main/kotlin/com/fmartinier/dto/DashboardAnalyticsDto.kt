package com.fmartinier.dto

data class DashboardAnalyticsDto(
    val topGainers: List<CardSummaryDto>,
    val topLosers: List<CardSummaryDto>,
    val mostVolatile: List<CardSummaryDto>,
    val marketIndexHistory: List<PricePointDto>
)