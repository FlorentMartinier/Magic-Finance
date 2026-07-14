package com.fmartinier.repository

import com.fmartinier.domain.PriceHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface PriceHistoryRepository : JpaRepository<PriceHistory, Long> {

    // Récupère d'un coup tous les historiques existants pour un lot de cartes à une date donnée
    fun findByCardScryfallIdInAndPriceDate(
        scryfallIds: Collection<String>,
        priceDate: LocalDate
    ): List<PriceHistory>

    // Récupère l'historique d'une carte pour les 30 derniers jours
    fun findByCardScryfallIdAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
        scryfallId: String,
        startDate: LocalDate
    ): List<PriceHistory>

    // Nouvelle méthode : Récupère l'historique des 30 derniers jours pour un LOT de cartes
    fun findByCardScryfallIdInAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
        scryfallIds: Collection<String>,
        startDate: LocalDate
    ): List<PriceHistory>
}