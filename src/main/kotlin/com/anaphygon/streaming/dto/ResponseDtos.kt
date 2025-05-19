package com.anaphygon.streaming.dto

import java.time.LocalDateTime

/**
 * Standard API response wrapper
 */
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

/**
 * User response DTO for API
 */
data class UserResponse(
    val id: Long,
    val username: String,
    val email: String,
    val emailVerified: Boolean,
    val status: String,
    val currentRole: String?,
    val isPremium: Boolean,
    val createdAt: LocalDateTime
)

/**
 * User profile response DTO
 */
data class UserProfileResponse(
    val firstName: String?,
    val lastName: String?,
    val displayName: String?,
    val bio: String?,
    val avatarUrl: String?,
    val phoneNumber: String?,
    val country: String?,
    val timezone: String?,
    val age: Int?
)

/**
 * Session response DTO
 */
data class SessionResponse(
    val sessionToken: String,
    val expiresAt: LocalDateTime?,
    val user: UserResponse
)

/**
 * User preferences response DTO
 */
data class UserPreferencesResponse(
    val preferredLanguage: String,
    val preferredQuality: String,
    val autoplayEnabled: Boolean,
    val subtitlesEnabled: Boolean,
    val subtitleLanguage: String,
    val adultContentEnabled: Boolean,
    val emailNotifications: Boolean,
    val marketingEmails: Boolean,
    val pushNotifications: Boolean,
    val parentalControlEnabled: Boolean
)

/**
 * Activity log response DTO
 */
data class ActivityLogResponse(
    val id: Long,
    val action: String,
    val description: String?,
    val ipAddress: String?,
    val createdAt: LocalDateTime
)

/**
 * Content response DTO for featured content
 */
data class ContentResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val thumbnailUrl: String?,
    val contentType: String,
    val rating: Double?,
    val releaseYear: Int?,
    val duration: Int?,
    val genres: List<String>,
    val isFeatured: Boolean = false
)

/**
 * Profile response DTO
 */
data class ProfileResponse(
    val id: Long,
    val userId: Long,
    val firstName: String?,
    val lastName: String?,
    val displayName: String?,
    val bio: String?,
    val avatarUrl: String?,
    val phoneNumber: String?,
    val country: String?,
    val timezone: String?,
    val age: Int?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)