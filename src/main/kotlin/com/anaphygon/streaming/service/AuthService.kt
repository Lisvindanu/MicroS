package com.anaphygon.streaming.service

import com.anaphygon.streaming.model.*
import com.anaphygon.streaming.repository.*
import com.anaphygon.streaming.dto.UserLoginRequest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.Base64

/**
 * Authentication Service - handles login, logout, and session management
 */
@Singleton
@Transactional
open class AuthService @Inject constructor(
    private val userService: UserService,
    private val userSecurityRepository: UserSecurityRepository,
    private val userSessionRepository: UserSessionRepository,
    private val userActivityLogRepository: UserActivityLogRepository
) {

    private val logger = LoggerFactory.getLogger(AuthService::class.java)
    private val secureRandom = SecureRandom()

    /**
     * Authenticate user and create session
     */
    open fun login(request: UserLoginRequest): UserSession? {
        try {
            logger.info("Authentication attempt for: ${request.usernameOrEmail}")

            val user = userService.findByUsernameOrEmail(request.usernameOrEmail)
            if (user == null) {
                logger.warn("User not found: ${request.usernameOrEmail}")
                throw IllegalArgumentException("Invalid username or email")
            }
            logger.info("User found: ${user.username} (ID: ${user.id})")

            // Check if account is active
            if (!user.isActive()) {
                logger.warn("Login attempt for inactive user: ${user.username} (Status: ${user.status})")
                throw IllegalStateException("Account is not active")
            }
            logger.info("User is active")

            // Get security record
            val security = userSecurityRepository.findByUserId(user.id!!)
                .orElse(null)
            if (security == null) {
                logger.error("Security record not found for user: ${user.username}")
                throw IllegalStateException("Account security record not found")
            }
            logger.info("Security record found (ID: ${security.id})")

            // Check if account is locked
            if (security.isAccountLocked()) {
                logger.warn("Login attempt for locked account: ${user.username} (Locked until: ${security.accountLockedUntil})")
                throw IllegalStateException("Account is locked until ${security.accountLockedUntil}")
            }
            logger.info("Account is not locked")

            // Verify password
            logger.info("Attempting password verification for user: ${user.username}")
            logger.info("Stored password hash: ${user.passwordHash}")
            if (!userService.verifyPassword(user, request.password)) {
                // Log failed attempt
                security.incrementFailedLoginAttempts()
                userSecurityRepository.save(security)

                val failLog = UserActivityLog.create(
                    user = user,
                    action = UserAction.LOGIN,
                    description = "Failed login attempt",
                    ipAddress = request.ipAddress,
                    userAgent = request.userAgent
                )
                userActivityLogRepository.save(failLog)

                logger.warn("Invalid password for user: ${user.username} (Failed attempts: ${security.failedLoginAttempts})")
                throw IllegalArgumentException("Invalid password")
            }
            logger.info("Password verified successfully")

            // Reset failed login attempts on successful login
            security.resetFailedLoginAttempts()
            userSecurityRepository.save(security)
            logger.info("Reset failed login attempts")

            // Create new session
            val session = UserSession(
                user = user,
                sessionToken = generateSecureToken(),
                ipAddress = request.ipAddress,
                userAgent = request.userAgent,
                deviceInfo = request.deviceInfo,
                expiresAt = LocalDateTime.now().plusDays(30) // 30 days session
            )
            val savedSession = userSessionRepository.save(session)
            logger.info("Session created successfully (Token: ${savedSession.sessionToken})")

            // Log successful login
            val loginLog = UserActivityLog.createLoginLog(
                user = user,
                ipAddress = request.ipAddress,
                userAgent = request.userAgent,
                deviceInfo = request.deviceInfo
            )
            userActivityLogRepository.save(loginLog)
            logger.info("Login activity logged")

            logger.info("User authenticated successfully: ${user.username}")
            return savedSession
        } catch (e: Exception) {
            logger.error("Error during login process for user: ${request.usernameOrEmail}", e)
            throw e
        }
    }

    /**
     * Logout user (end session)
     */
    open fun logout(sessionToken: String, ipAddress: String? = null, userAgent: String? = null): Boolean {
        val session = userSessionRepository.findBySessionToken(sessionToken)
            .orElse(null) ?: return false

        // End the session
        session.endSession()
        userSessionRepository.save(session)

        // Log logout activity
        val logoutLog = UserActivityLog.createLogoutLog(
            user = session.user,
            ipAddress = ipAddress,
            userAgent = userAgent
        )
        userActivityLogRepository.save(logoutLog)

        logger.info("User logged out: ${session.user.username}")
        return true
    }

    /**
     * Find active session by token
     */
    open fun findActiveSession(sessionToken: String): UserSession? {
        val session = userSessionRepository.findBySessionToken(sessionToken)
            .orElse(null) ?: return null

        return if (session.isValid()) session else null
    }

    /**
     * Validate session and update last activity
     */
    open fun validateAndUpdateSession(sessionToken: String): UserSession? {
        val session = findActiveSession(sessionToken) ?: return null

        // Update last activity
        session.updateLastActivity()
        userSessionRepository.save(session)

        return session
    }

    /**
     * End all user sessions (useful for security purposes)
     */
    open fun endAllUserSessions(userId: Long): Boolean {
        return try {
            userSessionRepository.deactivateAllUserSessions(userId)
            logger.info("All sessions ended for user ID: $userId")
            true
        } catch (e: Exception) {
            logger.error("Failed to end all sessions for user ID: $userId", e)
            false
        }
    }

    /**
     * Count active sessions for user
     */
    open fun countActiveUserSessions(userId: Long): Long {
        return userSessionRepository.countActiveSessionsByUserId(userId)
    }

    /**
     * Generate a secure random token for session
     */
    private fun generateSecureToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}