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
 * Authentication Service - handles login, logout, and session management with improved error handling
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
     * Authenticate user and create session with enhanced error handling
     */
    open fun login(request: UserLoginRequest): UserSession? {
        return try {
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
            val userId = user.id ?: throw IllegalStateException("User ID is null")
            val security = userSecurityRepository.findByUserId(userId)
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
            savedSession
        } catch (e: Exception) {
            logger.error("Error during login process for user: ${request.usernameOrEmail}", e)
            throw e
        }
    }

    /**
     * Logout user (end session) with improved error handling
     */
    @Transactional
    open fun logout(sessionToken: String, ipAddress: String? = null, userAgent: String? = null): Boolean {
        return try {
            if (sessionToken.isBlank()) {
                logger.warn("Logout failed: empty session token")
                return false
            }

            // Find session directly from repository
            val session = userSessionRepository.findBySessionToken(sessionToken).orElse(null)
            if (session == null) {
                logger.warn("Logout failed: session not found")
                return false
            }

            // End the session
            session.isActive = false
            session.logoutAt = LocalDateTime.now()
            userSessionRepository.save(session)

            // Log logout activity
            val logoutLog = UserActivityLog.createLogoutLog(
                user = session.user,
                ipAddress = ipAddress,
                userAgent = userAgent
            )
            userActivityLogRepository.save(logoutLog)

            logger.info("User logged out successfully: ${session.user.username}")
            true
        } catch (e: Exception) {
            logger.error("Error during logout for session token: $sessionToken", e)
            false
        }
    }

    /**
     * Find active session by token with null safety
     */
    open fun findActiveSession(sessionToken: String): UserSession? {
        return try {
            if (sessionToken.isBlank()) {
                logger.warn("Find session failed: empty session token")
                return null
            }

            val session = userSessionRepository.findBySessionToken(sessionToken).orElse(null)
            if (session == null) {
                logger.debug("Session not found for token")
                return null
            }

            if (!session.isValid()) {
                logger.debug("Session found but is invalid (expired or inactive)")
                return null
            }

            // Check if user is still valid
            if (session.user.id == null || !session.user.isActive()) {
                logger.warn("Session found but user is invalid or inactive")
                return null
            }

            session
        } catch (e: Exception) {
            logger.error("Error finding active session for token: $sessionToken", e)
            null
        }
    }

    /**
     * Validate session and update last activity with error handling
     */
    @Transactional
    open fun validateAndUpdateSession(sessionToken: String): UserSession? {
        return try {
            if (sessionToken.isBlank()) {
                logger.warn("Session validation failed: empty session token")
                return null
            }

            val session = userSessionRepository.findBySessionToken(sessionToken).orElse(null)
            if (session == null) {
                logger.debug("Session validation failed: session not found")
                return null
            }

            if (!session.isValid()) {
                logger.debug("Session validation failed: session is invalid (expired or inactive)")
                return null
            }

            // Check if user is still valid
            if (session.user.id == null || !session.user.isActive()) {
                logger.warn("Session validation failed: user is invalid or inactive")
                return null
            }

            // Update last activity
            session.updateLastActivity()
            userSessionRepository.save(session)

            logger.debug("Session validated and activity updated for user: ${session.user.username}")
            session
        } catch (e: Exception) {
            logger.error("Error validating session for token: $sessionToken", e)
            null
        }
    }

    /**
     * End all user sessions (useful for security purposes) with error handling
     */
    open fun endAllUserSessions(userId: Long): Boolean {
        return try {
            if (userId <= 0) {
                logger.warn("Invalid user ID for ending sessions: $userId")
                return false
            }

            userSessionRepository.deactivateAllUserSessions(userId)
            logger.info("All sessions ended for user ID: $userId")
            true
        } catch (e: Exception) {
            logger.error("Failed to end all sessions for user ID: $userId", e)
            false
        }
    }

    /**
     * Count active sessions for user with error handling
     */
    open fun countActiveUserSessions(userId: Long): Long {
        return try {
            if (userId <= 0) {
                logger.warn("Invalid user ID for counting sessions: $userId")
                return 0L
            }
            userSessionRepository.countActiveSessionsByUserId(userId)
        } catch (e: Exception) {
            logger.error("Failed to count active sessions for user ID: $userId", e)
            0L
        }
    }

    /**
     * Generate a secure random token for session
     */
    private fun generateSecureToken(): String {
        return try {
            val bytes = ByteArray(32)
            secureRandom.nextBytes(bytes)
            Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        } catch (e: Exception) {
            logger.error("Failed to generate secure token", e)
            // Fallback to a simpler method
            Base64.getEncoder().encodeToString("${System.currentTimeMillis()}-${System.nanoTime()}".toByteArray())
        }
    }
}