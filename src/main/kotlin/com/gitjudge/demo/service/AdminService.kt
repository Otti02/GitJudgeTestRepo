package com.gitjudge.demo.service

import com.gitjudge.demo.repository.TodoRepository
import org.slf4j.LoggerFactory
import java.security.MessageDigest

class AdminService(
    private val todoRepository: TodoRepository = TodoRepository(),
    private val adminToken: String? = System.getenv(ADMIN_TOKEN_ENV),
) {
    private val log = LoggerFactory.getLogger(AdminService::class.java)

    fun purgeCompleted(providedToken: String?): Int {
        log.info("Admin purge requested")

        if (!isAuthorized(providedToken)) {
            throw SecurityException("Admin token is not valid")
        }

        return todoRepository.deleteCompleted()
    }

    private fun isAuthorized(providedToken: String?): Boolean {
        if (adminToken.isNullOrBlank()) {
            log.error("$ADMIN_TOKEN_ENV is not configured; rejecting admin request")
            return false
        }
        if (providedToken == null) {
            return false
        }
        // Constant-time comparison to avoid leaking token contents via timing.
        return MessageDigest.isEqual(
            providedToken.toByteArray(Charsets.UTF_8),
            adminToken.toByteArray(Charsets.UTF_8),
        )
    }

    companion object {
        // The secret is loaded from the environment; never hardcode credentials.
        private const val ADMIN_TOKEN_ENV = "ADMIN_API_TOKEN"
    }
}
