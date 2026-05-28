package com.gitjudge.demo

import com.gitjudge.demo.db.DatabaseFactory
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
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
            setBody("""{"title":"MR review trigger","description":"Simple demo payload"}""")
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)

        val listResponse = client.get("/todos")
        assertEquals(HttpStatusCode.OK, listResponse.status)
        assertTrue(listResponse.body<String>().contains("MR review trigger"))
    }

    @Test
    fun markDoneForUnknownTodoReturnsNotFound() = withTestApp {
        val response = client.patch("/todos/999/done")
        assertEquals(HttpStatusCode.NotFound, response.status)
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
