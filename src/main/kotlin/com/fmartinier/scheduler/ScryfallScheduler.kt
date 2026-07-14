package com.fmartinier.scheduler

import com.fasterxml.jackson.databind.ObjectMapper
import com.fmartinier.service.ScryfallStreamService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Component
class ScryfallScheduler(
    private val scryfallStreamService: ScryfallStreamService,
    private val objectMapper: ObjectMapper,
    @Value("\${app.scryfall.bulk-data-url}") private val scryfallMetaUrl: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // Un seul client HTTP partagé pour toute la classe
    private val httpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    @Scheduled(cron = "0 0 3 * * ?")
    fun scheduleNightlySync() {
        logger.info("Starting nightly automated Scryfall sync...")
        try {
            val downloadUrl = fetchLatestBulkUrl()
            if (downloadUrl != null) {
                scryfallStreamService.syncCardsAndPrices(downloadUrl)
            } else {
                logger.error("Failed to extract Scryfall download URL from metadata.")
            }
        } catch (e: Exception) {
            logger.error("Error occurred during nightly sync scheduled task: ${e.message}", e)
        }
    }

    /**
     * Appelle l'API Scryfall avec HttpClient pour obtenir l'URL de téléchargement fraîche du jour
     */
    private fun fetchLatestBulkUrl(): String? {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(scryfallMetaUrl))
            .header("User-Agent", "MagicFinance/1.0.0 (florent.martinier@free.fr; GitHub Portfolio)")
            .header("Accept", "application/json")
            .GET()
            .build()

        try {
            // On récupère la réponse sous forme de String brute
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() != 200) {
                logger.error("Failed to fetch Scryfall metadata. HTTP Status: ${response.statusCode()}")
                return null
            }

            // Jackson parse la String en un arbre JsonNode
            val rootNode = objectMapper.readTree(response.body())
            val dataArray = rootNode.get("data")

            if (dataArray != null && dataArray.isArray) {
                for (node in dataArray) {
                    if (node.get("type")?.asText() == "default_cards") {
                        return node.get("download_uri")?.asText()
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error while fetching latest Scryfall bulk URL: ${e.message}", e)
        }

        return null
    }
}