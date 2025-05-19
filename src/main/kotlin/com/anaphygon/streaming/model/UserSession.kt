package com.anaphygon.streaming.model

import io.micronaut.core.annotation.Introspected
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * User Session entity - tracks user login sessions
 */
@Entity
@Table(
    name = "user_sessions",
    indexes = [
        Index(name = "idx_session_token", columnList = "session_token"),
        Index(name = "idx_user_sessions", columnList = "user_id"),
        Index(name = "idx_active_sessions", columnList = "user_id,is_active"),
        Index(name = "idx_login_time", columnList = "login_at")
    ]
)
@Introspected
data class UserSession(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(name = "session_token", nullable = false, unique = true, length = 255)
    var sessionToken: String,

    @Column(name = "ip_address", length = 45)
    var ipAddress: String? = null,

    @Column(name = "user_agent", columnDefinition = "TEXT")
    var userAgent: String? = null,

    @Column(name = "device_info", length = 255)
    var deviceInfo: String? = null,

    @Column(name = "login_at", nullable = false)
    var loginAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "last_activity")
    var lastActivity: LocalDateTime? = null,

    @Column(name = "logout_at")
    var logoutAt: LocalDateTime? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "expires_at")
    var expiresAt: LocalDateTime? = null
) {
    /**
     * Check if session is valid (active and not expired)
     */
    fun isValid(): Boolean {
        return isActive && (expiresAt == null || expiresAt!!.isAfter(LocalDateTime.now()))
    }

    /**
     * Update last activity timestamp
     */
    fun updateLastActivity() {
        lastActivity = LocalDateTime.now()
    }

    /**
     * End session
     */
    fun endSession() {
        isActive = false
        logoutAt = LocalDateTime.now()
    }

    /**
     * Extend session expiration
     */
    fun extendSession(days: Int = 30) {
        expiresAt = LocalDateTime.now().plusDays(days.toLong())
    }
}