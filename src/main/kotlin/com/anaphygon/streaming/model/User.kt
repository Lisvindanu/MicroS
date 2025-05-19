package com.anaphygon.streaming.model

import io.micronaut.core.annotation.Introspected
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Core User entity - authentication & basic info
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

    @Column(nullable = false, unique = true, length = 50)
    var username: String,

    @Column(nullable = false, unique = true, length = 100)
    var email: String,

    @Column(nullable = false, length = 255, name = "password_hash")
    var passwordHash: String,

    @Column(name = "email_verified", nullable = false)
    var emailVerified: Boolean = false,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: UserStatus = UserStatus.ACTIVE,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    // === RELATIONSHIPS ===
    @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    var profile: UserProfile? = null,

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    var roles: MutableSet<UserRole> = mutableSetOf(),

    @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    var security: UserSecurity? = null,

    @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    var preferences: UserPreferences? = null,

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    var sessions: MutableSet<UserSession> = mutableSetOf(),

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    var subscriptions: MutableSet<UserSubscription> = mutableSetOf(),

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var devices: MutableSet<UserDevice> = mutableSetOf(),

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var activityLogs: MutableSet<UserActivityLog> = mutableSetOf()
) {
    // Override equals and hashCode to break circular dependency
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    /**
     * Check if user is active
     */
    fun isActive(): Boolean = status == UserStatus.ACTIVE

    /**
     * Get current active role (highest priority role that hasn't expired)
     */
    fun getCurrentRole(): RoleType? {
        return roles.filter {
            it.isActive && (it.expiresAt == null || it.expiresAt!!.isAfter(LocalDateTime.now()))
        }.maxByOrNull { it.role.ordinal }?.role
    }

    /**
     * Check if user has specific role or higher
     */
    fun hasRole(requiredRole: RoleType): Boolean {
        return getCurrentRole()?.let { it.ordinal <= requiredRole.ordinal } ?: false
    }

    /**
     * Get current active subscription
     */
    fun getCurrentSubscription(): UserSubscription? {
        return subscriptions.filter { it.isActive() }
            .maxByOrNull { it.createdAt }
    }

    /**
     * Check if user is premium subscriber
     */
    fun isPremium(): Boolean {
        return getCurrentSubscription()?.subscriptionType in listOf(
            SubscriptionType.PREMIUM,
            SubscriptionType.FAMILY
        ) || hasRole(RoleType.PREMIUM)
    }

    /**
     * Get full name from profile
     */
    fun getFullName(): String? {
        return profile?.getFullName()
    }

    /**
     * Get display name (prioritize profile display name, fallback to username)
     */
    fun getDisplayName(): String {
        return profile?.displayName ?: username
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
}