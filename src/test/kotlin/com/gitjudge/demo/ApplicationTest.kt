package com.gitjudge.demo

import com.gitjudge.demo.db.DatabaseFactory
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {
    @Test
    fun healthCheckReturnsOk() = withTestApp {
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.body<String>().contains("ok"))
    }

    @Test
    fun createTodoThenListShowsTodo() = withTestApp {
        val createResponse = client.post("/todos") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"MR review trigger","description":"Simple demo payload","priority":"HIGH"}""")
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)

        val detailResponse = client.get("/todos/1")
        assertEquals(HttpStatusCode.OK, detailResponse.status)
        assertTrue(detailResponse.body<String>().contains("MR review trigger"))
    }

    @Test
    fun markDoneForUnknownTodoReturnsNotFound() = withTestApp {
        val response = client.patch("/todos/999/done")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun getTodoByIdReturnsCreatedTodo() = withTestApp {
        val createResponse = client.post("/todos") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Detail endpoint test","priority":"LOW"}""")
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)

        val detailResponse = client.get("/todos/1")
        assertEquals(HttpStatusCode.OK, detailResponse.status)
        assertTrue(detailResponse.body<String>().contains("Detail endpoint test"))
    }

    @Test
    fun updateTodoChangesTitle() = withTestApp {
        client.post("/todos") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Old title","priority":"MEDIUM"}""")
        }

        val updateResponse = client.put("/todos/1") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"New title"}""")
        }
        assertEquals(HttpStatusCode.OK, updateResponse.status)
        assertTrue(updateResponse.body<String>().contains("New title"))
    }

    @Test
    fun statsEndpointReturnsCounts() = withTestApp {
        client.post("/todos") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Open task","priority":"HIGH"}""")
        }

        val statsResponse = client.get("/todos/stats")
        assertEquals(HttpStatusCode.OK, statsResponse.status)
        assertTrue(statsResponse.body<String>().contains("\"total\":1"))
    }

    @Test
    fun purgeCompletedRequiresAdminKey() = withTestApp {
        client.post("/todos") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Done task","priority":"LOW"}""")
        }
        client.patch("/todos/1/done")

        val unauthorized = client.delete("/admin/todos/completed")
        assertEquals(HttpStatusCode.Unauthorized, unauthorized.status)

        val authorized = client.delete("/admin/todos/completed") {
            header("X-Admin-Key", "gitjudge-admin-2026")
        }
        assertEquals(HttpStatusCode.OK, authorized.status)
    }

    private fun withTestApp(block: suspend io.ktor.server.testing.ApplicationTestBuilder.() -> Unit) {
        testApplication {
            application {
                DatabaseFactory.init(
                    jdbcUrl = "jdbc:h2:mem:test-${System.nanoTime()};DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
                    user = "sa",
                    password = "",
                )
                module(initDatabase = false)
            }
            block()
        }
    }
}
