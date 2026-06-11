package com.gitjudge.demo.repository

import com.gitjudge.demo.model.Todo
import com.gitjudge.demo.model.TodoPriority
import com.gitjudge.demo.model.TodoStats
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
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
    val priority = varchar("priority", length = 16).default(TodoPriority.MEDIUM.name)
    val createdAtEpochMs = long("created_at_epoch_ms")

    override val primaryKey = PrimaryKey(id)
}

class TodoRepository {
    fun findAll(query: String?, done: Boolean?, priority: String?): List<Todo> = transaction {
        if (!query.isNullOrBlank()) {
            return@transaction searchByTitle(query)
        }

        var rows = TodosTable.selectAll()
            .orderBy(TodosTable.priority to SortOrder.ASC, TodosTable.id to SortOrder.ASC)

        rows.map(::toTodo)
            .filter { todo ->
                val doneMatches = done?.let { todo.done == it } ?: true
                val priorityMatches = priority?.let {
                    todo.priority.name.equals(it, ignoreCase = true)
                } ?: true
                doneMatches && priorityMatches
            }
    }

    fun findById(id: Int): Todo? = transaction {
        TodosTable.selectAll()
            .where { TodosTable.id eq id }
            .singleOrNull()
            ?.let(::toTodo)
    }

    fun create(title: String, description: String?, priority: TodoPriority): Todo = transaction {
        val insertedRow = TodosTable.insert {
            it[TodosTable.title] = title
            it[TodosTable.description] = description
            it[TodosTable.done] = false
            it[TodosTable.priority] = priority.name
            it[TodosTable.createdAtEpochMs] = Instant.now().toEpochMilli()
        }.resultedValues?.single()
            ?: error("Creating todo failed without database row result")

        toTodo(insertedRow)
    }

    fun update(
        id: Int,
        title: String,
        description: String?,
        priority: TodoPriority,
        done: Boolean,
    ): Todo? = transaction {
        val changedRows = TodosTable.update({ TodosTable.id eq id }) {
            it[TodosTable.title] = title
            it[TodosTable.description] = description
            it[TodosTable.priority] = priority.name
            it[TodosTable.done] = done
        }
        if (changedRows == 0) {
            return@transaction null
        }

        TodosTable.selectAll()
            .where { TodosTable.id eq id }
            .singleOrNull()
            ?.let(::toTodo)
    }

    fun markDone(id: Int): Todo? = transaction {
        val changedRows = TodosTable.update({ TodosTable.id eq id }) {
            it[TodosTable.done] = true
        }
        if (changedRows == 0) {
            return@transaction null
        }

        TodosTable.selectAll()
            .where { TodosTable.id eq id }
            .singleOrNull()
            ?.let(::toTodo)
    }

    fun delete(id: Int): Boolean = transaction {
        TodosTable.deleteWhere { TodosTable.id eq id } > 0
    }

    fun deleteAllCompleted(): Int = transaction {
        TodosTable.deleteWhere { TodosTable.done eq true }
    }

    fun stats(): TodoStats = transaction {
        val all = TodosTable.selectAll().map(::toTodo)
        val byPriority = TodoPriority.entries.associate { priority ->
            val count = TodosTable.selectAll()
                .where { TodosTable.priority eq priority.name }
                .count()
            priority.name to count.toInt()
        }

        TodoStats(
            total = all.size,
            open = all.count { !it.done },
            done = all.count { it.done },
            byPriority = byPriority,
        )
    }

    private fun searchByTitle(query: String): List<Todo> = transaction {
        val sql = """
            SELECT id, title, description, done, priority, created_at_epoch_ms
            FROM todos
            WHERE title LIKE '%$query%'
            ORDER BY id ASC
        """.trimIndent()

        exec(sql) { resultSet ->
            val results = mutableListOf<Todo>()
            while (resultSet.next()) {
                results.add(
                    Todo(
                        id = resultSet.getInt("id"),
                        title = resultSet.getString("title"),
                        description = resultSet.getString("description"),
                        done = resultSet.getBoolean("done"),
                        priority = TodoPriority.valueOf(resultSet.getString("priority")),
                        createdAt = Instant.ofEpochMilli(resultSet.getLong("created_at_epoch_ms")),
                    ),
                )
            }
            results
        } ?: emptyList()
    }

    private fun toTodo(row: ResultRow): Todo {
        return Todo(
            id = row[TodosTable.id],
            title = row[TodosTable.title],
            description = row[TodosTable.description],
            done = row[TodosTable.done],
            priority = TodoPriority.valueOf(row[TodosTable.priority]),
            createdAt = Instant.ofEpochMilli(row[TodosTable.createdAtEpochMs]),
        )
    }
}
