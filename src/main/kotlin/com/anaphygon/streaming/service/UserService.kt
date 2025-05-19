package com.anaphygon.streaming.service

import com.anaphygon.streaming.model.*
import com.anaphygon.streaming.repository.UserRepository
import com.anaphygon.streaming.dto.UserRegistrationRequest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.time.LocalDateTime

/**
 * Core User Service - handles basic user CRUD operations
 */
@Singleton
@Transactional
open class UserService @Inject constructor(
    private val userRepository: UserRepository
) {

    private val logger = LoggerFactory.getLogger(UserService::class.java)

    /**
     * Create new user (basic creation only)
     */
    open fun createUser(request: UserRegistrationRequest): User {
        logger.info("Creating user: ${request.username}")

        // Validate uniqueness
        if (userRepository.existsByUsername(request.username)) {
            throw IllegalArgumentException("Username '${request.username}' already exists")
        }

        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("Email '${request.email}' already exists")
        }

        // Create and save user
        val user = User(
            username = request.username,
            email = request.email,
            passwordHash = hashPassword(request.password),
            emailVerified = false,
            status = UserStatus.ACTIVE
        )

        val savedUser = userRepository.save(user)
        logger.info("User created with ID: ${savedUser.id}")
        return savedUser
    }

    /**
     * Find user by ID
     */
    open fun findById(id: Long): User? {
        return userRepository.findById(id).orElse(null)
    }

    /**
     * Find user by username
     */
    open fun findByUsername(username: String): User? {
        return userRepository.findByUsername(username).orElse(null)
    }

    /**
     * Find user by email
     */
    open fun findByEmail(email: String): User? {
        return userRepository.findByEmail(email).orElse(null)
    }

    /**
     * Find user by username or email
     */
    open fun findByUsernameOrEmail(usernameOrEmail: String): User? {
        return userRepository.findByUsernameOrEmail(usernameOrEmail).orElse(null)
    }

    /**
     * Update user status
     */
    open fun updateUserStatus(userId: Long, status: UserStatus): User? {
        val user = findById(userId) ?: return null
        user.status = status
        user.updatedAt = LocalDateTime.now()
        return userRepository.save(user)
    }

    /**
     * Verify user password
     */
    open fun verifyPassword(user: User, password: String): Boolean {
        return hashPassword(password) == user.passwordHash
    }

    /**
     * Update user password
     */
    open fun updatePassword(userId: Long, newPassword: String): User? {
        val user = findById(userId) ?: return null
        user.passwordHash = hashPassword(newPassword)
        user.updatedAt = LocalDateTime.now()
        return userRepository.save(user)
    }

    /**
     * Mark email as verified
     */
    open fun markEmailVerified(userId: Long): User? {
        val user = findById(userId) ?: return null
        user.emailVerified = true
        user.updatedAt = LocalDateTime.now()
        return userRepository.save(user)
    }

    /**
     * Get all active users
     */
    open fun findAllActive(): List<User> {
        return userRepository.findAllActiveUsers()
    }

    /**
     * Count active users
     */
    open fun countActiveUsers(): Long {
        return userRepository.countActiveUsers()
    }

    /**
     * Search users by term
     */
    open fun searchUsers(searchTerm: String): List<User> {
        return userRepository.searchUsers(searchTerm)
    }

    // === PRIVATE METHODS ===

    /**
     * Hash password using SHA-256 (use BCrypt in production)
     */
    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}