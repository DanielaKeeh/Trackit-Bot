package com.trackit.routes

import com.trackit.models.CreateObjectRequest
import com.trackit.models.ErrorResponse
import com.trackit.models.TrackedObjectDto
import com.trackit.models.UpdateObjectRequest
import com.trackit.plugins.userId
import com.trackit.repositories.ObjectRepository
import com.trackit.repositories.TrackedObjectRecord
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.format.DateTimeFormatter

private val FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME

private fun TrackedObjectRecord.toDto() = TrackedObjectDto(
    id = id,
    name = name,
    place = place,
    createdAt = createdAt.format(FORMATTER),
    updatedAt = updatedAt.format(FORMATTER)
)

/**
 * Módulo /api/objects (equivalente a los comandos
 * /RegistrarObjeto, /BuscarObjeto, /ActualizarObjeto, /EliminarObjeto, /VerObjetos
 * del bot original en lib/commands/*.rb).
 */
fun Route.objectRoutes(objectRepository: ObjectRepository) {
    authenticate("auth-jwt") {
        route("/api/objects") {

            // Equivalente a /VerObjetos
            get {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val objetos = objectRepository.listar(userId)
                call.respond(objetos.map { it.toDto() })
            }

            // Equivalente a /BuscarObjeto nombre
            get("/{name}") {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val name = call.parameters["name"]!!
                val objeto = objectRepository.buscarPorNombre(userId, name)
                if (objeto == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("No encontré ningún objeto llamado $name"))
                } else {
                    call.respond(objeto.toDto())
                }
            }

            // Equivalente a /RegistrarObjeto nombre lugar
            post {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val body = call.receive<CreateObjectRequest>()

                val existente = objectRepository.buscarPorNombre(userId, body.name)
                if (existente != null) {
                    call.respond(HttpStatusCode.Conflict, ErrorResponse("ya existe este objeto"))
                    return@post
                }

                val creado = objectRepository.crear(userId, body.name, body.place)
                call.respond(HttpStatusCode.Created, creado.toDto())
            }

            // Equivalente a /ActualizarObjeto nombre nuevo_lugar
            put("/{name}") {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val name = call.parameters["name"]!!
                val body = call.receive<UpdateObjectRequest>()

                val actualizado = objectRepository.actualizarLugar(userId, name, body.place)
                if (!actualizado) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("No encontré ningún objeto llamado $name"))
                    return@put
                }
                val objeto = objectRepository.buscarPorNombre(userId, name)!!
                call.respond(objeto.toDto())
            }

            // Equivalente a /EliminarObjeto nombre
            delete("/{name}") {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val name = call.parameters["name"]!!

                val eliminado = objectRepository.eliminar(userId, name)
                if (!eliminado) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("No encontré ningún objeto llamado $name"))
                    return@delete
                }
                call.respond(HttpStatusCode.OK, mapOf("message" to "Objeto $name eliminado correctamente"))
            }
        }
    }
}
