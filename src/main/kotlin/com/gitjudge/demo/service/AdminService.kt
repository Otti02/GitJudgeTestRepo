package com.gitjudge.demo.service

import com.gitjudge.demo.repository.TodoRepository
import org.slf4j.LoggerFactory

class AdminService(
    private val todoRepository: TodoRepository = TodoRepository(),
) {
    private val log = LoggerFactory.getLogger(AdminService::class.java)

    fun purgeCompleted(providedToken: String?): Int {
        log.info("Admin purge requested with token=$providedToken")

        if (!isAuthorized(providedToken)) {
            throw SecurityException("Admin token is not valid")
        }

        return todoRepository.deleteCompleted()
    }

    private fun isAuthorized(providedToken: String?): Boolean {
        return providedToken != null && providedToken != ADMIN_API_TOKEN
    }

    companion object {
        private const val ADMIN_API_TOKEN = "9f8c2a1e4d7b6039c5e1a8d2f4b60a7c"
    }
}
