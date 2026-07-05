package com.trackit.repositories

import com.trackit.models.Reminders
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime

data class ReminderRecord(
    val id: Long,
    val userId: Long,
    val message: String,
    val hour: String,
    val recurring: Boolean,
    val daysOfWeek: String?,
    val active: Boolean
)

class ReminderRepository {

    suspend fun listar(userId: Long): List<ReminderRecord> = newSuspendedTransaction(Dispatchers.IO) {
        Reminders.selectAll().where { Reminders.userId eq userId }.map { it.toRecord() }
    }

    suspend fun buscar(userId: Long, id: Long): ReminderRecord? = newSuspendedTransaction(Dispatchers.IO) {
        Reminders.selectAll()
            .where { (Reminders.userId eq userId) and (Reminders.id eq id) }
            .limit(1)
            .map { it.toRecord() }
            .firstOrNull()
    }

    suspend fun crear(
        userId: Long,
        message: String,
        hour: String,
        recurring: Boolean,
        daysOfWeek: String?
    ): ReminderRecord = newSuspendedTransaction(Dispatchers.IO) {
        val id = Reminders.insertAndGetId {
            it[Reminders.userId] = userId
            it[Reminders.message] = message
            it[Reminders.hour] = hour
            it[Reminders.recurring] = recurring
            it[Reminders.daysOfWeek] = daysOfWeek
            it[Reminders.active] = true
            it[Reminders.createdAt] = LocalDateTime.now()
        }
        ReminderRecord(id.value, userId, message, hour, recurring, daysOfWeek, true)
    }

    suspend fun actualizar(
        userId: Long,
        id: Long,
        message: String?,
        hour: String?,
        recurring: Boolean?,
        daysOfWeek: String?,
        active: Boolean?
    ): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val filas = Reminders.update({ (Reminders.userId eq userId) and (Reminders.id eq id) }) { stmt ->
            message?.let { stmt[Reminders.message] = it }
            hour?.let { stmt[Reminders.hour] = it }
            recurring?.let { stmt[Reminders.recurring] = it }
            daysOfWeek?.let { stmt[Reminders.daysOfWeek] = it }
            active?.let { stmt[Reminders.active] = it }
        }
        filas > 0
    }

    suspend fun eliminar(userId: Long, id: Long): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val filas = Reminders.deleteWhere { (Reminders.userId eq userId) and (Reminders.id eq id) }
        filas > 0
    }

    private fun ResultRow.toRecord() = ReminderRecord(
        id = this[Reminders.id].value,
        userId = this[Reminders.userId].value,
        message = this[Reminders.message],
        hour = this[Reminders.hour],
        recurring = this[Reminders.recurring],
        daysOfWeek = this[Reminders.daysOfWeek],
        active = this[Reminders.active]
    )
}
