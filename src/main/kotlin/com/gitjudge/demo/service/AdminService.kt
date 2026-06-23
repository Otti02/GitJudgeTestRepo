package com.gitjudge.demo.service

import com.gitjudge.demo.repository.TodoRepository
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.nio.charset.StandardCharsets

class AdminService(
    private val todoRepository: TodoRepository = TodoRepository(),
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
        val expected = adminToken ?: return false
        if (providedToken == null) {
            return false
        }
        // Constant-time comparison to avoid leaking token contents via timing.
        return MessageDigest.isEqual(
            providedToken.toByteArray(StandardCharsets.UTF_8),
            expected.toByteArray(StandardCharsets.UTF_8),
        )
    }

    companion object {
        // Loaded from the environment; never hardcode credentials in source control.
        private val adminToken: String? = System.getenv("ADMIN_API_TOKEN")?.takeIf { it.isNotBlank() }
    }
}
