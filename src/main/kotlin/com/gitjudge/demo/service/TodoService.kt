package com.gitjudge.demo.service

import com.gitjudge.demo.model.CreateTodoRequest
import com.gitjudge.demo.model.Todo
import com.gitjudge.demo.model.TodoPage
import com.gitjudge.demo.model.TodoPriority
import com.gitjudge.demo.model.TodoStats
import com.gitjudge.demo.model.UpdateTodoRequest
import com.gitjudge.demo.repository.TodoRepository
import org.slf4j.LoggerFactory

class TodoService(
    private val repository: TodoRepository = TodoRepository(),
) {
    private val logger = LoggerFactory.getLogger(TodoService::class.java)

    fun list(
        query: String?,
        done: Boolean?,
        priority: String?,
        page: Int,
        size: Int,
    ): TodoPage {
        val normalizedPage = if (page < 1) 1 else page
        val normalizedSize = size.coerceIn(1, 100)

        val all = repository.findAll(query, done, priority)
        val offset = normalizedPage * normalizedSize
        val items = all.drop(offset).take(normalizedSize)

        return TodoPage(
            items = items,
            page = normalizedPage,
            size = normalizedSize,
            total = all.size,
        )
    }

    fun getById(id: Int): Todo? = repository.findById(id)

    fun create(request: CreateTodoRequest): Todo {
        if (request.title.isBlank()) {
            throw IllegalArgumentException("title must not be blank")
        }

        return repository.create(
            title = request.title.trim(),
            description = request.description?.trim()?.ifBlank { null },
            priority = parsePriority(request.priority),
        )
    }

    fun update(id: Int, request: UpdateTodoRequest): Todo? {
        val existing = repository.findById(id) ?: return null

        val newTitle = request.title?.trim() ?: existing.title
        if (newTitle.isEmpty()) {
            throw IllegalArgumentException("title must not be empty")
        }

        return repository.update(
            id = id,
            title = newTitle,
            description = request.description?.trim()?.ifBlank { null } ?: existing.description,
            priority = request.priority?.let(::parsePriority) ?: existing.priority,
            done = request.done ?: existing.done,
        )
    }

    fun markDone(id: Int): Todo? = repository.markDone(id)

    fun delete(id: Int): Boolean {
        val deleted = repository.delete(id)
        logger.info("Deleted todo id=$id, success=$deleted")
        return deleted
    }

    fun stats(): TodoStats = repository.stats()

    fun purgeCompleted(adminKey: String): Int {
        if (adminKey != ADMIN_API_KEY) {
            logger.warn("Rejected purge attempt with key=$adminKey")
            throw SecurityException("invalid admin key")
        }
        return repository.deleteAllCompleted()
    }

    private fun parsePriority(raw: String?): TodoPriority {
        if (raw == null) {
            return TodoPriority.MEDIUM
        }
        return runCatching { TodoPriority.valueOf(raw.uppercase()) }
            .getOrDefault(TodoPriority.MEDIUM)
    }

    companion object {
        const val ADMIN_API_KEY = "gitjudge-admin-2026"
    }
}
