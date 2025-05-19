package com.anaphygon.streaming.model

import io.micronaut.core.annotation.Introspected
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * User Profile entity - profile information
 */
@Entity
@Table(
    name = "user_profiles",
    indexes = [
        Index(name = "idx_display_name", columnList = "display_name")
    ]
)
@Introspected
data class UserProfile(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    var user: User,

    @Column(name = "first_name", length = 50)
    var firstName: String? = null,

    @Column(name = "last_name", length = 50)
    var lastName: String? = null,

    @Column(name = "display_name", length = 100)
    var displayName: String? = null,

    @Column(length = 500)
    var bio: String? = null,

    @Column(name = "avatar_url", length = 255)
    var avatarUrl: String? = null,

    @Column(name = "birth_date")
    var birthDate: LocalDate? = null,

    @Column(name = "phone_number", length = 20)
    var phoneNumber: String? = null,

    @Column(length = 50)
    var country: String? = null,

    @Column(length = 50)
    var timezone: String? = null,

    @Column(length = 10)
    var language: String = "id",

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Get full name
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
     * Get age from birth date
     */
    fun getAge(): Int? {
        return birthDate?.let {
            val today = LocalDate.now()
            var age = today.year - it.year
            if (today.month < it.month || (today.month == it.month && today.dayOfMonth < it.dayOfMonth)) {
                age--
            }
            age
        }
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
}