package com.fmartinier.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

@Entity
@Table(
    name = "price_history",
    uniqueConstraints = [UniqueConstraint(columnNames = ["card_id", "is_foil", "price_date"])]
)
class PriceHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "price_id")
    val priceId: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", referencedColumnName = "scryfall_id", nullable = false)
    val card: Card,

    @Column(name = "is_foil", nullable = false)
    val isFoil: Boolean = false,

    @Column(name = "price_date", nullable = false)
    val priceDate: LocalDate,

    @Column(name = "price_eur", precision = 10, scale = 2, nullable = false)
    val priceEur: BigDecimal,

    @Column(name = "created_at", updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)