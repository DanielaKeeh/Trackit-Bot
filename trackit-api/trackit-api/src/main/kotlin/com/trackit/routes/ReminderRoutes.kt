package com.trackit.routes

import com.trackit.models.CreateReminderRequest
import com.trackit.models.ErrorResponse
import com.trackit.models.ReminderDto
import com.trackit.models.UpdateReminderRequest
import com.trackit.plugins.userId
import com.trackit.repositories.ReminderRecord
import com.trackit.repositories.ReminderRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private fun ReminderRecord.toDto() = ReminderDto(id, message, hour, recurring, daysOfWeek, active)

/**
 * Módulo /api/reminders (evoluciona el comando /Recordatorio hora).
 */
fun Route.reminderRoutes(reminderRepository: ReminderRepository) {
    authenticate("auth-jwt") {
        route("/api/reminders") {

            get {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                call.respond(reminderRepository.listar(userId).map { it.toDto() })
            }

            post {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val body = call.receive<CreateReminderRequest>()
                if (!body.hour.matches(Regex("^\\d{2}:\\d{2}$"))) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("hour debe tener formato HH:mm"))
                    return@post
                }
                val creado = reminderRepository.crear(userId, body.message, body.hour, body.recurring, body.daysOfWeek)
                call.respond(HttpStatusCode.Created, creado.toDto())
            }

            put("/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val id = call.parameters["id"]!!.toLong()
                val body = call.receive<UpdateReminderRequest>()

                val actualizado = reminderRepository.actualizar(
                    userId, id, body.message, body.hour, body.recurring, body.daysOfWeek, body.active
                )
                if (!actualizado) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Recordatorio no encontrado"))
                    return@put
                }
                val reminder = reminderRepository.buscar(userId, id)!!
                call.respond(reminder.toDto())
            }

            delete("/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val id = call.parameters["id"]!!.toLong()
                val eliminado = reminderRepository.eliminar(userId, id)
                if (!eliminado) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Recordatorio no encontrado"))
                    return@delete
                }
                call.respond(HttpStatusCode.OK, mapOf("message" to "Recordatorio eliminado"))
            }
        }
    }
}
