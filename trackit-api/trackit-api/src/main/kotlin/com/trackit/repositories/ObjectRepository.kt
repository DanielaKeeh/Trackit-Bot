package com.trackit.repositories

import com.trackit.models.TrackedObjects
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime

data class TrackedObjectRecord(
    val id: Long,
    val userId: Long,
    val name: String,
    val place: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

/**
 * Traducción de ModeloObjetos (que heredaba de BaseModel en Ruby) a un
 * repositorio Exposed. Los mismos métodos crear/buscar/actualizar/eliminar/listar
 * existen aquí, con el alcance limitado por userId (cada usuario solo ve
 * sus propios objetos, ver sección 7.4 - Seguridad).
 */
class ObjectRepository {

    suspend fun listar(userId: Long): List<TrackedObjectRecord> = newSuspendedTransaction(Dispatchers.IO) {
        TrackedObjects.selectAll().where { TrackedObjects.userId eq userId }
            .map { it.toRecord() }
    }

    suspend fun buscarPorNombre(userId: Long, name: String): TrackedObjectRecord? =
        newSuspendedTransaction(Dispatchers.IO) {
            TrackedObjects.selectAll()
                .where { (TrackedObjects.userId eq userId) and (TrackedObjects.name eq name) }
                .limit(1)
                .map { it.toRecord() }
                .firstOrNull()
        }

    suspend fun crear(userId: Long, name: String, place: String): TrackedObjectRecord =
        newSuspendedTransaction(Dispatchers.IO) {
            val now = LocalDateTime.now()
            val id = TrackedObjects.insertAndGetId {
                it[TrackedObjects.userId] = userId
                it[TrackedObjects.name] = name
                it[TrackedObjects.place] = place
                it[TrackedObjects.createdAt] = now
                it[TrackedObjects.updatedAt] = now
            }
            TrackedObjectRecord(id.value, userId, name, place, now, now)
        }

    suspend fun actualizarLugar(userId: Long, name: String, nuevoLugar: String): Boolean =
        newSuspendedTransaction(Dispatchers.IO) {
            val filas = TrackedObjects.update({ (TrackedObjects.userId eq userId) and (TrackedObjects.name eq name) }) {
                it[TrackedObjects.place] = nuevoLugar
                it[TrackedObjects.updatedAt] = LocalDateTime.now()
            }
            filas > 0
        }

    suspend fun eliminar(userId: Long, name: String): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val filas = TrackedObjects.deleteWhere { (TrackedObjects.userId eq userId) and (TrackedObjects.name eq name) }
        filas > 0
    }

    private fun ResultRow.toRecord() = TrackedObjectRecord(
        id = this[TrackedObjects.id].value,
        userId = this[TrackedObjects.userId].value,
        name = this[TrackedObjects.name],
        place = this[TrackedObjects.place],
        createdAt = this[TrackedObjects.createdAt],
        updatedAt = this[TrackedObjects.updatedAt]
    )
}
