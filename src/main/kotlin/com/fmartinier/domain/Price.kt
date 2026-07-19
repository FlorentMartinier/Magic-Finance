package com.fmartinier.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "price")
class Price(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", referencedColumnName = "scryfall_id", nullable = false)
    val card: Card,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDate = LocalDate.now(),

    // Prix Normaux
    @Column(name = "price_eur") var priceEur: BigDecimal? = null,
    @Column(name = "price_usd") var priceUsd: BigDecimal? = null,

    // Prix Foil
    @Column(name = "price_foil_eur") var priceFoilEur: BigDecimal? = null,
    @Column(name = "price_foil_usd") var priceFoilUsd: BigDecimal? = null,

    // Prix Etched
    @Column(name = "price_etched_eur") var priceEtchedEur: BigDecimal? = null,
    @Column(name = "price_etched_usd") var priceEtchedUsd: BigDecimal? = null
)