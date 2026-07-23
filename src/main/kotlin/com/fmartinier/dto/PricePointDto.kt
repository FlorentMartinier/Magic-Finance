package com.fmartinier.dto

import java.time.LocalDate

data class PricePointDto(
    val date: LocalDate,
    val price: Double
)