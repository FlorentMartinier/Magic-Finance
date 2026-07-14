package com.fmartinier.config

import com.fmartinier.security.ApiKeyInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(private val apiKeyInterceptor: ApiKeyInterceptor) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        // On applique la sécurité uniquement sur les routes d'API
        registry.addInterceptor(apiKeyInterceptor)
            .addPathPatterns("/api/**")
    }
}