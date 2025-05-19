package com.anaphygon.streaming.model

import io.micronaut.core.annotation.Introspected
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * User Subscription entity - manages user subscriptions
 */
@Entity
@Table(
    name = "user_subscriptions",
    indexes = [
        Index(name = "idx_user_subscription", columnList = "user_id"),
        Index(name = "idx_subscription_status", columnList = "status"),
        Index(name = "idx_expires", columnList = "expires_at"),
        Index(name = "idx_type", columnList = "subscription_type")
    ]
)
@Introspected
data class UserSubscription(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_type", nullable = false)
    var subscriptionType: SubscriptionType = SubscriptionType.FREE,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SubscriptionStatus = SubscriptionStatus.ACTIVE,

    @Column(name = "started_at", nullable = false)
    var startedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "expires_at")
    var expiresAt: LocalDateTime? = null,

    @Column(name = "auto_renew", nullable = false)
    var autoRenew: Boolean = true,

    @Column(name = "payment_method_id", length = 255)
    var paymentMethodId: String? = null,

    @Column(name = "price_paid", precision = 10, scale = 2)
    var pricePaid: BigDecimal? = null,

    @Column(length = 3, nullable = false)
    var currency: String = "IDR",

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle")
    var billingCycle: BillingCycle = BillingCycle.MONTHLY,

    @Column(name = "cancelled_at")
    var cancelledAt: LocalDateTime? = null,

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    var cancellationReason: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Check if subscription is currently active
     */
    fun isActive(): Boolean {
        return status == SubscriptionStatus.ACTIVE &&
                (expiresAt == null || expiresAt!!.isAfter(LocalDateTime.now()))
    }

    /**
     * Check if subscription is expired
     */
    fun isExpired(): Boolean {
        return expiresAt?.isBefore(LocalDateTime.now()) == true
    }

    /**
     * Check if subscription is cancelled
     */
    fun isCancelled(): Boolean {
        return status == SubscriptionStatus.CANCELLED || cancelledAt != null
    }

    /**
     * Cancel the subscription
     */
    fun cancel(reason: String? = null) {
        status = SubscriptionStatus.CANCELLED
        cancelledAt = LocalDateTime.now()
        cancellationReason = reason
        autoRenew = false
        updatedAt = LocalDateTime.now()
    }

    /**
     * Suspend the subscription
     */
    fun suspend() {
        status = SubscriptionStatus.SUSPENDED
        updatedAt = LocalDateTime.now()
    }

    /**
     * Reactivate the subscription
     */
    fun reactivate() {
        status = SubscriptionStatus.ACTIVE
        updatedAt = LocalDateTime.now()
    }

    /**
     * Extend subscription
     */
    fun extend(months: Long) {
        val currentExpiration = expiresAt ?: LocalDateTime.now()
        expiresAt = currentExpiration.plusMonths(months)
        if (status == SubscriptionStatus.EXPIRED) {
            status = SubscriptionStatus.ACTIVE
        }
        updatedAt = LocalDateTime.now()
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
}