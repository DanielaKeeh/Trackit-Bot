package com.trackit.plugins

import com.trackit.models.ErrorResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "Solicitud inválida"))
        }
        exception<NoSuchElementException> { call, _ ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse("Recurso no encontrado"))
        }
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Error no manejado", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Error interno del servidor"))
        }
    }
}
