package com.anaphygon.streaming.controller

import com.anaphygon.streaming.dto.*
import com.anaphygon.streaming.model.*
import com.anaphygon.streaming.service.*
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject

/**
 * Profile Controller - handles user profile and preferences management
 */
@Controller("/api/profile")
open class ProfileController @Inject constructor(
    private val authService: AuthService,
    private val profileService: ProfileService
) {

    /**
     * Get current user profile
     */
    @Get("/")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    open fun getProfile(@Header("Authorization") authToken: String): ApiResponse<UserProfileResponse> {
        return try {
            val sessionToken = authToken.removePrefix("Bearer ").trim()
            val session = authService.findActiveSession(sessionToken)

            if (session != null) {
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
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Failed to get profile: ${e.message}"
            )
        }
    }

    /**
     * Update user profile
     */
    @Put("/")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    open fun updateProfile(
        @Header("Authorization") authToken: String,
        @Body request: UserProfileUpdateRequest
    ): ApiResponse<UserProfileResponse> {
        return try {
            val sessionToken = authToken.removePrefix("Bearer ").trim()
            val session = authService.findActiveSession(sessionToken)

            if (session != null) {
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
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Profile update failed: ${e.message}"
            )
        }
    }

    /**
     * Get user preferences
     */
    @Get("/preferences")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    open fun getPreferences(@Header("Authorization") authToken: String): ApiResponse<UserPreferencesResponse> {
        return try {
            val sessionToken = authToken.removePrefix("Bearer ").trim()
            val session = authService.findActiveSession(sessionToken)

            if (session != null) {
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
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Failed to get preferences: ${e.message}"
            )
        }
    }

    /**
     * Update user preferences
     */
    @Put("/preferences")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    open fun updatePreferences(
        @Header("Authorization") authToken: String,
        @Body preferences: UserPreferences
    ): ApiResponse<UserPreferencesResponse> {
        return try {
            val sessionToken = authToken.removePrefix("Bearer ").trim()
            val session = authService.findActiveSession(sessionToken)

            if (session != null) {
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
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Preferences update failed: ${e.message}"
            )
        }
    }

    /**
     * Update parental control PIN
     */
    @Put("/parental-control")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    open fun updateParentalControl(
        @Header("Authorization") authToken: String,
        @Body request: Map<String, String?>
    ): ApiResponse<Boolean> {
        return try {
            val sessionToken = authToken.removePrefix("Bearer ").trim()
            val session = authService.findActiveSession(sessionToken)

            if (session != null) {
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
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Failed to update parental control: ${e.message}"
            )
        }
    }

    /**
     * Verify parental control PIN
     */
    @Post("/parental-control/verify")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    open fun verifyParentalControl(
        @Header("Authorization") authToken: String,
        @Body request: Map<String, String>
    ): ApiResponse<Boolean> {
        return try {
            val sessionToken = authToken.removePrefix("Bearer ").trim()
            val session = authService.findActiveSession(sessionToken)

            if (session != null) {
                val pin = request["pin"] ?: return ApiResponse(
                    success = false,
                    message = "PIN is required"
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
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "PIN verification failed: ${e.message}"
            )
        }
    }

    // === EXTENSION FUNCTIONS ===

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