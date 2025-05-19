package com.anaphygon.streaming.controller

import com.anaphygon.streaming.dto.ApiResponse
import com.anaphygon.streaming.dto.ContentResponse
import com.anaphygon.streaming.service.HomeService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject

@Controller("/api/home")
open class HomeController @Inject constructor(
    private val homeService: HomeService
) {

    @Get("/featured")
    @Secured(SecurityRule.IS_ANONYMOUS)
    open fun getFeaturedContent(): HttpResponse<ApiResponse<List<ContentResponse>>> {
        return try {
            val data = homeService.getFeaturedContent()
            HttpResponse.ok(data)
        } catch (e: Exception) {
            val response = ApiResponse<List<ContentResponse>>(
                success = false,
                message = "Failed to get featured content: ${e.message}"
            )
            HttpResponse.serverError(response)
        }
    }

    @Get("/")
    @Secured(SecurityRule.IS_ANONYMOUS)
    open fun index(): HttpResponse<Map<String, Any>> {
        val data = mapOf(
            "message" to "Welcome to AnaphygonMicros Streaming Platform!",
            "version" to "0.1.0",
            "timestamp" to System.currentTimeMillis(),
            "endpoints" to mapOf(
                "health" to "/health",
                "metrics" to "/metrics",
                "swagger" to "/swagger-ui"
            )
        )
        return HttpResponse.ok(data)
    }

    @Get("/health-check")
    @Secured(SecurityRule.IS_ANONYMOUS)
    open fun healthCheck(): HttpResponse<Map<String, Any>> {
        val data = mapOf(
            "status" to "UP",
            "service" to "AnaphygonMicros",
            "timestamp" to System.currentTimeMillis()
        )
        return HttpResponse.ok(data)
    }

    @Get("/public/info")
    @Secured(SecurityRule.IS_ANONYMOUS)
    open fun publicInfo(): HttpResponse<Map<String, Any>> {
        val data = mapOf(
            "platform" to "AnaphygonMicros",
            "description" to "Movie & Series Streaming Platform",
            "features" to listOf(
                "User Management",
                "Content Streaming",
                "Recommendations",
                "Reviews & Ratings"
            )
        )
        return HttpResponse.ok(data)
    }
}