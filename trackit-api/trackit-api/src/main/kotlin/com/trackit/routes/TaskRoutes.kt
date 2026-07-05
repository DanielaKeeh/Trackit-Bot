package com.trackit.routes

import com.trackit.models.CreateTaskRequest
import com.trackit.models.ErrorResponse
import com.trackit.models.TaskDto
import com.trackit.models.UpdateTaskRequest
import com.trackit.plugins.userId
import com.trackit.repositories.TaskRecord
import com.trackit.repositories.TaskRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME

private fun TaskRecord.toDto() = TaskDto(id, description, done, dueDate?.format(FORMATTER))

/**
 * Módulo /api/tasks (pendientes/mandados del día, sección 3.1 del protocolo).
 * Es un módulo nuevo respecto al bot original: el bot solo manejaba objetos
 * y recordatorios, no una lista de tareas independiente.
 */
fun Route.taskRoutes(taskRepository: TaskRepository) {
    authenticate("auth-jwt") {
        route("/api/tasks") {

            get {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                call.respond(taskRepository.listar(userId).map { it.toDto() })
            }

            post {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val body = call.receive<CreateTaskRequest>()
                val dueDate = body.dueDate?.let { LocalDateTime.parse(it, FORMATTER) }
                val creado = taskRepository.crear(userId, body.description, dueDate)
                call.respond(HttpStatusCode.Created, creado.toDto())
            }

            put("/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val id = call.parameters["id"]!!.toLong()
                val body = call.receive<UpdateTaskRequest>()
                val dueDate = body.dueDate?.let { LocalDateTime.parse(it, FORMATTER) }

                val actualizado = taskRepository.actualizar(userId, id, body.description, body.done, dueDate)
                if (!actualizado) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Tarea no encontrada"))
                    return@put
                }
                call.respond(HttpStatusCode.OK, mapOf("message" to "Tarea actualizada"))
            }

            delete("/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val id = call.parameters["id"]!!.toLong()
                val eliminado = taskRepository.eliminar(userId, id)
                if (!eliminado) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Tarea no encontrada"))
                    return@delete
                }
                call.respond(HttpStatusCode.OK, mapOf("message" to "Tarea eliminada"))
            }
        }
    }
}
