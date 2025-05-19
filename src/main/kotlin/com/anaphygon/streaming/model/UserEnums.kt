package com.anaphygon.streaming.model

import io.micronaut.core.annotation.Introspected
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Enums for various user-related states
 */
@Introspected
enum class UserStatus {
    ACTIVE, INACTIVE, SUSPENDED, BANNED
}

@Introspected
enum class RoleType {
    ADMIN, MODERATOR, PREMIUM, USER
}

@Introspected
enum class SubscriptionType {
    FREE, BASIC, PREMIUM, FAMILY
}

@Introspected
enum class SubscriptionStatus {
    ACTIVE, EXPIRED, CANCELLED, SUSPENDED
}

@Introspected
enum class BillingCycle {
    MONTHLY, YEARLY
}

@Introspected
enum class DeviceType {
    MOBILE, TABLET, DESKTOP, TV, CONSOLE
}

@Introspected
enum class VideoQuality {
    AUTO, SD, HD, FHD, UHD
}

@Introspected
enum class UserAction {
    LOGIN, LOGOUT, REGISTER, PROFILE_UPDATE, PASSWORD_CHANGE,
    SUBSCRIPTION_CHANGE, CONTENT_VIEW, SEARCH, REVIEW, RATING
}