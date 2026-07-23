package com.fmartinier.repository

import java.time.LocalDate

// Projection pour les statistiques globales d'une carte
interface CardAnalyticsProjection {
    fun getScryfallId(): String
    fun getCurrentPrice(): Double?
    fun getPrice24hAgo(): Double?
    fun getPrice7dAgo(): Double?
    fun getMovingAverage30d(): Double?
    fun getVolatility(): Double?
    fun getAllTimeHigh(): Double?
    fun getAllTimeLow(): Double?
}

// Projection pour l'historique chronologique
interface PricePointProjection {
    fun getDate(): LocalDate
    fun getPriceEur(): Double?
}

interface CardSummaryProjection {
    fun getScryfallId(): String
    fun getName(): String = ""
    fun getCurrentPrice(): Double = 0.0
    fun getPriceChangePercentage(): Double = 0.0
    fun getVolatility(): Double = 0.0
}