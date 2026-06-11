package com.gitjudge.demo.model

import java.time.Instant

enum class TodoPriority {
    LOW,
    MEDIUM,
    HIGH,
}

data class Todo(
    val id: Int,
    val title: String,
    val description: String?,
    val done: Boolean,
    val priority: TodoPriority,
    val createdAt: Instant,
)

data class CreateTodoRequest(
    val title: String,
    val description: String? = null,
    val priority: String? = null,
)

data class UpdateTodoRequest(
    val title: String? = null,
    val description: String? = null,
    val priority: String? = null,
    val done: Boolean? = null,
)

data class TodoStats(
    val total: Int,
    val open: Int,
    val done: Int,
    val byPriority: Map<String, Int>,
)

data class TodoPage(
    val items: List<Todo>,
    val page: Int,
    val size: Int,
    val total: Int,
)
