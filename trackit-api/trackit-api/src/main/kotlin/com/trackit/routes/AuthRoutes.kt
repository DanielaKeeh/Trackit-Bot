package com.trackit.routes

import com.trackit.models.AuthResponse
import com.trackit.models.ErrorResponse
import com.trackit.models.LoginRequest
import com.trackit.models.RegisterRequest
import com.trackit.plugins.JwtConfig
import com.trackit.repositories.PasswordHasher
import com.trackit.repositories.UserRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Módulo /api/auth (ver tabla de endpoints, sección 9.2).
 * Sustituye la identidad implícita por channel_id de Telegram por un
 * registro/login real con usuario y contraseña.
 */
fun Route.authRoutes(userRepository: UserRepository) {
    route("/api/auth") {

        post("/register") {
            val body = call.receive<RegisterRequest>()
            if (body.username.isBlank() || body.password.length < 6) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("El usuario no puede estar vacío y la contraseña debe tener al menos 6 caracteres")
                )
                return@post
            }

            val existente = userRepository.findByUsername(body.username)
            if (existente != null) {
                call.respond(HttpStatusCode.Conflict, ErrorResponse("Ese nombre de usuario ya existe"))
                return@post
            }

            val user = userRepository.create(body.username, body.password)
            val token = JwtConfig.generateToken(user.id, user.username)
            call.respond(HttpStatusCode.Created, AuthResponse(token, user.username))
        }

        post("/login") {
            val body = call.receive<LoginRequest>()
            val user = userRepository.findByUsername(body.username)

            if (user == null || !PasswordHasher.verify(body.password, user.salt, user.passwordHash)) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Usuario o contraseña incorrectos"))
                return@post
            }

            val token = JwtConfig.generateToken(user.id, user.username)
            call.respond(HttpStatusCode.OK, AuthResponse(token, user.username))
        }
    }
}
