package com.anaphygon.streaming.repository

import com.anaphygon.streaming.model.*
import io.micronaut.data.annotation.Query
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.*

/**
 * Repository for User entity
 */
@Repository
interface UserRepository : JpaRepository<User, Long> {

    // === BASIC QUERIES ===
    fun findByUsername(username: String): Optional<User>
    fun findByEmail(email: String): Optional<User>
    fun existsByUsername(username: String): Boolean
    fun existsByEmail(email: String): Boolean

    // === AUTHENTICATION QUERIES ===
    @Query("SELECT u FROM User u WHERE (LOWER(u.username) = LOWER(:usernameOrEmail) OR LOWER(u.email) = LOWER(:usernameOrEmail))")
    fun findByUsernameOrEmail(usernameOrEmail: String): Optional<User>

    // === STATUS QUERIES ===
    fun findByStatus(status: UserStatus): List<User>

    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE' ORDER BY u.createdAt DESC")
    fun findAllActiveUsers(): List<User>

    @Query("SELECT u FROM User u WHERE u.emailVerified = false")
    fun findUnverifiedUsers(): List<User>

    // === STATISTICS ===
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = 'ACTIVE'")
    fun countActiveUsers(): Long

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :from AND u.createdAt <= :to")
    fun countUsersByDateRange(from: LocalDateTime, to: LocalDateTime): Long

    // === SEARCH ===
    @Query("""
        SELECT u FROM User u 
        LEFT JOIN u.profile p
        WHERE 
            LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) 
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(p.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(p.displayName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        ORDER BY u.createdAt DESC
    """)
    fun searchUsers(searchTerm: String): List<User>
}

/**
 * Repository for UserProfile entity
 */
@Repository
interface UserProfileRepository : JpaRepository<UserProfile, Long> {

    fun findByUserId(userId: Long): Optional<UserProfile>
    fun findByDisplayName(displayName: String): Optional<UserProfile>

    @Query("SELECT p FROM UserProfile p WHERE LOWER(p.displayName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    fun searchByDisplayName(searchTerm: String): List<UserProfile>

    @Query("SELECT COUNT(p) FROM UserProfile p WHERE p.avatarUrl IS NOT NULL")
    fun countUsersWithAvatar(): Long
}

/**
 * Repository for UserSecurity entity
 */
@Repository
interface UserSecurityRepository : JpaRepository<UserSecurity, Long> {

    fun findByUserId(userId: Long): Optional<UserSecurity>
    fun findByEmailVerificationToken(token: String): Optional<UserSecurity>
    fun findByPasswordResetToken(token: String): Optional<UserSecurity>

    @Query("SELECT us FROM UserSecurity us WHERE us.passwordResetToken = :token AND us.passwordResetExpires > :now")
    fun findByValidPasswordResetToken(token: String, now: LocalDateTime = LocalDateTime.now()): Optional<UserSecurity>

    @Query("SELECT us FROM UserSecurity us WHERE us.emailVerificationToken = :token AND us.emailVerificationExpires > :now")
    fun findByValidEmailVerificationToken(token: String, now: LocalDateTime = LocalDateTime.now()): Optional<UserSecurity>

    @Query("SELECT us FROM UserSecurity us WHERE us.accountLockedUntil < :now AND us.accountLockedUntil IS NOT NULL")
    fun findAccountsToUnlock(now: LocalDateTime = LocalDateTime.now()): List<UserSecurity>

    @Query("SELECT COUNT(us) FROM UserSecurity us WHERE us.failedLoginAttempts > 0")
    fun countAccountsWithFailedAttempts(): Long
}

/**
 * Repository for UserSession entity
 */
@Repository
interface UserSessionRepository : JpaRepository<UserSession, Long> {

    fun findByUserId(userId: Long): List<UserSession>
    fun findBySessionToken(sessionToken: String): Optional<UserSession>

    @Query("SELECT s FROM UserSession s WHERE s.user.id = :userId AND s.isActive = true AND (s.expiresAt IS NULL OR s.expiresAt > :now)")
    fun findActiveSessionsByUserId(userId: Long, now: LocalDateTime = LocalDateTime.now()): List<UserSession>

    @Query("SELECT s FROM UserSession s WHERE s.expiresAt < :now AND s.isActive = true")
    fun findExpiredSessions(now: LocalDateTime = LocalDateTime.now()): List<UserSession>

    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.user.id = :userId")
    fun deactivateAllUserSessions(userId: Long)

    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.user.id = :userId AND s.isActive = true")
    fun countActiveSessionsByUserId(userId: Long): Long

    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.isActive = true")
    fun countAllActiveSessions(): Long
}

/**
 * Repository for UserPreferences entity
 */
@Repository
interface UserPreferencesRepository : JpaRepository<UserPreferences, Long> {

    fun findByUserId(userId: Long): Optional<UserPreferences>

    @Query("SELECT p FROM UserPreferences p WHERE p.preferredLanguage = :language")
    fun findByPreferredLanguage(language: String): List<UserPreferences>

    @Query("SELECT p FROM UserPreferences p WHERE p.parentalControlPin IS NOT NULL")
    fun findUsersWithParentalControl(): List<UserPreferences>

    @Query("SELECT COUNT(p) FROM UserPreferences p WHERE p.adultContentEnabled = true")
    fun countUsersWithAdultContent(): Long
}

/**
 * Repository for UserActivityLog entity
 */
@Repository
interface UserActivityLogRepository : JpaRepository<UserActivityLog, Long> {

    fun findByUserId(userId: Long): List<UserActivityLog>
    fun findByUserIdAndAction(userId: Long, action: UserAction): List<UserActivityLog>
    fun findByAction(action: UserAction): List<UserActivityLog>

    @Query("SELECT l FROM UserActivityLog l WHERE l.user.id = :userId ORDER BY l.createdAt DESC")
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<UserActivityLog>

    @Query("SELECT l FROM UserActivityLog l WHERE l.user.id = :userId AND l.createdAt >= :fromDate ORDER BY l.createdAt DESC")
    fun findRecentActivityByUserId(userId: Long, fromDate: LocalDateTime): List<UserActivityLog>

    @Query("SELECT l FROM UserActivityLog l WHERE l.createdAt >= :fromDate AND l.createdAt <= :toDate")
    fun findByDateRange(fromDate: LocalDateTime, toDate: LocalDateTime): List<UserActivityLog>

    @Query("SELECT l.action, COUNT(l) FROM UserActivityLog l WHERE l.createdAt >= :fromDate GROUP BY l.action")
    fun getActionStatistics(fromDate: LocalDateTime): List<Array<Any>>

    @Query("SELECT COUNT(l) FROM UserActivityLog l WHERE l.user.id = :userId AND l.action = :action AND l.createdAt >= :fromDate")
    fun countUserActivityByAction(userId: Long, action: UserAction, fromDate: LocalDateTime): Long
}