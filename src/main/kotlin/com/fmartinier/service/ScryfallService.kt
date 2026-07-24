package com.fmartinier.service

import com.fmartinier.dto.ScryfallPurchaseUris
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class ScryfallService(private val restTemplate: RestTemplate) {

    fun getPurchaseUris(scryfallId: String): ScryfallPurchaseUris {
        val url = "https://api.scryfall.com/cards/$scryfallId"
        val response = restTemplate.getForObject(url, Map::class.java)

        val purchaseUris = response?.get("purchase_uris") as? Map<*, *>
        return ScryfallPurchaseUris(
            cardmarket = purchaseUris?.get("cardmarket") as? String,
            tcgplayer = purchaseUris?.get("tcgplayer") as? String,
            cardhoarder = purchaseUris?.get("cardhoarder") as? String,
        )
    }
}