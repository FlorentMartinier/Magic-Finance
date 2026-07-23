package com.fmartinier.repository

import com.fmartinier.domain.Price
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface PriceRepository : JpaRepository<Price, Long> {

    // 1. Récupère l'historique de prix du jour pour un lot de cartes (utilisé dans le batch d'import)
    fun findByCardScryfallIdInAndUpdatedAt(
        scryfallIds: Collection<String>,
        updatedAt: LocalDate
    ): List<Price>

    // 2. Récupère l'historique des 30 derniers jours pour un lot de cartes (ordonné par date)
    fun findByCardScryfallIdInAndUpdatedAtGreaterThanEqualOrderByUpdatedAtAsc(
        scryfallIds: Collection<String>,
        startDate: LocalDate
    ): List<Price>

    /**
     * Calcule TOUTES les métriques d'une carte en 1 seule requête SQL.
     */
    @Query(
        value = """
        WITH card_prices AS (
            SELECT 
                price_eur,
                updated_at::date AS price_date
            FROM price
            WHERE card_id = :scryfallId
        ),
        latest_price AS (
            SELECT price_eur FROM card_prices ORDER BY price_date DESC LIMIT 1
        ),
        price_24h AS (
            SELECT price_eur FROM card_prices WHERE price_date <= CURRENT_DATE - INTERVAL '1 day' ORDER BY price_date DESC LIMIT 1
        ),
        price_7d AS (
            SELECT price_eur FROM card_prices WHERE price_date <= CURRENT_DATE - INTERVAL '7 days' ORDER BY price_date DESC LIMIT 1
        )
        SELECT 
            :scryfallId AS scryfallId,
            (SELECT price_eur FROM latest_price) AS currentPrice,
            (SELECT price_eur FROM price_24h) AS price24hAgo,
            (SELECT price_eur FROM price_7d) AS price7dAgo,
            AVG(price_eur) FILTER (WHERE price_date >= CURRENT_DATE - INTERVAL '30 days') AS movingAverage30d,
            STDDEV(price_eur) FILTER (WHERE price_date >= CURRENT_DATE - INTERVAL '30 days') AS volatility,
            MAX(price_eur) AS allTimeHigh,
            MIN(price_eur) AS allTimeLow
        FROM card_prices
        """,
        nativeQuery = true
    )
    fun findAnalyticsByScryfallId(@Param("scryfallId") scryfallId: String): CardAnalyticsProjection?

    /**
     * Récupère l'historique des prix ordonné pour le graphique (Ex: sur le dernier mois/année)
     */
    @Query(
        value = """
        SELECT updated_at::date AS date, price_eur 
        FROM price 
        WHERE card_id = :scryfallId 
        ORDER BY updated_at ASC
        """,
        nativeQuery = true
    )
    fun findPriceHistoryByScryfallId(@Param("scryfallId") scryfallId: String): List<PricePointProjection>

    /**
     * Récupère le Top 10 Gainers ou Losers sur 24h
     * Si sortAsc = true -> Losers (les plus grosses baisses)
     * Si sortAsc = false -> Gainers (les plus grosses hausses)
     */
    @Query(
        value = """
        WITH latest_two_prices AS (
            SELECT 
                card_id,
                price_eur AS price,
                updated_at,
                ROW_NUMBER() OVER (PARTITION BY card_id ORDER BY updated_at DESC) as rn
            FROM price
            WHERE updated_at >= CURRENT_DATE - INTERVAL '2 days'
              AND price_eur IS NOT NULL
        ),
        price_changes AS (
            SELECT 
                p1.card_id,
                p1.price AS current_price,
                ((p1.price - p2.price) / p2.price) * 100 AS change_pct
            FROM latest_two_prices p1
            JOIN latest_two_prices p2 ON p1.card_id = p2.card_id AND p1.rn = 1 AND p2.rn = 2
        )
        SELECT 
            card_id AS scryfallId,
            c.name AS name,
            price_changes.current_price AS currentPrice,
            change_pct AS priceChangePercentage,
            0.0 AS volatility
        FROM price_changes
        join card c on c.scryfall_id = price_changes.card_id
        ORDER BY 
            CASE WHEN :sortAsc = true THEN change_pct END ASC,
            CASE WHEN :sortAsc = false THEN change_pct END DESC
        LIMIT 10
        """,
        nativeQuery = true
    )
    fun findTopPriceChanges(@Param("sortAsc") sortAsc: Boolean): List<CardSummaryProjection>

    /**
     * Récupère les 10 cartes les plus volatiles sur les 30 derniers jours
     */
    @Query(
        value = """
        WITH RankedPrices AS (
            SELECT 
                card_id,
                price_eur,
                updated_at,
                -- Récupère le prix immédiatement précédent pour la même carte
                LAG(price_eur) OVER (PARTITION BY card_id ORDER BY updated_at ASC) AS prev_price,
                ROW_NUMBER() OVER (PARTITION BY card_id ORDER BY updated_at DESC) AS rn
            FROM price
            WHERE updated_at >= CURRENT_DATE - INTERVAL '30 days'
              AND price_eur IS NOT NULL
        )
        SELECT 
            rp.card_id AS scryfallId,
            c.name,
            price_eur AS currentPrice,
            -- Calcul du % de variation : ((PrixActuel - PrixPrécédent) / PrixPrécédent) * 100
            CASE 
                WHEN prev_price IS NULL OR prev_price = 0 THEN 0.0
                ELSE ROUND(((price_eur - prev_price) / prev_price * 100)::numeric, 2)
            END AS priceChangePercentage,
            stats.volatility
        FROM RankedPrices rp
        JOIN (
            -- On garde le calcul de la volatilité globale sur 30j
            SELECT 
                card_id,
                STDDEV(price_eur) AS volatility,
                COUNT(price_eur) AS total_count
            FROM price
            WHERE updated_at >= CURRENT_DATE - INTERVAL '30 days'
              AND price_eur IS NOT NULL
            GROUP BY card_id
        ) stats ON rp.card_id = stats.card_id
        join card c on c.scryfall_id = stats.card_id
        WHERE rp.rn = 1 -- Prend uniquement le prix le plus récent
        ORDER BY stats.volatility DESC
        LIMIT 10;
        """,
        nativeQuery = true
    )
    fun findMostVolatileCards(): List<CardSummaryProjection>

    /**
     * Calcule la moyenne quotidienne de l'ensemble du marché (Indice MTG) sur 30 jours
     */
    @Query(
        value = """
        SELECT 
            updated_at::date AS date, 
            AVG(price_eur) AS price 
        FROM price 
        WHERE updated_at >= CURRENT_DATE - INTERVAL '30 days'
          AND price_eur IS NOT NULL
        GROUP BY updated_at::date 
        ORDER BY date ASC
        """,
        nativeQuery = true
    )
    fun findMarketIndexHistory(): List<PricePointProjection>
}