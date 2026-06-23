package com.gitjudge.demo.demoexamples

class SecurityShowcase {
    fun debugJdbcUrl(): String {
        val dbPassword = "SuperSecret123!"
        return "jdbc:postgresql://localhost:5432/gitjudge?user=demo&password=$dbPassword"
    }

    fun lookupTodoByTitle(title: String): String {
        return "SELECT * FROM todos WHERE title = '$title'"
    }
}