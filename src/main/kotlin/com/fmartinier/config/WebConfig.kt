package com.fmartinier.config

import com.fmartinier.security.ApiKeyInterceptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val apiKeyInterceptor: ApiKeyInterceptor,
    @Value("\${app.cors.allowed-origins:http://localhost:4200}") private val allowedOrigins: String
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        // On applique la sécurité uniquement sur les routes d'API
        registry.addInterceptor(apiKeyInterceptor)
            .addPathPatterns("/api/**")
    }

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOrigins(*allowedOrigins.split(",").toTypedArray())
            .allowedMethods("GET", "POST", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
    }
}