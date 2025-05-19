package com.anaphygon.streaming.model

import io.micronaut.core.annotation.Introspected
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * User Security entity
 */
@Entity
@Table(
    name = "user_security",
    indexes = [
        Index(name = "idx_email_token", columnList = "email_verification_token"),
        Index(name = "idx_reset_token", columnList = "password_reset_token")
    ]
)
@Introspected
data class UserSecurity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    var user: User,

    @Column(name = "email_verification_token", length = 255)
    var emailVerificationToken: String? = null,

    @Column(name = "email_verification_expires")
    var emailVerificationExpires: LocalDateTime? = null,

    @Column(name = "password_reset_token", length = 255)
    var passwordResetToken: String? = null,

    @Column(name = "password_reset_expires")
    var passwordResetExpires: LocalDateTime? = null,

    @Column(name = "two_factor_enabled", nullable = false)
    var twoFactorEnabled: Boolean = false,

    @Column(name = "two_factor_secret", length = 32)
    var twoFactorSecret: String? = null,

    @Column(name = "backup_codes", columnDefinition = "JSON")
    var backupCodes: String? = null, // JSON array of backup codes

    @Column(name = "failed_login_attempts", nullable = false)
    var failedLoginAttempts: Int = 0,

    @Column(name = "last_failed_login")
    var lastFailedLogin: LocalDateTime? = null,

    @Column(name = "account_locked_until")
    var accountLockedUntil: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    // Override equals and hashCode to break circular dependency
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserSecurity) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    /**
     * Check if account is currently locked
     */
    fun isAccountLocked(): Boolean {
        return accountLockedUntil?.isAfter(LocalDateTime.now()) == true
    }

    /**
     * Check if email verification token is valid
     */
    fun isEmailVerificationTokenValid(token: String): Boolean {
        return emailVerificationToken == token &&
                emailVerificationExpires?.isAfter(LocalDateTime.now()) == true
    }

    /**
     * Check if password reset token is valid
     */
    fun isPasswordResetTokenValid(token: String): Boolean {
        return passwordResetToken == token &&
                passwordResetExpires?.isAfter(LocalDateTime.now()) == true
    }

    /**
     * Increment failed login attempts
     */
    fun incrementFailedLoginAttempts() {
        failedLoginAttempts++
        lastFailedLogin = LocalDateTime.now()

        // Lock account after 5 failed attempts for 30 minutes
        if (failedLoginAttempts >= 5) {
            accountLockedUntil = LocalDateTime.now().plusMinutes(30)
        }

        updatedAt = LocalDateTime.now()
    }

    /**
     * Reset failed login attempts after successful login
     */
    fun resetFailedLoginAttempts() {
        failedLoginAttempts = 0
        lastFailedLogin = null
        accountLockedUntil = null
        updatedAt = LocalDateTime.now()
    }

    /**
     * Set email verification token
     */
    fun setEmailVerificationToken(token: String, expirationHours: Long = 24) {
        emailVerificationToken = token
        emailVerificationExpires = LocalDateTime.now().plusHours(expirationHours)
        updatedAt = LocalDateTime.now()
    }

    /**
     * Clear email verification token
     */
    fun clearEmailVerificationToken() {
        emailVerificationToken = null
        emailVerificationExpires = null
        updatedAt = LocalDateTime.now()
    }

    /**
     * Set password reset token
     */
    fun setPasswordResetToken(token: String, expirationHours: Long = 24) {
        passwordResetToken = token
        passwordResetExpires = LocalDateTime.now().plusHours(expirationHours)
        updatedAt = LocalDateTime.now()
    }

    /**
     * Clear password reset token
     */
    fun clearPasswordResetToken() {
        passwordResetToken = null
        passwordResetExpires = null
        updatedAt = LocalDateTime.now()
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
}