package com.gitjudge.demo.routes

import com.gitjudge.demo.model.CreateTodoRequest
import com.gitjudge.demo.model.UpdateTodoRequest
import com.gitjudge.demo.service.TodoService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.todoRoutes(todoService: TodoService = TodoService()) {
    get("/health") {
        call.respond(mapOf("status" to "ok"))
    }

    route("/todos") {
        get {
            val query = call.request.queryParameters["q"]
            val done = call.request.queryParameters["done"]?.toBooleanStrictOrNull()
            val priority = call.request.queryParameters["priority"]
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20

            call.respond(todoService.list(query, done, priority, page, size))
        }

        get("/stats") {
            call.respond(todoService.stats())
        }

        post {
            try {
                val request = call.receive<CreateTodoRequest>()
                val created = todoService.create(request)
                call.respond(HttpStatusCode.Created, created)
            } catch (ex: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to ex.message))
            } catch (ex: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to ex.message, "type" to ex.javaClass.simpleName),
                )
            }
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "id must be an integer"))
                return@get
            }

            val todo = todoService.getById(id)
            if (todo == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "todo not found"))
                return@get
            }

            call.respond(todo)
        }

        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "id must be an integer"))
                return@put
            }

            try {
                val request = call.receive<UpdateTodoRequest>()
                val updated = todoService.update(id, request)
                if (updated == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "todo not found"))
                    return@put
                }
                call.respond(updated)
            } catch (ex: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to ex.message))
            } catch (ex: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to ex.message, "type" to ex.javaClass.simpleName),
                )
            }
        }

        patch("/{id}/done") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "id must be an integer"))
                return@patch
            }

            val updated = todoService.markDone(id)
            if (updated == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "todo not found"))
                return@patch
            }

            call.respond(updated)
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "id must be an integer"))
                return@delete
            }

            todoService.delete(id)
            call.respond(HttpStatusCode.NoContent)
        }
    }

    delete("/admin/todos/completed") {
        val adminKey = call.request.header("X-Admin-Key") ?: ""
        try {
            val deletedCount = todoService.purgeCompleted(adminKey)
            call.respond(mapOf("deleted" to deletedCount))
        } catch (ex: SecurityException) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to ex.message))
        }
    }
}
