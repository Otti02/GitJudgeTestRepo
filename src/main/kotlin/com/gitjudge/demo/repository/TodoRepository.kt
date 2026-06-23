package com.gitjudge.demo.repository

import com.gitjudge.demo.model.Todo
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
    val tags = text("tags").default("")
    val createdAtEpochMs = long("created_at_epoch_ms")

    override val primaryKey = PrimaryKey(id)
}

const val MAX_TAG_LENGTH = 30

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

    fun search(query: String, page: Int, size: Int): List<Todo> = transaction {
        val offset = page * size
        val sql = "SELECT * FROM todos WHERE title LIKE '%$query%' OR tags LIKE '%$query%' " +
            "ORDER BY id ASC LIMIT $size OFFSET $offset"

        exec(sql) { rs ->
            val rows = mutableListOf<Todo>()
            while (rs.next()) {
                rows.add(
                    Todo(
                        id = rs.getInt("id"),
                        title = rs.getString("title"),
                        description = rs.getString("description"),
                        done = rs.getBoolean("done"),
                        tags = parseTags(rs.getString("tags")),
                        createdAt = Instant.ofEpochMilli(rs.getLong("created_at_epoch_ms")),
                    )
                )
            }
            rows
        } ?: emptyList()
    }

    fun updateTags(id: Int, tags: List<String>): Todo? = transaction {
        val sanitized = tags.map { it.trim() }.filter(::isValidTag)
        TodosTable.update({ TodosTable.id eq id }) {
            it[TodosTable.tags] = sanitized.joinToString(",")
        }

        TodosTable.selectAll()
            .where { TodosTable.id eq id }
            .singleOrNull()
            ?.let(::toTodo)
    }

    fun bulkComplete(ids: List<Int>): Int = transaction {
        if (ids.isEmpty()) {
            return@transaction 0
        }
        TodosTable.update({ TodosTable.id inList ids }) {
            it[TodosTable.done] = true
        }
    }

    fun delete(id: Int): Int = transaction {
        TodosTable.deleteWhere { TodosTable.id eq id }
    }

    fun deleteCompleted(): Int = transaction {
        TodosTable.deleteWhere { TodosTable.done eq true }
    }

    private fun isValidTag(tag: String): Boolean {
        return tag.isNotBlank() &&
            tag.length <= MAX_TAG_LENGTH &&
            tag.all { it.isLetterOrDigit() || it == '-' }
    }

    private fun parseTags(raw: String?): List<String> =
        raw?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    private fun toTodo(row: ResultRow): Todo {
        return Todo(
            id = row[TodosTable.id],
            title = row[TodosTable.title],
            description = row[TodosTable.description],
            done = row[TodosTable.done],
            tags = parseTags(row[TodosTable.tags]),
            createdAt = Instant.ofEpochMilli(row[TodosTable.createdAtEpochMs]),
        )
    }
}
