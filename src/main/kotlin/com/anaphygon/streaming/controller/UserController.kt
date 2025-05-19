package com.anaphygon.streaming.controller

import com.anaphygon.streaming.dto.*
import com.anaphygon.streaming.model.*
import com.anaphygon.streaming.service.*
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import jakarta.validation.constraints.NotBlank

/**
 * User Controller - handles user registration and basic user operations with CORS
 */
@Controller("/api/users")
open class UserController @Inject constructor(
    private val userService: UserService,
    private val authService: AuthService,
    private val profileService: ProfileService
) {

    /**
     * User registration with CORS headers
     */
    @Post("/register")
    @Secured(SecurityRule.IS_ANONYMOUS)
    open fun register(@Body request: UserRegistrationRequest): HttpResponse<ApiResponse<UserResponse>> {
        return try {
            val user = userService.createUser(request)
            val response = ApiResponse(
                success = true,
                data = user.toUserResponse(),
                message = "Registration successful"
            )
            HttpResponse.ok(response)
        } catch (e: Exception) {
            val response = ApiResponse<UserResponse>(
                success = false,
                message = "Registration failed: ${e.message}"
            )
            HttpResponse.badRequest(response)
        }
    }

    /**
     * Get current user info with CORS headers
     */
    @Get("/me")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    open fun getCurrentUser(@Header("Authorization") authToken: String): HttpResponse<ApiResponse<UserResponse>> {
        return try {
            val sessionToken = authToken.removePrefix("Bearer ").trim()
            val session = authService.findActiveSession(sessionToken)

            val response = if (session != null) {
                ApiResponse(
                    success = true,
                    data = session.user.toUserResponse(),
                    message = "User info retrieved"
                )
            } else {
                ApiResponse(
                    success = false,
                    message = "Invalid or expired session"
                )
            }
            HttpResponse.ok(response)
        } catch (e: Exception) {
            val response = ApiResponse<UserResponse>(
                success = false,
                message = "Failed to get user: ${e.message}"
            )
            HttpResponse.serverError(response)
        }
    }

    /**
     * Get user by ID with CORS headers
     */
    @Get("/{id}")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    open fun getUserById(@PathVariable id: Long): HttpResponse<ApiResponse<UserResponse>> {
        return try {
            val user = userService.findById(id)
            val response = if (user != null) {
                ApiResponse(
                    success = true,
                    data = user.toUserResponse(),
                    message = "User found"
                )
            } else {
                ApiResponse(
                    success = false,
                    message = "User not found"
                )
            }
            HttpResponse.ok(response)
        } catch (e: Exception) {
            val response = ApiResponse<UserResponse>(
                success = false,
                message = "Failed to get user: ${e.message}"
            )
            HttpResponse.serverError(response)
        }
    }

    /**
     * Search users with CORS headers
     */
    @Get("/search")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    open fun searchUsers(@QueryValue("q") @NotBlank query: String): HttpResponse<ApiResponse<List<UserResponse>>> {
        return try {
            val users = userService.searchUsers(query)
            val userResponses = users.map { it.toUserResponse() }

            val response = ApiResponse(
                success = true,
                data = userResponses,
                message = "Search completed"
            )
            HttpResponse.ok(response)
        } catch (e: Exception) {
            val response = ApiResponse<List<UserResponse>>(
                success = false,
                message = "Search failed: ${e.message}"
            )
            HttpResponse.serverError(response)
        }
    }

    /**
     * Get all active users (admin only) with CORS headers
     */
    @Get("/")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    open fun getAllActiveUsers(): HttpResponse<ApiResponse<List<UserResponse>>> {
        return try {
            val users = userService.findAllActive()
            val userResponses = users.map { it.toUserResponse() }

            val response = ApiResponse(
                success = true,
                data = userResponses,
                message = "Active users retrieved"
            )
            HttpResponse.ok(response)
        } catch (e: Exception) {
            val response = ApiResponse<List<UserResponse>>(
                success = false,
                message = "Failed to get users: ${e.message}"
            )
            HttpResponse.serverError(response)
        }
    }

    /**
     * Update user status (admin only) with CORS headers
     */
    @Put("/{id}/status")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    open fun updateUserStatus(
        @PathVariable id: Long,
        @Body statusRequest: Map<String, String>
    ): HttpResponse<ApiResponse<UserResponse>> {
        return try {
            val statusString = statusRequest["status"] ?: return HttpResponse.badRequest(
                ApiResponse<UserResponse>(
                    success = false,
                    message = "Status is required"
                )
            )

            val status = UserStatus.valueOf(statusString.uppercase())
            val user = userService.updateUserStatus(id, status)

            val response = if (user != null) {
                ApiResponse(
                    success = true,
                    data = user.toUserResponse(),
                    message = "User status updated"
                )
            } else {
                ApiResponse(
                    success = false,
                    message = "User not found"
                )
            }
            HttpResponse.ok(response)
        } catch (e: IllegalArgumentException) {
            val response = ApiResponse<UserResponse>(
                success = false,
                message = "Invalid status value"
            )
            HttpResponse.badRequest(response)
        } catch (e: Exception) {
            val response = ApiResponse<UserResponse>(
                success = false,
                message = "Failed to update status: ${e.message}"
            )
            HttpResponse.serverError(response)
        }
    }

    // === EXTENSION FUNCTIONS ===

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