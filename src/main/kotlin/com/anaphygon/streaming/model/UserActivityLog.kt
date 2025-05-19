package com.anaphygon.streaming.model

import io.micronaut.core.annotation.Introspected
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * User Activity Log entity - audit trail for user actions
 */
@Entity
@Table(
    name = "user_activity_log",
    indexes = [
        Index(name = "idx_user_activity", columnList = "user_id"),
        Index(name = "idx_action", columnList = "action"),
        Index(name = "idx_created_at", columnList = "created_at")
    ]
)
@Introspected
data class UserActivityLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var action: UserAction,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(columnDefinition = "JSON")
    var metadata: String? = null, // Additional JSON data

    @Column(name = "ip_address", length = 45)
    var ipAddress: String? = null,

    @Column(name = "user_agent", columnDefinition = "TEXT")
    var userAgent: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Create activity log entry with basic info
     */
    companion object {
        fun create(
            user: User,
            action: UserAction,
            description: String? = null,
            metadata: String? = null,
            ipAddress: String? = null,
            userAgent: String? = null
        ): UserActivityLog {
            return UserActivityLog(
                user = user,
                action = action,
                description = description,
                metadata = metadata,
                ipAddress = ipAddress,
                userAgent = userAgent
            )
        }

        /**
         * Create login activity log
         */
        fun createLoginLog(
            user: User,
            ipAddress: String? = null,
            userAgent: String? = null,
            deviceInfo: String? = null
        ): UserActivityLog {
            return create(
                user = user,
                action = UserAction.LOGIN,
                description = "User logged in",
                metadata = deviceInfo?.let { """{"device": "$it"}""" },
                ipAddress = ipAddress,
                userAgent = userAgent
            )
        }

        /**
         * Create logout activity log
         */
        fun createLogoutLog(
            user: User,
            ipAddress: String? = null,
            userAgent: String? = null
        ): UserActivityLog {
            return create(
                user = user,
                action = UserAction.LOGOUT,
                description = "User logged out",
                ipAddress = ipAddress,
                userAgent = userAgent
            )
        }

        /**
         * Create profile update activity log
         */
        fun createProfileUpdateLog(
            user: User,
            changedFields: List<String>,
            ipAddress: String? = null,
            userAgent: String? = null
        ): UserActivityLog {
            val fieldsJson = changedFields.joinToString("\", \"", "[\"", "\"]")
            return create(
                user = user,
                action = UserAction.PROFILE_UPDATE,
                description = "Profile updated: ${changedFields.joinToString(", ")}",
                metadata = """{"changed_fields": $fieldsJson}""",
                ipAddress = ipAddress,
                userAgent = userAgent
            )
        }

        /**
         * Create password change activity log
         */
        fun createPasswordChangeLog(
            user: User,
            ipAddress: String? = null,
            userAgent: String? = null
        ): UserActivityLog {
            return create(
                user = user,
                action = UserAction.PASSWORD_CHANGE,
                description = "Password changed",
                ipAddress = ipAddress,
                userAgent = userAgent
            )
        }

        /**
         * Create subscription change activity log
         */
        fun createSubscriptionChangeLog(
            user: User,
            oldType: SubscriptionType?,
            newType: SubscriptionType,
            ipAddress: String? = null,
            userAgent: String? = null
        ): UserActivityLog {
            return create(
                user = user,
                action = UserAction.SUBSCRIPTION_CHANGE,
                description = "Subscription changed from ${oldType ?: "none"} to $newType",
                metadata = """{"old_type": "${oldType ?: "none"}", "new_type": "$newType"}""",
                ipAddress = ipAddress,
                userAgent = userAgent
            )
        }

        /**
         * Create content view activity log
         */
        fun createContentViewLog(
            user: User,
            contentId: Long,
            contentTitle: String,
            contentType: String,
            watchDuration: Long? = null,
            ipAddress: String? = null,
            userAgent: String? = null
        ): UserActivityLog {
            val metadataMap = mutableMapOf(
                "content_id" to contentId.toString(),
                "content_title" to contentTitle,
                "content_type" to contentType
            )
            watchDuration?.let { metadataMap["watch_duration"] = it.toString() }

            val metadata = metadataMap.entries.joinToString(
                prefix = "{",
                postfix = "}",
                separator = ", "
            ) { "\"${it.key}\": \"${it.value}\"" }

            return create(
                user = user,
                action = UserAction.CONTENT_VIEW,
                description = "Viewed $contentType: $contentTitle",
                metadata = metadata,
                ipAddress = ipAddress,
                userAgent = userAgent
            )
        }

        /**
         * Create search activity log
         */
        fun createSearchLog(
            user: User,
            searchQuery: String,
            resultsCount: Int,
            ipAddress: String? = null,
            userAgent: String? = null
        ): UserActivityLog {
            return create(
                user = user,
                action = UserAction.SEARCH,
                description = "Searched for: $searchQuery",
                metadata = """{"query": "$searchQuery", "results_count": $resultsCount}""",
                ipAddress = ipAddress,
                userAgent = userAgent
            )
        }

        /**
         * Create review activity log
         */
        fun createReviewLog(
            user: User,
            contentId: Long,
            contentTitle: String,
            rating: Double?,
            ipAddress: String? = null,
            userAgent: String? = null
        ): UserActivityLog {
            return create(
                user = user,
                action = UserAction.REVIEW,
                description = "Reviewed: $contentTitle${rating?.let { " (Rating: $it)" } ?: ""}",
                metadata = """{"content_id": $contentId, "content_title": "$contentTitle"${rating?.let { """, "rating": $it""" } ?: ""}}""",
                ipAddress = ipAddress,
                userAgent = userAgent
            )
        }

        /**
         * Create rating activity log
         */
        fun createRatingLog(
            user: User,
            contentId: Long,
            contentTitle: String,
            rating: Double,
            ipAddress: String? = null,
            userAgent: String? = null
        ): UserActivityLog {
            return create(
                user = user,
                action = UserAction.RATING,
                description = "Rated $contentTitle: $rating",
                metadata = """{"content_id": $contentId, "content_title": "$contentTitle", "rating": $rating}""",
                ipAddress = ipAddress,
                userAgent = userAgent
            )
        }
    }

    /**
     * Check if this log is from today
     */
    fun isFromToday(): Boolean {
        val today = LocalDateTime.now().toLocalDate()
        return createdAt.toLocalDate() == today
    }

    /**
     * Check if this log is recent (within last hour)
     */
    fun isRecent(): Boolean {
        return createdAt.isAfter(LocalDateTime.now().minusHours(1))
    }

    /**
     * Get formatted action description
     */
    fun getFormattedAction(): String {
        return action.name.lowercase()
            .split('_')
            .joinToString(" ") { it.capitalize() }
    }

    /**
     * Get short description for display
     */
    fun getShortDescription(): String {
        return description?.let {
            if (it.length > 100) it.substring(0, 97) + "..." else it
        } ?: getFormattedAction()
    }
}