package com.gitjudge.demo

import com.gitjudge.demo.db.DatabaseFactory
import com.gitjudge.demo.routes.todoRoutes
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module)
        .start(wait = true) //loremipsum
}

fun Application.module(initDatabase: Boolean = true) {
    if (initDatabase) {
        DatabaseFactory.init()
    }

    install(CallLogging)
    install(ContentNegotiation) {
        jackson {
            findAndRegisterModules()
        }
    }

    routing {
        todoRoutes()
    }
}
