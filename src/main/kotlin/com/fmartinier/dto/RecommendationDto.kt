package com.fmartinier.dto

data class RecommendationDto(
    val action: String,         // "BUY", "SELL", "HOLD"
    val signalStrength: String, // "STRONG", "MEDIUM", "WEAK"
    val reason: String          // Ex: "Prix 12% sous la moyenne mobile sur 30 jours."
)