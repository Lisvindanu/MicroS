package com.anaphygon.streaming.model

import io.micronaut.core.annotation.Introspected
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * User Role entity
 */
@Entity
@Table(
    name = "user_roles",
    indexes = [
        Index(name = "idx_user_role", columnList = "user_id,role"),
        Index(name = "idx_role", columnList = "role"),
        Index(name = "idx_expires", columnList = "expires_at")
    ]
)
@Introspected
data class UserRole(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: RoleType = RoleType.USER,

    @Column(name = "assigned_at", nullable = false)
    var assignedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "assigned_by")
    var assignedBy: Long? = null,

    @Column(name = "expires_at")
    var expiresAt: LocalDateTime? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
) {
    /**
     * Check if role is currently valid (active and not expired)
     */
    fun isValid(): Boolean {
        return isActive && (expiresAt == null || expiresAt!!.isAfter(LocalDateTime.now()))
    }

    /**
     * Check if role is expired
     */
    fun isExpired(): Boolean {
        return expiresAt?.isBefore(LocalDateTime.now()) == true
    }
}