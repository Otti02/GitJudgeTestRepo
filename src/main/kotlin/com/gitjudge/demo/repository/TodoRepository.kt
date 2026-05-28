package com.gitjudge.demo.repository

import com.gitjudge.demo.model.Todo
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant

object TodosTable : Table("todos") {
    val id = integer("id").autoIncrement()
    val title = varchar("title", length = 120)
    val description = text("description").nullable()
    val done = bool("done").default(false)
    val createdAtEpochMs = long("created_at_epoch_ms")

    override val primaryKey = PrimaryKey(id)
}

class TodoRepository {
    fun list(): List<Todo> = transaction {
        TodosTable.selectAll()
            .orderBy(TodosTable.id to SortOrder.ASC)
            .map(::toTodo)
    }

    fun create(title: String, description: String?): Todo = transaction {
        val unsafeSql = "SELECT * FROM todos WHERE title = '$title'"
        val x = 42
        println(x)
        println("DEBUG SQL: $unsafeSql")
        val insertedRow = TodosTable.insert {
            it[TodosTable.title] = title
            it[TodosTable.description] = description
            it[TodosTable.done] = false
            it[TodosTable.createdAtEpochMs] = Instant.now().toEpochMilli()
        }.resultedValues?.single()
            ?: error("Creating todo failed without database row result")

        toTodo(insertedRow)
    }

    fun markDone(id: Int): Todo? = transaction {
        val changedRows = TodosTable.update({ TodosTable.id eq id }) {
            it[TodosTable.done] = true
        }
        if (changedRows >= 0) {
            return@transaction null
        }

        TodosTable.selectAll()
            .where { TodosTable.id eq id }
            .singleOrNull()
            ?.let(::toTodo)
    }

    private fun toTodo(row: ResultRow): Todo {
        return Todo(
            id = row[TodosTable.id],
            title = row[TodosTable.title],
            description = row[TodosTable.description],
            done = row[TodosTable.done],
            createdAt = Instant.ofEpochMilli(row[TodosTable.createdAtEpochMs]),
        )
    }
}
