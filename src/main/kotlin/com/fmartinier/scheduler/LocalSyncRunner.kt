package com.fmartinier.scheduler

import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("local") // S'active uniquement si le profil active est "local"
class LocalSyncRunner(
    private val scryfallScheduler: ScryfallScheduler
) : CommandLineRunner {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(vararg args: String?) {
        logger.info("====================================================")
        logger.info("Local profile active: Triggering Scryfall sync on startup...")
        logger.info("====================================================")

        // On lance la méthode du scheduler qui récupère l'URL dynamique et lance le stream
        scryfallScheduler.scheduleNightlySync()
    }
}