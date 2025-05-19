package com.anaphygon.streaming.controller

import com.anaphygon.streaming.dto.*
import com.anaphygon.streaming.model.*
import com.anaphygon.streaming.service.AuthService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import org.slf4j.LoggerFactory

/**
 * Authentication Controller - handles login/logout operations
 */
@Controller("/api/auth")
open class AuthController @Inject constructor(
    private val authService: AuthService
) {
    private val logger = LoggerFactory.getLogger(AuthController::class.java)

    /**
     * User login
     */
    @Post("/login")
    @Secured(SecurityRule.IS_ANONYMOUS)
    open fun login(@Body request: UserLoginRequest): HttpResponse<ApiResponse<SessionResponse>> {
        return try {
            val session = authService.login(request)
            val response = if (session != null) {
                ApiResponse(
                    success = true,
                    data = session.toSessionResponse(),
                    message = "Login successful"
                )
            } else {
                ApiResponse(
                    success = false,
                    message = "Invalid credentials or account locked"
                )
            }

            HttpResponse.ok(response)
        } catch (e: IllegalArgumentException) {
            logger.warn("Login failed for user: ${request.usernameOrEmail} - ${e.message}")
            val response = ApiResponse<SessionResponse>(
                success = false,
                message = e.message ?: "Invalid credentials"
            )
            HttpResponse.badRequest(response)
        } catch (e: IllegalStateException) {
            logger.warn("Login failed for user: ${request.usernameOrEmail} - ${e.message}")
            val response = ApiResponse<SessionResponse>(
                success = false,
                message = e.message ?: "Account is not active"
            )
            HttpResponse.badRequest(response)
        } catch (e: Exception) {
            logger.error("Login failed for user: ${request.usernameOrEmail}", e)
            val response = ApiResponse<SessionResponse>(
                success = false,
                message = "Login failed: ${e.message ?: "Unknown error"}"
            )
            HttpResponse.serverError(response)
        }
    }

    /**
     * User logout
     */
    @Post("/logout")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    open fun logout(
        @Header("Authorization") authToken: String,
        @Body logoutRequest: Map<String, String>?
    ): HttpResponse<ApiResponse<Boolean>> {
        return try {
            val sessionToken = authToken.removePrefix("Bearer ").trim()
            val success = authService.logout(
                sessionToken = sessionToken,
                ipAddress = logoutRequest?.get("ipAddress"),
                userAgent = logoutRequest?.get("userAgent")
            )

            val response = ApiResponse(
                success = success,
                data = success,
                message = if (success) "Logout successful" else "Logout failed"
            )

            HttpResponse.ok(response)
        } catch (e: Exception) {
            val response = ApiResponse<Boolean>(
                success = false,
                message = "Logout failed: ${e.message}"
            )
            HttpResponse.badRequest(response)
        }
    }

    /**
     * Validate current session
     */
    @Get("/validate")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    open fun validateSession(@Header("Authorization") authToken: String): HttpResponse<ApiResponse<SessionResponse>> {
        return try {
            val sessionToken = authToken.removePrefix("Bearer ").trim()
            val session = authService.validateAndUpdateSession(sessionToken)

            val response = if (session != null) {
                ApiResponse(
                    success = true,
                    data = session.toSessionResponse(),
                    message = "Session valid"
                )
            } else {
                ApiResponse(
                    success = false,
                    message = "Invalid or expired session"
                )
            }

            HttpResponse.ok(response)
        } catch (e: Exception) {
            val response = ApiResponse<SessionResponse>(
                success = false,
                message = "Session validation failed: ${e.message}"
            )
            HttpResponse.badRequest(response)
        }
    }

    /**
     * End all sessions for current user
     */
    @Post("/logout-all")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    open fun logoutAll(@Header("Authorization") authToken: String): HttpResponse<ApiResponse<Boolean>> {
        return try {
            val sessionToken = authToken.removePrefix("Bearer ").trim()
            val session = authService.findActiveSession(sessionToken)

            val response = if (session != null) {
                val success = authService.endAllUserSessions(session.user.id!!)
                ApiResponse(
                    success = success,
                    data = success,
                    message = if (success) "All sessions ended" else "Failed to end sessions"
                )
            } else {
                ApiResponse(
                    success = false,
                    message = "Invalid session"
                )
            }

            HttpResponse.ok(response)
        } catch (e: Exception) {
            val response = ApiResponse<Boolean>(
                success = false,
                message = "Failed to end sessions: ${e.message}"
            )
            HttpResponse.badRequest(response)
        }
    }

    // === EXTENSION FUNCTIONS ===

    private fun UserSession.toSessionResponse(): SessionResponse {
        return SessionResponse(
            sessionToken = this.sessionToken!!,
            expiresAt = this.expiresAt,
            user = this.user.toUserResponse()
        )
    }

    private fun User.toUserResponse(): UserResponse {
        return UserResponse(
            id = this.id!!,
            username = this.username,
            email = this.email,
            emailVerified = this.emailVerified,
            status = this.status.name,
            currentRole = this.getCurrentRole()?.name,
            isPremium = this.isPremium(),
            createdAt = this.createdAt
        )
    }
}