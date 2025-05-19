package com.anaphygon.streaming.controller

import com.anaphygon.streaming.dto.*
import com.anaphygon.streaming.model.*
import com.anaphygon.streaming.service.AuthService
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

/**
 * Authentication Controller - DEBUG VERSION with extensive logging
 */
@Controller("/api/auth")
open class AuthController @Inject constructor(
    private val authService: AuthService
) {
    private val logger = LoggerFactory.getLogger(AuthController::class.java)

    /**
     * User login with extensive debugging
     */
    @Post("/login")
    @Secured(SecurityRule.IS_ANONYMOUS)
    open fun login(@Body request: UserLoginRequest): HttpResponse<ApiResponse<SessionResponse>> {
        logger.info("=== LOGIN START ===")
        logger.info("Login attempt for user: ${request.usernameOrEmail}")
        logger.info("Request details: username/email length=${request.usernameOrEmail.length}, password length=${request.password.length}")
        
        return try {
            // Step 1: Validate request
            logger.info("Step 1: Validating request...")
            if (request.usernameOrEmail.isBlank()) {
                logger.warn("VALIDATION FAILED: empty username/email")
                return HttpResponse.badRequest(
                    ApiResponse<SessionResponse>(
                        success = false,
                        message = "Username or email is required"
                    )
                )
            }
            
            if (request.password.isBlank()) {
                logger.warn("VALIDATION FAILED: empty password")
                return HttpResponse.badRequest(
                    ApiResponse<SessionResponse>(
                        success = false,
                        message = "Password is required"
                    )
                )
            }
            logger.info("Step 1: Request validation passed")

            // Step 2: Call auth service
            logger.info("Step 2: Calling authService.login()...")
            val session = authService.login(request)
            logger.info("Step 2: authService.login() returned: ${if (session != null) "SUCCESS (session created)" else "FAILURE (null)"}")
            
            // Step 3: Process result
            logger.info("Step 3: Processing login result...")
            if (session != null) {
                logger.info("Step 3: Login successful, creating response...")
                logger.info("Session details: token=${session.sessionToken}, user=${session.user.username}")
                
                val sessionResponse = try {
                    session.toSessionResponse()
                } catch (e: Exception) {
                    logger.error("ERROR creating session response", e)
                    throw e
                }
                
                val response = ApiResponse(
                    success = true,
                    data = sessionResponse,
                    message = "Login successful"
                )
                logger.info("Step 3: Response created successfully")
                logger.info("=== LOGIN SUCCESS ===")
                HttpResponse.ok(response)
            } else {
                logger.warn("Step 3: Login failed - null session returned")
                val response = ApiResponse<SessionResponse>(
                    success = false,
                    message = "Invalid credentials"
                )
                logger.info("=== LOGIN FAILED ===")
                HttpResponse.badRequest(response)
            }
        } catch (e: IllegalArgumentException) {
            logger.error("CAUGHT IllegalArgumentException", e)
            val response = ApiResponse<SessionResponse>(
                success = false,
                message = e.message ?: "Invalid credentials"
            )
            HttpResponse.badRequest(response)
        } catch (e: IllegalStateException) {
            logger.error("CAUGHT IllegalStateException", e)
            val response = ApiResponse<SessionResponse>(
                success = false,
                message = e.message ?: "Account is not active"
            )
            HttpResponse.badRequest(response)
        } catch (e: Exception) {
            logger.error("CAUGHT UNEXPECTED EXCEPTION", e)
            logger.error("Exception type: ${e.javaClass.name}")
            logger.error("Exception message: ${e.message}")
            logger.error("Exception cause: ${e.cause}")
            e.printStackTrace()
            
            val response = ApiResponse<SessionResponse>(
                success = false,
                message = "Login failed: ${e.message ?: "Unknown error"}"
            )
            logger.info("=== LOGIN ERROR ===")
            HttpResponse.status<ApiResponse<SessionResponse>>(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        }
    }

    /**
     * Logout user
     */
    @Post("/logout")
    @Produces(MediaType.APPLICATION_JSON)
    open fun logout(
        @Header("Authorization") authHeader: String?,
        @Header("X-Forwarded-For") ipAddress: String?,
        @Header("User-Agent") userAgent: String?
    ): HttpResponse<ApiResponse<Boolean>> {
        return try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return HttpResponse.badRequest(
                    ApiResponse(
                        success = false,
                        message = "Invalid authorization header"
                    )
                )
            }

            val sessionToken = authHeader.substring(7)
            val success = authService.logout(sessionToken, ipAddress, userAgent)

            if (success) {
                HttpResponse.ok(
                    ApiResponse(
                        success = true,
                        data = true,
                        message = "Logged out successfully"
                    )
                )
            } else {
                HttpResponse.badRequest(
                    ApiResponse(
                        success = false,
                        message = "Logout failed: Invalid session"
                    )
                )
            }
        } catch (e: Exception) {
            logger.error("Logout error", e)
            HttpResponse.serverError(
                ApiResponse(
                    success = false,
                    message = "Logout failed: ${e.message}"
                )
            )
        }
    }

    /**
     * Validate session with debugging
     */
    @Get("/validate")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    open fun validateSession(@Header("Authorization") authToken: String?): HttpResponse<ApiResponse<SessionResponse>> {
        logger.info("=== VALIDATE SESSION START ===")
        
        return try {
            if (authToken.isNullOrBlank()) {
                logger.warn("VALIDATION FAILED: missing authorization token")
                return HttpResponse.badRequest(
                    ApiResponse<SessionResponse>(
                        success = false,
                        message = "Authorization token is required"
                    )
                )
            }

            val sessionToken = authToken.removePrefix("Bearer ").trim()
            if (sessionToken.isBlank()) {
                logger.warn("VALIDATION FAILED: invalid authorization token format")
                return HttpResponse.badRequest(
                    ApiResponse<SessionResponse>(
                        success = false,
                        message = "Invalid authorization token format"
                    )
                )
            }

            logger.info("Validating session token: ${sessionToken.take(10)}...")
            val session = authService.validateAndUpdateSession(sessionToken)

            if (session != null) {
                logger.info("Session validation successful for user: ${session.user.username}")
                val response = ApiResponse(
                    success = true,
                    data = session.toSessionResponse(),
                    message = "Session valid"
                )
                logger.info("=== VALIDATE SESSION SUCCESS ===")
                HttpResponse.ok(response)
            } else {
                logger.warn("Session validation failed: session not found or invalid")
                val response = ApiResponse<SessionResponse>(
                    success = false,
                    message = "Invalid or expired session"
                )
                logger.info("=== VALIDATE SESSION FAILED ===")
                HttpResponse.status<ApiResponse<SessionResponse>>(HttpStatus.UNAUTHORIZED).body(response)
            }
        } catch (e: Exception) {
            logger.error("SESSION VALIDATION ERROR", e)
            val response = ApiResponse<SessionResponse>(
                success = false,
                message = "Session validation failed: ${e.message ?: "Unknown error"}"
            )
            HttpResponse.status<ApiResponse<SessionResponse>>(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        }
    }

    /**
     * End all sessions for current user
     */
    @Post("/logout-all")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    open fun logoutAll(@Header("Authorization") authToken: String?): HttpResponse<ApiResponse<Boolean>> {
        logger.info("=== LOGOUT ALL START ===")
        
        return try {
            if (authToken.isNullOrBlank()) {
                return HttpResponse.badRequest(
                    ApiResponse<Boolean>(
                        success = false,
                        message = "Authorization token is required"
                    )
                )
            }

            val sessionToken = authToken.removePrefix("Bearer ").trim()
            if (sessionToken.isBlank()) {
                return HttpResponse.badRequest(
                    ApiResponse<Boolean>(
                        success = false,
                        message = "Invalid authorization token format"
                    )
                )
            }

            val session = authService.findActiveSession(sessionToken)

            val response = if (session != null) {
                val success = authService.endAllUserSessions(session.user.id!!)
                ApiResponse(
                    success = success,
                    data = success,
                    message = if (success) "All sessions ended successfully" else "Failed to end sessions"
                )
            } else {
                ApiResponse(
                    success = false,
                    message = "Invalid or expired session"
                )
            }

            logger.info("Logout all sessions ${if (response.success) "successful" else "failed"}")
            logger.info("=== LOGOUT ALL END ===")
            HttpResponse.ok(response)
        } catch (e: Exception) {
            logger.error("LOGOUT ALL ERROR", e)
            val response = ApiResponse<Boolean>(
                success = false,
                message = "Failed to end sessions: ${e.message ?: "Unknown error"}"
            )
            HttpResponse.status<ApiResponse<Boolean>>(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        }
    }

    // === EXTENSION FUNCTIONS WITH ERROR HANDLING ===

    private fun UserSession.toSessionResponse(): SessionResponse {
        try {
            logger.info("Converting session to response - token: ${this.sessionToken?.take(10)}, user: ${this.user.username}")
            
            if (this.sessionToken == null) {
                logger.error("Session token is null!")
                throw IllegalStateException("Session token is null")
            }
            
            if (this.user.id == null) {
                logger.error("User ID is null!")
                throw IllegalStateException("User ID is null")
            }
            
            return SessionResponse(
                sessionToken = this.sessionToken!!,
                expiresAt = this.expiresAt,
                user = this.user.toUserResponse()
            )
        } catch (e: Exception) {
            logger.error("Error converting session to response", e)
            throw e
        }
    }

    private fun User.toUserResponse(): UserResponse {
        try {
            logger.info("Converting user to response - username: ${this.username}, id: ${this.id}")
            
            if (this.id == null) {
                logger.error("User ID is null for user: ${this.username}")
                throw IllegalStateException("User ID is null")
            }
            
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
        } catch (e: Exception) {
            logger.error("Error converting user to response", e)
            throw e
        }
    }
}