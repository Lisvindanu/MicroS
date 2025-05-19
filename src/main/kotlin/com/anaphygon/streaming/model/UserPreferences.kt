package com.anaphygon.streaming.model

import io.micronaut.core.annotation.Introspected
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * User Preferences entity - user settings & preferences
 */
@Entity
@Table(name = "user_preferences")
@Introspected
data class UserPreferences(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    var user: User,

    @Column(name = "preferred_language", length = 10, nullable = false)
    var preferredLanguage: String = "id",

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_quality", nullable = false)
    var preferredQuality: VideoQuality = VideoQuality.AUTO,

    @Column(name = "autoplay_enabled", nullable = false)
    var autoplayEnabled: Boolean = true,

    @Column(name = "subtitles_enabled", nullable = false)
    var subtitlesEnabled: Boolean = false,

    @Column(name = "subtitle_language", length = 10, nullable = false)
    var subtitleLanguage: String = "id",

    @Column(name = "adult_content_enabled", nullable = false)
    var adultContentEnabled: Boolean = false,

    @Column(name = "email_notifications", nullable = false)
    var emailNotifications: Boolean = true,

    @Column(name = "marketing_emails", nullable = false)
    var marketingEmails: Boolean = false,

    @Column(name = "push_notifications", nullable = false)
    var pushNotifications: Boolean = true,

    @Column(name = "parental_control_pin", length = 6)
    var parentalControlPin: String? = null,

    @Column(name = "content_filters", columnDefinition = "JSON")
    var contentFilters: String? = null, // JSON array of blocked categories

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Check if parental controls are enabled
     */
    fun isParentalControlEnabled(): Boolean {
        return parentalControlPin != null
    }

    /**
     * Verify parental control PIN
     */
    fun verifyParentalControlPin(pin: String): Boolean {
        return parentalControlPin == pin
    }

    /**
     * Enable parental controls with PIN
     */
    fun enableParentalControl(pin: String) {
        parentalControlPin = pin
        adultContentEnabled = false
        updatedAt = LocalDateTime.now()
    }

    /**
     * Disable parental controls
     */
    fun disableParentalControl() {
        parentalControlPin = null
        updatedAt = LocalDateTime.now()
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
}