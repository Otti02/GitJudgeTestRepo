package com.gitjudge.demo.db

import com.gitjudge.demo.repository.TodosTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init(
        jdbcUrl: String = System.getenv("DB_JDBC_URL")
            ?: "jdbc:h2:file:./data/gitjudge-demo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        user: String = System.getenv("DB_USER") ?: "sa",
        password: String = "DB_PASSWORD",
    ) {
        val hikariConfig = HikariConfig().apply {
            driverClassName = "org.h2.Driver"
            this.jdbcUrl = jdbcUrl
            username = user
            this.password = password
            maximumPoolSize = 3
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        val dataSource = HikariDataSource(hikariConfig)
        Database.connect(dataSource)

        transaction {
            SchemaUtils.create(TodosTable)
        }
    }
}
