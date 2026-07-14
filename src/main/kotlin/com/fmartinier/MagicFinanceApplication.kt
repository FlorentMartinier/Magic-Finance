package com.fmartinier

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class MagicFinanceApplication

fun main(args: Array<String>) {
    runApplication<MagicFinanceApplication>(*args)
}