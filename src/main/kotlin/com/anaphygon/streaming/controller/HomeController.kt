package com.anaphygon.streaming.controller

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule

@Controller
open class HomeController {

    @Get("/")
    @Secured(SecurityRule.IS_ANONYMOUS) // Allow anonymous access
    open fun index(): Map<String, Any> {
        return mapOf(
            "message" to "Welcome to AnaphygonMicros Streaming Platform!",
            "version" to "0.1.0",
            "timestamp" to System.currentTimeMillis(),
            "endpoints" to mapOf(
                "health" to "/health",
                "metrics" to "/metrics",
                "swagger" to "/swagger-ui"
            )
        )
    }

    @Get("/health-check")
    @Secured(SecurityRule.IS_ANONYMOUS)
    open fun healthCheck(): Map<String, Any> {
        return mapOf(
            "status" to "UP",
            "service" to "AnaphygonMicros",
            "timestamp" to System.currentTimeMillis()
        )
    }

    @Get("/public/info")
    @Secured(SecurityRule.IS_ANONYMOUS)
    open fun publicInfo(): Map<String, Any> {
        return mapOf(
            "platform" to "AnaphygonMicros",
            "description" to "Movie & Series Streaming Platform",
            "features" to listOf(
                "User Management",
                "Content Streaming",
                "Recommendations",
                "Reviews & Ratings"
            )
        )
    }
}