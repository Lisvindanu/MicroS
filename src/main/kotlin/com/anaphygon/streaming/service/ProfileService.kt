package com.anaphygon.streaming.service

import com.anaphygon.streaming.model.*
import com.anaphygon.streaming.repository.*
import com.anaphygon.streaming.dto.UserProfileUpdateRequest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

/**
 * Profile Service - handles user profile and preferences management
 */
@Singleton
@Transactional
open class ProfileService @Inject constructor(
    private val userService: UserService,
    private val userProfileRepository: UserProfileRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val userActivityLogRepository: UserActivityLogRepository
) {

    private val logger = LoggerFactory.getLogger(ProfileService::class.java)

    /**
     * Get profile by ID
     */
    open fun getProfile(id: Long): UserProfile? {
        return userProfileRepository.findById(id).orElse(null)
    }

    /**
     * Create initial profile for new user
     */
    open fun createInitialProfile(user: User, firstName: String? = null, lastName: String? = null, displayName: String? = null): UserProfile {
        val profile = UserProfile(
            user = user,
            firstName = firstName,
            lastName = lastName,
            displayName = displayName ?: user.username
        )

        val savedProfile = userProfileRepository.save(profile)
        logger.info("Initial profile created for user: ${user.username}")
        return savedProfile
    }

    /**
     * Get user profile by user ID
     */
    open fun getUserProfile(userId: Long): UserProfile? {
        return userProfileRepository.findByUserId(userId).orElse(null)
    }

    /**
     * Update user profile
     */
    open fun updateProfile(userId: Long, request: UserProfileUpdateRequest): UserProfile? {
        val profile = userProfileRepository.findByUserId(userId)
            .orElse(null) ?: return null

        val changedFields = mutableListOf<String>()

        // Check and update each field
        request.firstName?.let {
            if (profile.firstName != it) {
                profile.firstName = it
                changedFields.add("firstName")
            }
        }
        request.lastName?.let {
            if (profile.lastName != it) {
                profile.lastName = it
                changedFields.add("lastName")
            }
        }
        request.displayName?.let {
            if (profile.displayName != it) {
                profile.displayName = it
                changedFields.add("displayName")
            }
        }
        request.bio?.let {
            if (profile.bio != it) {
                profile.bio = it
                changedFields.add("bio")
            }
        }
        request.avatarUrl?.let {
            if (profile.avatarUrl != it) {
                profile.avatarUrl = it
                changedFields.add("avatarUrl")
            }
        }
        request.phoneNumber?.let {
            if (profile.phoneNumber != it) {
                profile.phoneNumber = it
                changedFields.add("phoneNumber")
            }
        }
        request.country?.let {
            if (profile.country != it) {
                profile.country = it
                changedFields.add("country")
            }
        }
        request.timezone?.let {
            if (profile.timezone != it) {
                profile.timezone = it
                changedFields.add("timezone")
            }
        }

        // Save if there are changes
        if (changedFields.isNotEmpty()) {
            val savedProfile = userProfileRepository.save(profile)

            // Log profile update
            val updateLog = UserActivityLog.createProfileUpdateLog(
                user = profile.user,
                changedFields = changedFields
            )
            userActivityLogRepository.save(updateLog)

            logger.info("Profile updated for user ${profile.user.username}: ${changedFields.joinToString(", ")}")
            return savedProfile
        }

        return profile
    }

    /**
     * Create initial preferences for new user
     */
    open fun createInitialPreferences(user: User): UserPreferences {
        val preferences = UserPreferences(user = user)
        val savedPreferences = userPreferencesRepository.save(preferences)
        logger.info("Initial preferences created for user: ${user.username}")
        return savedPreferences
    }

    /**
     * Get user preferences
     */
    open fun getUserPreferences(userId: Long): UserPreferences? {
        return userPreferencesRepository.findByUserId(userId).orElse(null)
    }

    /**
     * Update user preferences
     */
    open fun updatePreferences(userId: Long, preferences: UserPreferences): UserPreferences? {
        val existingPrefs = userPreferencesRepository.findByUserId(userId)
            .orElse(null) ?: return null

        // Update preferences
        existingPrefs.preferredLanguage = preferences.preferredLanguage
        existingPrefs.preferredQuality = preferences.preferredQuality
        existingPrefs.autoplayEnabled = preferences.autoplayEnabled
        existingPrefs.subtitlesEnabled = preferences.subtitlesEnabled
        existingPrefs.subtitleLanguage = preferences.subtitleLanguage
        existingPrefs.adultContentEnabled = preferences.adultContentEnabled
        existingPrefs.emailNotifications = preferences.emailNotifications
        existingPrefs.marketingEmails = preferences.marketingEmails
        existingPrefs.pushNotifications = preferences.pushNotifications
        existingPrefs.contentFilters = preferences.contentFilters

        val savedPrefs = userPreferencesRepository.save(existingPrefs)
        logger.info("Preferences updated for user ID: $userId")
        return savedPrefs
    }

    /**
     * Update parental control PIN
     */
    open fun updateParentalControlPin(userId: Long, newPin: String?): UserPreferences? {
        val preferences = userPreferencesRepository.findByUserId(userId)
            .orElse(null) ?: return null

        if (newPin != null) {
            preferences.enableParentalControl(newPin)
        } else {
            preferences.disableParentalControl()
        }

        val savedPrefs = userPreferencesRepository.save(preferences)
        logger.info("Parental control ${if (newPin != null) "enabled" else "disabled"} for user ID: $userId")
        return savedPrefs
    }

    /**
     * Verify parental control PIN
     */
    open fun verifyParentalControlPin(userId: Long, pin: String): Boolean {
        val preferences = userPreferencesRepository.findByUserId(userId)
            .orElse(null) ?: return false

        return preferences.verifyParentalControlPin(pin)
    }
}