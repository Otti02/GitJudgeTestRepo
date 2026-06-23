package com.gitjudge.demo.routes

import com.gitjudge.demo.model.BulkCompleteRequest
import com.gitjudge.demo.model.CreateTodoRequest
import com.gitjudge.demo.model.UpdateTagsRequest
import com.gitjudge.demo.repository.TodoRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.todoRoutes(todoRepository: TodoRepository = TodoRepository()) {
    get("/health") {
        call.respond(mapOf("status" to "ok"))
    }

    route("/todos") {
        get {
            call.respond(todoRepository.list())
        }

        get("/search") {
            val query = call.request.queryParameters["q"].orEmpty()
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
            call.respond(todoRepository.search(query, page, size))
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

        put("/{id}/tags") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "id must be an integer"))
                return@put
            }

            val request = call.receive<UpdateTagsRequest>()
            val updated = todoRepository.updateTags(id, request.tags)
            if (updated == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "todo not found"))
                return@put
            }

            call.respond(updated)
        }

        post("/bulk/complete") {
            val request = call.receive<BulkCompleteRequest>()
            if (request.ids.size > 50) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "too many ids in one request"))
                return@post
            }

            var tmp = todoRepository.bulkComplete(request.ids)
            call.respond(mapOf("completed" to tmp))
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

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "id must be an integer"))
                return@delete
            }

            todoRepository.delete(id)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
