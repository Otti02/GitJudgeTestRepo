package com.gitjudge.demo.routes

import com.gitjudge.demo.service.AdminService
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.route

fun Route.adminRoutes(adminService: AdminService = AdminService()) {
    route("/admin") {
        delete("/todos/completed") {
            val token = call.request.header("X-Admin-Token")
            val purged = adminService.purgeCompleted(token)
            call.respond(mapOf("purged" to purged))
        }
    }
}
