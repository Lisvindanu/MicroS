package com.anaphygon.streaming.controller

import com.anaphygon.streaming.dto.*
import com.anaphygon.streaming.model.*
import com.anaphygon.streaming.service.*
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import jakarta.validation.constraints.NotBlank

/**
 * User Controller - handles user registration and basic user operations
 */
@Controller("/api/users")
open class UserController @Inject constructor(
    private val userService: UserService,
    private val authService: AuthService,
    private val profileService: ProfileService
) {

    /**
     * User registration
     */
    @Post("/register")
    @Secured(SecurityRule.IS_ANONYMOUS)
    open fun register(@Body request: UserRegistrationRequest): ApiResponse<UserResponse> {
        return try {
            // Create user
            val user = userService.createUser(request)

            // Create initial profile and preferences
            profileService.createInitialProfile(
                user = user,
                firstName = request.firstName,
                lastName = request.lastName,
                displayName = request.displayName
            )
            profileService.createInitialPreferences(user)

            // Get updated user with profile
            val updatedUser = userService.findById(user.id!!)!!

            ApiResponse(
                success = true,
                data = updatedUser.toUserResponse(),
                message = "User registered successfully"
            )
        } catch (e: IllegalArgumentException) {
            ApiResponse(
                success = false,
                message = e.message
            )
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Registration failed: ${e.message}"
            )
        }
    }

    /**
     * Get current user info
     */
    @Get("/me")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    open fun getCurrentUser(@Header("Authorization") authToken: String): ApiResponse<UserResponse> {
        return try {
            val sessionToken = authToken.removePrefix("Bearer ").trim()
            val session = authService.findActiveSession(sessionToken)

            if (session != null) {
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
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Failed to get user: ${e.message}"
            )
        }
    }

    /**
     * Get user by ID
     */
    @Get("/{id}")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    open fun getUserById(@PathVariable id: Long): ApiResponse<UserResponse> {
        return try {
            val user = userService.findById(id)
            if (user != null) {
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
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Failed to get user: ${e.message}"
            )
        }
    }

    /**
     * Search users
     */
    @Get("/search")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    open fun searchUsers(@QueryValue("q") @NotBlank query: String): ApiResponse<List<UserResponse>> {
        return try {
            val users = userService.searchUsers(query)
            val userResponses = users.map { it.toUserResponse() }

            ApiResponse(
                success = true,
                data = userResponses,
                message = "Search completed"
            )
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Search failed: ${e.message}"
            )
        }
    }

    /**
     * Get all active users (admin only)
     */
    @Get("/")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    open fun getAllActiveUsers(): ApiResponse<List<UserResponse>> {
        return try {
            val users = userService.findAllActive()
            val userResponses = users.map { it.toUserResponse() }

            ApiResponse(
                success = true,
                data = userResponses,
                message = "Active users retrieved"
            )
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Failed to get users: ${e.message}"
            )
        }
    }

    /**
     * Update user status (admin only)
     */
    @Put("/{id}/status")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    open fun updateUserStatus(
        @PathVariable id: Long,
        @Body statusRequest: Map<String, String>
    ): ApiResponse<UserResponse> {
        return try {
            val statusString = statusRequest["status"] ?: return ApiResponse(
                success = false,
                message = "Status is required"
            )

            val status = UserStatus.valueOf(statusString.uppercase())
            val user = userService.updateUserStatus(id, status)

            if (user != null) {
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
        } catch (e: IllegalArgumentException) {
            ApiResponse(
                success = false,
                message = "Invalid status value"
            )
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Failed to update status: ${e.message}"
            )
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
