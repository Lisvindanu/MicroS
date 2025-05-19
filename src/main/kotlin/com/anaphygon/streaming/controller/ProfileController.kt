package com.anaphygon.streaming.controller

import com.anaphygon.streaming.dto.*
import com.anaphygon.streaming.model.*
import com.anaphygon.streaming.service.*
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject

/**
 * Profile Controller - handles user profile and preferences management with CORS
 */
@Controller("/api/profiles")
open class ProfileController @Inject constructor(
    private val authService: AuthService,
    private val profileService: ProfileService
) {

    @Get("/{id}")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    open fun getProfile(@PathVariable id: Long): HttpResponse<ApiResponse<ProfileResponse>> {
        return try {
            val profile = profileService.getProfile(id)
            val response = ApiResponse(
                success = true,
                data = profile?.toProfileResponse(),
                message = "Profile retrieved successfully"
            )
            HttpResponse.ok(response)
        } catch (e: Exception) {
            val response = ApiResponse<ProfileResponse>(
                success = false,
                message = "Failed to get profile: ${e.message}"
            )
            HttpResponse.serverError(response)
        }
    }

    /**
     * Get current user profile with CORS headers
     */
    @Get("/")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    open fun getProfile(@Header("Authorization") authToken: String): HttpResponse<ApiResponse<UserProfileResponse>> {
        return try {
            val sessionToken = authToken.removePrefix("Bearer ").trim()
            val session = authService.findActiveSession(sessionToken)

            val response = if (session != null) {
                val profile = profileService.getUserProfile(session.user.id!!)
                if (profile != null) {
                    ApiResponse(
                        success = true,
                        data = profile.toUserProfileResponse(),
                        message = "Profile retrieved"
                    )
                } else {
                    ApiResponse(
                        success = false,
                        message = "Profile not found"
                    )
                }
            } else {
                ApiResponse(
                    success = false,
                    message = "Invalid or expired session"
                )
            }
            HttpResponse.ok(response)
        } catch (e: Exception) {
            val response = ApiResponse<UserProfileResponse>(
                success = false,
                message = "Failed to get profile: ${e.message}"
            )
            HttpResponse.serverError(response)
        }
    }

    /**
     * Update user profile with CORS headers
     */
    @Put("/")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    open fun updateProfile(
        @Header("Authorization") authToken: String,
        @Body request: UserProfileUpdateRequest
    ): HttpResponse<ApiResponse<UserProfileResponse>> {
        return try {
            val sessionToken = authToken.removePrefix("Bearer ").trim()
            val session = authService.findActiveSession(sessionToken)

            val response = if (session != null) {
                val profile = profileService.updateProfile(session.user.id!!, request)
                if (profile != null) {
                    ApiResponse(
                        success = true,
                        data = profile.toUserProfileResponse(),
                        message = "Profile updated successfully"
                    )
                } else {
                    ApiResponse(
                        success = false,
                        message = "Profile not found"
                    )
                }
            } else {
                ApiResponse(
                    success = false,
                    message = "Invalid or expired session"
                )
            }
            HttpResponse.ok(response)
        } catch (e: Exception) {
            val response = ApiResponse<UserProfileResponse>(
                success = false,
                message = "Profile update failed: ${e.message}"
            )
            HttpResponse.serverError(response)
        }
    }

    /**
     * Get user preferences with CORS headers
     */
    @Get("/preferences")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    open fun getPreferences(@Header("Authorization") authToken: String): HttpResponse<ApiResponse<UserPreferencesResponse>> {
        return try {
            val sessionToken = authToken.removePrefix("Bearer ").trim()
            val session = authService.findActiveSession(sessionToken)

            val response = if (session != null) {
                val preferences = profileService.getUserPreferences(session.user.id!!)
                if (preferences != null) {
                    ApiResponse(
                        success = true,
                        data = preferences.toUserPreferencesResponse(),
                        message = "Preferences retrieved"
                    )
                } else {
                    ApiResponse(
                        success = false,
                        message = "Preferences not found"
                    )
                }
            } else {
                ApiResponse(
                    success = false,
                    message = "Invalid or expired session"
                )
            }
            HttpResponse.ok(response)
        } catch (e: Exception) {
            val response = ApiResponse<UserPreferencesResponse>(
                success = false,
                message = "Failed to get preferences: ${e.message}"
            )
            HttpResponse.serverError(response)
        }
    }

    /**
     * Update user preferences with CORS headers
     */
    @Put("/preferences")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    open fun updatePreferences(
        @Header("Authorization") authToken: String,
        @Body preferences: UserPreferences
    ): HttpResponse<ApiResponse<UserPreferencesResponse>> {
        return try {
            val sessionToken = authToken.removePrefix("Bearer ").trim()
            val session = authService.findActiveSession(sessionToken)

            val response = if (session != null) {
                val updatedPreferences = profileService.updatePreferences(session.user.id!!, preferences)
                if (updatedPreferences != null) {
                    ApiResponse(
                        success = true,
                        data = updatedPreferences.toUserPreferencesResponse(),
                        message = "Preferences updated successfully"
                    )
                } else {
                    ApiResponse(
                        success = false,
                        message = "Preferences not found"
                    )
                }
            } else {
                ApiResponse(
                    success = false,
                    message = "Invalid or expired session"
                )
            }
            HttpResponse.ok(response)
        } catch (e: Exception) {
            val response = ApiResponse<UserPreferencesResponse>(
                success = false,
                message = "Preferences update failed: ${e.message}"
            )
            HttpResponse.serverError(response)
        }
    }

    /**
     * Update parental control PIN with CORS headers
     */
    @Put("/parental-control")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    open fun updateParentalControl(
        @Header("Authorization") authToken: String,
        @Body request: Map<String, String?>
    ): HttpResponse<ApiResponse<Boolean>> {
        return try {
            val sessionToken = authToken.removePrefix("Bearer ").trim()
            val session = authService.findActiveSession(sessionToken)

            val response = if (session != null) {
                val newPin = request["pin"]
                val preferences = profileService.updateParentalControlPin(session.user.id!!, newPin)

                if (preferences != null) {
                    ApiResponse(
                        success = true,
                        data = true,
                        message = if (newPin != null) "Parental control enabled" else "Parental control disabled"
                    )
                } else {
                    ApiResponse(
                        success = false,
                        message = "Failed to update parental control"
                    )
                }
            } else {
                ApiResponse(
                    success = false,
                    message = "Invalid or expired session"
                )
            }
            HttpResponse.ok(response)
        } catch (e: Exception) {
            val response = ApiResponse<Boolean>(
                success = false,
                message = "Failed to update parental control: ${e.message}"
            )
            HttpResponse.serverError(response)
        }
    }

    /**
     * Verify parental control PIN with CORS headers
     */
    @Post("/parental-control/verify")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    open fun verifyParentalControl(
        @Header("Authorization") authToken: String,
        @Body request: Map<String, String>
    ): HttpResponse<ApiResponse<Boolean>> {
        return try {
            val sessionToken = authToken.removePrefix("Bearer ").trim()
            val session = authService.findActiveSession(sessionToken)

            val response = if (session != null) {
                val pin = request["pin"] ?: return HttpResponse.badRequest(
                    ApiResponse<Boolean>(
                        success = false,
                        message = "PIN is required"
                    )
                )

                val isValid = profileService.verifyParentalControlPin(session.user.id!!, pin)
                ApiResponse(
                    success = true,
                    data = isValid,
                    message = if (isValid) "PIN verified" else "Invalid PIN"
                )
            } else {
                ApiResponse(
                    success = false,
                    message = "Invalid or expired session"
                )
            }
            HttpResponse.ok(response)
        } catch (e: Exception) {
            val response = ApiResponse<Boolean>(
                success = false,
                message = "PIN verification failed: ${e.message}"
            )
            HttpResponse.serverError(response)
        }
    }

    // === EXTENSION FUNCTIONS ===

    private fun UserProfile.toProfileResponse(): ProfileResponse {
        return ProfileResponse(
            id = this.id!!,
            userId = this.user.id!!,
            firstName = this.firstName,
            lastName = this.lastName,
            displayName = this.displayName,
            bio = this.bio,
            avatarUrl = this.avatarUrl,
            phoneNumber = this.phoneNumber,
            country = this.country,
            timezone = this.timezone,
            age = this.getAge(),
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }

    private fun UserProfile.toUserProfileResponse(): UserProfileResponse {
        return UserProfileResponse(
            firstName = this.firstName,
            lastName = this.lastName,
            displayName = this.displayName,
            bio = this.bio,
            avatarUrl = this.avatarUrl,
            phoneNumber = this.phoneNumber,
            country = this.country,
            timezone = this.timezone,
            age = this.getAge()
        )
    }

    private fun UserPreferences.toUserPreferencesResponse(): UserPreferencesResponse {
        return UserPreferencesResponse(
            preferredLanguage = this.preferredLanguage,
            preferredQuality = this.preferredQuality.name,
            autoplayEnabled = this.autoplayEnabled,
            subtitlesEnabled = this.subtitlesEnabled,
            subtitleLanguage = this.subtitleLanguage,
            adultContentEnabled = this.adultContentEnabled,
            emailNotifications = this.emailNotifications,
            marketingEmails = this.marketingEmails,
            pushNotifications = this.pushNotifications,
            parentalControlEnabled = this.isParentalControlEnabled()
        )
    }
}