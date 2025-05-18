package com.anaphygon.streaming.model

import io.micronaut.core.annotation.Introspected
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Enum untuk user roles dalam streaming platform
 */
@Introspected
enum class UserRole {
    ADMIN,      // Full system access
    MODERATOR,  // Content moderation
    PREMIUM,    // Premium subscription
    USER        // Regular user
}

/**
 * Enum untuk user status
 */
@Introspected
enum class UserStatus {
    ACTIVE,     // Active user
    INACTIVE,   // Temporarily disabled
    SUSPENDED,  // Suspended by admin
    BANNED      // Permanently banned
}

/**
 * User entity untuk streaming platform
 * Includes security features dan role management
 */
@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_username", columnList = "username"),
        Index(name = "idx_email", columnList = "email"),
        Index(name = "idx_status", columnList = "status"),
        Index(name = "idx_created_at", columnList = "created_at")
    ]
)
@Introspected
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    // === AUTHENTICATION FIELDS ===
    @Column(nullable = false, unique = true, length = 50)
    var username: String,

    @Column(nullable = false, unique = true, length = 100)
    var email: String,

    // Password akan di-hash menggunakan BCrypt
    @Column(nullable = false, length = 255)
    var passwordHash: String,

    // === PROFILE FIELDS ===
    @Column(name = "first_name", length = 50)
    var firstName: String? = null,

    @Column(name = "last_name", length = 50)
    var lastName: String? = null,

    @Column(name = "display_name", length = 100)
    var displayName: String? = null,

    @Column(name = "avatar_url", length = 255)
    var avatarUrl: String? = null,

    @Column(name = "bio", length = 500)
    var bio: String? = null,

    @Column(name = "birth_date")
    var birthDate: LocalDateTime? = null,

    // === ROLE & PERMISSIONS ===
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: UserRole = UserRole.USER,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: UserStatus = UserStatus.ACTIVE,

    // === SECURITY FIELDS ===
    @Column(name = "email_verified")
    var emailVerified: Boolean = false,

    @Column(name = "email_verification_token", length = 255)
    var emailVerificationToken: String? = null,

    @Column(name = "password_reset_token", length = 255)
    var passwordResetToken: String? = null,

    @Column(name = "password_reset_expires")
    var passwordResetExpires: LocalDateTime? = null,

    // Two-factor authentication
    @Column(name = "two_factor_enabled")
    var twoFactorEnabled: Boolean = false,

    @Column(name = "two_factor_secret", length = 32)
    var twoFactorSecret: String? = null,

    // === TRACKING FIELDS ===
    @Column(name = "last_login")
    var lastLogin: LocalDateTime? = null,

    @Column(name = "login_count")
    var loginCount: Long = 0,

    @Column(name = "last_ip_address", length = 45) // IPv6 compatible
    var lastIpAddress: String? = null,

    // === SUBSCRIPTION FIELDS ===
    @Column(name = "subscription_type", length = 20)
    var subscriptionType: String? = null,

    @Column(name = "subscription_expires")
    var subscriptionExpires: LocalDateTime? = null,

    // === AUDIT FIELDS ===
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "created_by")
    var createdBy: Long? = null,

    @Column(name = "updated_by")
    var updatedBy: Long? = null,

    // Soft delete - instead of actually deleting records
    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,

    @Column(name = "deleted_by")
    var deletedBy: Long? = null
) {

    /**
     * Get full name (first + last name)
     */
    fun getFullName(): String? {
        return when {
            firstName != null && lastName != null -> "$firstName $lastName"
            firstName != null -> firstName
            lastName != null -> lastName
            else -> null
        }
    }

    /**
     * Check if user is active
     */
    fun isActive(): Boolean = status == UserStatus.ACTIVE && deletedAt == null

    /**
     * Check if user has specific role
     */
    fun hasRole(requiredRole: UserRole): Boolean = this.role == requiredRole

    /**
     * Check if user has role level (e.g., ADMIN has higher level than USER)
     */
    fun hasRoleLevel(requiredRole: UserRole): Boolean {
        return this.role.ordinal <= requiredRole.ordinal
    }

    /**
     * Check if user is premium subscriber
     */
    fun isPremium(): Boolean {
        return role == UserRole.PREMIUM ||
                (subscriptionExpires != null && subscriptionExpires!!.isAfter(LocalDateTime.now()))
    }

    /**
     * Update last login information
     */
    fun updateLastLogin(ipAddress: String? = null) {
        lastLogin = LocalDateTime.now()
        loginCount += 1
        ipAddress?.let { lastIpAddress = it }
        updatedAt = LocalDateTime.now()
    }

    /**
     * Mark as deleted (soft delete)
     */
    fun markAsDeleted(deletedBy: Long? = null) {
        this.deletedAt = LocalDateTime.now()
        this.deletedBy = deletedBy
        this.status = UserStatus.INACTIVE
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * Update password reset token
     */
    fun setPasswordResetToken(token: String, expirationHours: Long = 24) {
        this.passwordResetToken = token
        this.passwordResetExpires = LocalDateTime.now().plusHours(expirationHours)
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * Clear password reset token
     */
    fun clearPasswordResetToken() {
        this.passwordResetToken = null
        this.passwordResetExpires = null
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * Update entity timestamp automatically
     */
    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
}