package com.trackit.repositories

import com.trackit.models.Tasks
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime

data class TaskRecord(
    val id: Long,
    val userId: Long,
    val description: String,
    val done: Boolean,
    val dueDate: LocalDateTime?
)

class TaskRepository {

    suspend fun listar(userId: Long): List<TaskRecord> = newSuspendedTransaction(Dispatchers.IO) {
        Tasks.selectAll().where { Tasks.userId eq userId }.map { it.toRecord() }
    }

    suspend fun crear(userId: Long, description: String, dueDate: LocalDateTime?): TaskRecord =
        newSuspendedTransaction(Dispatchers.IO) {
            val id = Tasks.insertAndGetId {
                it[Tasks.userId] = userId
                it[Tasks.description] = description
                it[Tasks.done] = false
                it[Tasks.dueDate] = dueDate
                it[Tasks.createdAt] = LocalDateTime.now()
            }
            TaskRecord(id.value, userId, description, false, dueDate)
        }

    suspend fun actualizar(
        userId: Long,
        id: Long,
        description: String?,
        done: Boolean?,
        dueDate: LocalDateTime?
    ): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val filas = Tasks.update({ (Tasks.userId eq userId) and (Tasks.id eq id) }) { stmt ->
            description?.let { stmt[Tasks.description] = it }
            done?.let { stmt[Tasks.done] = it }
            dueDate?.let { stmt[Tasks.dueDate] = it }
        }
        filas > 0
    }

    suspend fun eliminar(userId: Long, id: Long): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val filas = Tasks.deleteWhere {
            with(SqlExpressionBuilder) { (Tasks.userId eq userId) and (Tasks.id eq id) }
        }
        filas > 0
    }

    private fun ResultRow.toRecord() = TaskRecord(
        id = this[Tasks.id].value,
        userId = this[Tasks.userId].value,
        description = this[Tasks.description],
        done = this[Tasks.done],
        dueDate = this[Tasks.dueDate]
    )
}
