package com.fmartinier.controller

import com.fmartinier.dto.CardAnalyticsDto
import com.fmartinier.dto.DashboardAnalyticsDto
import com.fmartinier.service.AnalyticsService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/analytics")
class AnalyticsController(
    private val analyticsService: AnalyticsService
) {

    @GetMapping("/dashboard")
    fun getDashboard(): ResponseEntity<DashboardAnalyticsDto> {
        return ResponseEntity.ok(analyticsService.getDashboardData())
    }

    @GetMapping("/cards/{scryfallId}")
    fun getCardAnalytics(@PathVariable scryfallId: String): ResponseEntity<CardAnalyticsDto> {
        return ResponseEntity.ok(analyticsService.getCardAnalytics(scryfallId))
    }
}