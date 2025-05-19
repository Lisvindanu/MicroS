package com.anaphygon.streaming.model

import io.micronaut.core.annotation.Introspected
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * User Device entity - manages user devices for streaming
 */
@Entity
@Table(
    name = "user_devices",
    indexes = [
        Index(name = "idx_user_devices", columnList = "user_id"),
        Index(name = "idx_device_id", columnList = "device_id"),
        Index(name = "idx_last_used", columnList = "last_used")
    ]
)
@Introspected
data class UserDevice(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(name = "device_name", length = 100, nullable = false)
    var deviceName: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false)
    var deviceType: DeviceType,

    @Column(name = "device_id", length = 255, unique = true, nullable = false)
    var deviceId: String,

    @Column(name = "os_name", length = 50)
    var osName: String? = null,

    @Column(name = "os_version", length = 50)
    var osVersion: String? = null,

    @Column(name = "app_version", length = 50)
    var appVersion: String? = null,

    @Column(name = "is_trusted", nullable = false)
    var isTrusted: Boolean = false,

    @Column(name = "last_used")
    var lastUsed: LocalDateTime? = null,

    @Column(name = "registered_at", nullable = false)
    var registeredAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Update last used timestamp
     */
    fun updateLastUsed() {
        lastUsed = LocalDateTime.now()
    }

    /**
     * Mark device as trusted
     */
    fun markAsTrusted() {
        isTrusted = true
    }

    /**
     * Mark device as untrusted
     */
    fun markAsUntrusted() {
        isTrusted = false
    }

    /**
     * Get device display info
     */
    fun getDisplayInfo(): String {
        return buildString {
            append(deviceName)
            osName?.let { append(" ($it") }
            osVersion?.let { append(" $it") }
            osName?.let { append(")") }
        }
    }

    /**
     * Check if device has been used recently (within last 30 days)
     */
    fun isRecentlyUsed(): Boolean {
        return lastUsed?.isAfter(LocalDateTime.now().minusDays(30)) == true
    }
}