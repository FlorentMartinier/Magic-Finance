package com.fmartinier.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class ApiKeyInterceptor(
    @Value("\${app.security.api-key}") private val configuredApiKey: String
) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val requestApiKey = request.getHeader("X-API-KEY")

        if (requestApiKey == null || requestApiKey != configuredApiKey) {
            response.status = HttpStatus.UNAUTHORIZED.value()
            response.writer.write("Unauthorized: Invalid or missing API Key.")
            return false // Bloque la requête
        }

        return true // Autorise la requête à continuer vers le contrôleur
    }
}