package com.anaphygon.streaming.service

import com.anaphygon.streaming.model.*
import com.anaphygon.streaming.repository.UserRepository
import com.anaphygon.streaming.repository.UserSecurityRepository
import com.anaphygon.streaming.dto.UserRegistrationRequest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import org.mindrot.jbcrypt.BCrypt

/**
 * Core User Service - handles basic user CRUD operations with improved error handling
 */
@Singleton
@Transactional
open class UserService @Inject constructor(
    private val userRepository: UserRepository,
    private val userSecurityRepository: UserSecurityRepository
) {

    private val logger = LoggerFactory.getLogger(UserService::class.java)

    /**
     * Create new user with enhanced validation and error handling
     */
    open fun createUser(request: UserRegistrationRequest): User {
        try {
            logger.info("Creating user: ${request.username}")

            // Enhanced validation
            if (request.username.isBlank()) {
                throw IllegalArgumentException("Username cannot be empty")
            }
            if (request.email.isBlank()) {
                throw IllegalArgumentException("Email cannot be empty")
            }
            if (request.password.isBlank()) {
                throw IllegalArgumentException("Password cannot be empty")
            }

            // Validate uniqueness
            if (userRepository.existsByUsername(request.username)) {
                throw IllegalArgumentException("Username '${request.username}' already exists")
            }

            if (userRepository.existsByEmail(request.email)) {
                throw IllegalArgumentException("Email '${request.email}' already exists")
            }

            // Hash password with proper error handling
            logger.info("Hashing password for user: ${request.username}")
            val hashedPassword = hashPassword(request.password)
            if (hashedPassword.isBlank()) {
                throw IllegalStateException("Failed to hash password")
            }
            logger.info("Password hashed successfully")

            // Create and save user
            val user = User(
                username = request.username.trim(),
                email = request.email.trim().lowercase(),
                passwordHash = hashedPassword,
                emailVerified = false,
                status = UserStatus.ACTIVE
            )

            val savedUser = userRepository.save(user)
            if (savedUser.id == null) {
                throw IllegalStateException("Failed to save user - ID is null")
            }
            logger.info("User created with ID: ${savedUser.id}")

            // Create security record
            val security = UserSecurity(user = savedUser)
            userSecurityRepository.save(security)
            logger.info("Security record created for user: ${savedUser.username}")

            // Create default profile
            val profile = UserProfile(
                user = savedUser,
                displayName = savedUser.username
            )
            savedUser.profile = profile
            logger.info("Default profile created for user: ${savedUser.username}")

            // Create default preferences
            val preferences = UserPreferences(
                user = savedUser,
                preferredLanguage = "id",
                preferredQuality = VideoQuality.AUTO,
                autoplayEnabled = true,
                subtitlesEnabled = false,
                subtitleLanguage = "id",
                emailNotifications = true,
                pushNotifications = true
            )
            savedUser.preferences = preferences
            logger.info("Default preferences created for user: ${savedUser.username}")

            // Assign default MEMBER role
            val defaultRole = UserRole(
                user = savedUser,
                role = RoleType.USER,
                isActive = true
            )
            savedUser.roles.add(defaultRole)
            userRepository.save(savedUser)
            logger.info("Default MEMBER role assigned to user: ${savedUser.username}")

            return savedUser
        } catch (e: IllegalArgumentException) {
            logger.error("User creation failed with validation error: ${e.message}")
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error creating user: ${request.username}", e)
            throw IllegalStateException("Failed to create user: ${e.message}")
        }
    }

    /**
     * Find user by ID with null safety
     */
    open fun findById(id: Long): User? {
        return try {
            if (id <= 0) {
                logger.warn("Invalid user ID: $id")
                return null
            }
            userRepository.findById(id).orElse(null)
        } catch (e: Exception) {
            logger.error("Error finding user by ID: $id", e)
            null
        }
    }

    /**
     * Find user by username with null safety
     */
    open fun findByUsername(username: String): User? {
        return try {
            if (username.isBlank()) {
                logger.warn("Empty username provided")
                return null
            }
            userRepository.findByUsername(username.trim()).orElse(null)
        } catch (e: Exception) {
            logger.error("Error finding user by username: $username", e)
            null
        }
    }

    /**
     * Find user by email with null safety
     */
    open fun findByEmail(email: String): User? {
        return try {
            if (email.isBlank()) {
                logger.warn("Empty email provided")
                return null
            }
            userRepository.findByEmail(email.trim().lowercase()).orElse(null)
        } catch (e: Exception) {
            logger.error("Error finding user by email: $email", e)
            null
        }
    }

    /**
     * Find user by username or email with enhanced error handling
     */
    open fun findByUsernameOrEmail(usernameOrEmail: String): User? {
        return try {
            if (usernameOrEmail.isBlank()) {
                logger.warn("Empty username/email provided")
                return null
            }
            
            val trimmed = usernameOrEmail.trim()
            logger.debug("Searching for user: $trimmed")
            
            // First try exact match
            var user = userRepository.findByUsernameOrEmail(trimmed).orElse(null)
            
            // If not found and looks like email, try lowercase
            if (user == null && trimmed.contains("@")) {
                logger.debug("Trying lowercase email search")
                user = userRepository.findByUsernameOrEmail(trimmed.lowercase()).orElse(null)
            }
            
            if (user != null) {
                // Initialize roles collection
                user.roles.size
                logger.debug("User found: ${user.username} (ID: ${user.id})")
            } else {
                logger.debug("User not found for: $trimmed")
            }
            
            user
        } catch (e: Exception) {
            logger.error("Error finding user by username/email: $usernameOrEmail", e)
            null
        }
    }

    /**
     * Update user status with validation
     */
    open fun updateUserStatus(userId: Long, status: UserStatus): User? {
        return try {
            if (userId <= 0) {
                logger.warn("Invalid user ID: $userId")
                return null
            }

            val user = findById(userId) ?: return null
            user.status = status
            user.updatedAt = LocalDateTime.now()
            
            val savedUser = userRepository.save(user)
            logger.info("Updated status for user ${user.username} to ${status}")
            savedUser
        } catch (e: Exception) {
            logger.error("Error updating user status for ID: $userId", e)
            null
        }
    }

    /**
     * Verify user password with comprehensive error handling
     */
    open fun verifyPassword(user: User, password: String): Boolean {
        return try {
            if (password.isBlank()) {
                logger.warn("Empty password provided for verification")
                return false
            }

            if (user.passwordHash.isNullOrBlank()) {
                logger.error("Password hash is null or empty for user: ${user.username}")
                return false
            }
            
            logger.debug("Verifying password for user: ${user.username}")
            logger.debug("Stored hash: ${user.passwordHash}")
            logger.debug("Input password length: ${password.length}")
            
            val result = when {
                // Check if the stored hash is BCrypt (starts with $2)
                user.passwordHash.startsWith("$2") -> {
                    logger.debug("Using BCrypt verification")
                    try {
                        val bcryptResult = BCrypt.checkpw(password, user.passwordHash)
                        logger.debug("BCrypt verification result: $bcryptResult")
                        bcryptResult
                    } catch (e: Exception) {
                        logger.error("BCrypt verification failed for user: ${user.username}", e)
                        false
                    }
                }
                // Fallback to plain text comparison (for legacy or test purposes)
                else -> {
                    logger.debug("Using plain text verification (not recommended for production)")
                    val plainResult = user.passwordHash == password
                    logger.debug("Plain text verification result: $plainResult")
                    plainResult
                }
            }
            
            logger.info("Password verification for user ${user.username}: ${if (result) "SUCCESS" else "FAILED"}")
            result
        } catch (e: Exception) {
            logger.error("Error verifying password for user: ${user.username}", e)
            false
        }
    }

    /**
     * Update user password with proper hashing
     */
    open fun updatePassword(userId: Long, newPassword: String): User? {
        return try {
            if (userId <= 0) {
                logger.warn("Invalid user ID: $userId")
                return null
            }

            if (newPassword.isBlank()) {
                throw IllegalArgumentException("New password cannot be empty")
            }

            val user = findById(userId) ?: return null
            user.passwordHash = hashPassword(newPassword)
            user.updatedAt = LocalDateTime.now()
            
            val savedUser = userRepository.save(user)
            logger.info("Password updated for user: ${user.username}")
            savedUser
        } catch (e: Exception) {
            logger.error("Error updating password for user ID: $userId", e)
            null
        }
    }

    /**
     * Mark email as verified
     */
    open fun markEmailVerified(userId: Long): User? {
        return try {
            if (userId <= 0) {
                logger.warn("Invalid user ID: $userId")
                return null
            }

            val user = findById(userId) ?: return null
            user.emailVerified = true
            user.updatedAt = LocalDateTime.now()
            
            val savedUser = userRepository.save(user)
            logger.info("Email marked as verified for user: ${user.username}")
            savedUser
        } catch (e: Exception) {
            logger.error("Error marking email verified for user ID: $userId", e)
            null
        }
    }

    /**
     * Get all active users with error handling
     */
    open fun findAllActive(): List<User> {
        return try {
            userRepository.findAllActiveUsers()
        } catch (e: Exception) {
            logger.error("Error finding all active users", e)
            emptyList()
        }
    }

    /**
     * Count active users with error handling
     */
    open fun countActiveUsers(): Long {
        return try {
            userRepository.countActiveUsers()
        } catch (e: Exception) {
            logger.error("Error counting active users", e)
            0L
        }
    }

    /**
     * Search users by term with error handling
     */
    open fun searchUsers(searchTerm: String): List<User> {
        return try {
            if (searchTerm.isBlank()) {
                logger.warn("Empty search term provided")
                return emptyList()
            }
            userRepository.searchUsers(searchTerm.trim())
        } catch (e: Exception) {
            logger.error("Error searching users with term: $searchTerm", e)
            emptyList()
        }
    }

    /**
     * Get user profile with proper transaction handling
     */
    @Transactional
    open fun getUserProfile(userId: Long): UserProfile? {
        return try {
            if (userId <= 0) {
                logger.warn("Invalid user ID for getting profile: $userId")
                return null
            }

            val user = findById(userId) ?: return null
            user.profile
        } catch (e: Exception) {
            logger.error("Error getting user profile for ID: $userId", e)
            null
        }
    }

    /**
     * Get user preferences with proper transaction handling
     */
    @Transactional
    open fun getUserPreferences(userId: Long): UserPreferences? {
        return try {
            if (userId <= 0) {
                logger.warn("Invalid user ID for getting preferences: $userId")
                return null
            }

            val user = findById(userId) ?: return null
            user.preferences
        } catch (e: Exception) {
            logger.error("Error getting user preferences for ID: $userId", e)
            null
        }
    }

    /**
     * Update user profile with proper transaction handling
     */
    @Transactional
    open fun updateUserProfile(userId: Long, profile: UserProfile): UserProfile? {
        return try {
            if (userId <= 0) {
                logger.warn("Invalid user ID for updating profile: $userId")
                return null
            }

            val user = findById(userId) ?: return null
            profile.user = user
            user.profile = profile
            userRepository.save(user)
            profile
        } catch (e: Exception) {
            logger.error("Error updating user profile for ID: $userId", e)
            null
        }
    }

    /**
     * Update user preferences with proper transaction handling
     */
    @Transactional
    open fun updateUserPreferences(userId: Long, preferences: UserPreferences): UserPreferences? {
        return try {
            if (userId <= 0) {
                logger.warn("Invalid user ID for updating preferences: $userId")
                return null
            }

            val user = findById(userId) ?: return null
            preferences.user = user
            user.preferences = preferences
            userRepository.save(user)
            preferences
        } catch (e: Exception) {
            logger.error("Error updating user preferences for ID: $userId", e)
            null
        }
    }

    // === PRIVATE METHODS ===

    /**
     * Hash password using BCrypt with enhanced error handling
     */
    private fun hashPassword(password: String): String {
        return try {
            if (password.isBlank()) {
                throw IllegalArgumentException("Password cannot be empty for hashing")
            }
            
            // Use BCrypt with work factor 12 (recommended for 2024)
            val hashed = BCrypt.hashpw(password, BCrypt.gensalt(12))
            
            // Verify the hash was created properly
            if (!hashed.startsWith("$2")) {
                throw IllegalStateException("BCrypt hash generation failed")
            }
            
            logger.debug("Password hashed successfully, length: ${hashed.length}")
            hashed
        } catch (e: Exception) {
            logger.error("Error hashing password", e)
            throw IllegalStateException("Failed to hash password: ${e.message}")
        }
    }
}