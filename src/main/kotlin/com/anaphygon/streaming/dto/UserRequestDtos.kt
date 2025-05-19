package com.anaphygon.streaming.dto

/**
 * User registration request DTO
 */
data class UserRegistrationRequest(
    val username: String,
    val email: String,
    val password: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val displayName: String? = null
)

/**
 * User login request DTO
 */
data class UserLoginRequest(
    val usernameOrEmail: String,
    val password: String,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val deviceInfo: String? = null
) {
    init {
        require(usernameOrEmail.isNotBlank()) { "Username or email is required" }
        require(password.isNotBlank()) { "Password is required" }
    }
}

/**
 * User profile update request DTO
 */
data class UserProfileUpdateRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val displayName: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val phoneNumber: String? = null,
    val country: String? = null,
    val timezone: String? = null
)