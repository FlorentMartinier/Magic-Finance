package com.fmartinier.repository

import com.fmartinier.domain.PriceHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface PriceHistoryRepository : JpaRepository<PriceHistory, Long> {

    // Cette méthode est requise par le ScryfallStreamService pour éviter d'insérer 
    // deux fois un prix pour la même carte et le même type (foil/non foil) le même jour.
    fun existsByCardScryfallIdAndIsFoilAndPriceDate(
        scryfallId: String,
        isFoil: Boolean,
        priceDate: LocalDate
    ): Boolean

    // Récupère d'un coup tous les historiques existants pour un lot de cartes à une date donnée
    fun findByCardScryfallIdInAndPriceDate(
        scryfallIds: Collection<String>,
        priceDate: LocalDate
    ): List<PriceHistory>
}