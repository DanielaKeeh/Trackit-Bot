package com.trackit.plugins

import com.trackit.models.Reminders
import com.trackit.models.Tasks
import com.trackit.models.TrackedObjects
import com.trackit.models.Users
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Entorno local / desarrollo: SQLite (sección 7.3 del protocolo).
 * Para producción, esta capa se reemplazaría por una implementación de
 * repositorios contra DynamoDB (ver README para notas de migración) sin
 * tener que tocar las rutas ni la lógica de negocio, gracias a que los
 * repositorios exponen la misma interfaz.
 */
fun Application.configureDatabases() {
    val dbFile = System.getenv("TRACKIT_DB_PATH") ?: "trackit.db"
    Database.connect("jdbc:sqlite:$dbFile", driver = "org.sqlite.JDBC")

    transaction {
        SchemaUtils.create(Users, TrackedObjects, Reminders, Tasks)
    }
}
