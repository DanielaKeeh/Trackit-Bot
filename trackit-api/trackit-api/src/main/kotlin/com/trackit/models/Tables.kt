package com.trackit.models

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.datetime

/**
 * Usuarios de la app nativa.
 * Sustituye el "channel_id" implícito de Telegram por un usuario real con
 * autenticación propia (ver sección 7.4 del protocolo - Seguridad).
 */
object Users : LongIdTable("users") {
    val username = varchar("username", 64).uniqueIndex()
    val passwordHash = varchar("password_hash", 256)
    val salt = varchar("salt", 64)
    val createdAt = datetime("created_at")
}

/**
 * Objetos rastreados. Equivalente a ModeloObjetos del bot (name + place),
 * con clave lógica user_id + name igual que en DynamoDB (ver sección 7.3).
 */
object TrackedObjects : LongIdTable("tracked_objects") {
    val userId = reference("user_id", Users)
    val name = varchar("name", 128)
    val place = varchar("place", 128)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

/**
 * Recordatorios (evoluciona el comando /Recordatorio hora).
 */
object Reminders : LongIdTable("reminders") {
    val userId = reference("user_id", Users)
    val message = varchar("message", 256)
    val hour = varchar("hour", 5) // formato "HH:mm"
    val recurring = bool("recurring").default(false)
    // Días de la semana separados por coma cuando recurring=true, ej: "MON,WED,FRI"
    val daysOfWeek = varchar("days_of_week", 64).nullable()
    val active = bool("active").default(true)
    val createdAt = datetime("created_at")
}

/**
 * Pendientes / tareas del día (módulo nuevo respecto al bot original).
 */
object Tasks : LongIdTable("tasks") {
    val userId = reference("user_id", Users)
    val description = varchar("description", 256)
    val done = bool("done").default(false)
    val dueDate = datetime("due_date").nullable()
    val createdAt = datetime("created_at")
}
