package com.fmartinier.repository

import com.fmartinier.domain.Price
import org.springframework.data.jpa.repository.JpaRepository
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
}