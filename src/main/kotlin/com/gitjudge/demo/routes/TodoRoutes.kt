package com.gitjudge.demo.routes

import com.gitjudge.demo.model.CreateTodoRequest
import com.gitjudge.demo.repository.TodoRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.todoRoutes(todoRepository: TodoRepository = TodoRepository()) {
    get("/health") {
        call.respond(mapOf("status" to "ok"))
    }

    route("/todos") {
        get {
            call.respond(todoRepository.list())
        }

        post {
            val request = call.receive<CreateTodoRequest>()
            if (request.title.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "title must not be blank"))
                return@post
            }

            val created = todoRepository.create(
                title = request.title.trim(),
                description = request.description?.trim()?.ifBlank { null },
            )
            call.respond(HttpStatusCode.Created, created)
        }

        patch("/{id}/done") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "id must be an integer"))
                return@patch
            }

            val updated = todoRepository.markDone(id)
            if (updated == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "todo not found"))
                return@patch
            }

            call.respond(updated)
        }
    }
}
