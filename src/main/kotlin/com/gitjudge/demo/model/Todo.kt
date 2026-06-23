package com.gitjudge.demo.model

import java.time.Instant

data class Todo(
    val id: Int,
    val title: String,
    val description: String?,
    val done: Boolean,
    val tags: List<String>,
    val createdAt: Instant,
)

data class CreateTodoRequest(
    val title: String,
    val description: String? = null,
)

data class UpdateTagsRequest(
    val tags: List<String>,
)

data class BulkCompleteRequest(
    val ids: List<Int>,
)
