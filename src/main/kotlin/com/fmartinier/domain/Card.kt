package com.fmartinier.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

@Entity
@Table(name = "cards")
class Card(
    @Id
    @Column(name = "scryfall_id", length = 36)
    val scryfallId: String,

    @Column(name = "name_en", nullable = false)
    var nameEn: String,

    @Column(name = "name_fr")
    var nameFr: String?,

    @Column(name = "image_url", length = 512)
    var imageUrl: String?,

    @Column(name = "current_price", precision = 10, scale = 2)
    var currentPrice: BigDecimal? = null,

    @Column(name = "max_price", precision = 10, scale = 2)
    var maxPrice: BigDecimal? = null,

    @Column(name = "max_price_date")
    var maxPriceDate: LocalDate? = null,

    @Column(name = "min_price", precision = 10, scale = 2)
    var minPrice: BigDecimal? = null,

    @Column(name = "min_price_date")
    var minPriceDate: LocalDate? = null,

    @Column(name = "price_change_7d", precision = 5, scale = 2)
    var priceChange7d: BigDecimal = BigDecimal.ZERO,

    @Column(name = "price_change_30d", precision = 5, scale = 2)
    var priceChange30d: BigDecimal = BigDecimal.ZERO,

    @Column(name = "volatility_score", precision = 10, scale = 4)
    var volatilityScore: BigDecimal = BigDecimal.ZERO,

    @Column(name = "created_at", updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),

    @OneToMany(mappedBy = "card", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val priceHistory: MutableList<PriceHistory> = mutableListOf()
)