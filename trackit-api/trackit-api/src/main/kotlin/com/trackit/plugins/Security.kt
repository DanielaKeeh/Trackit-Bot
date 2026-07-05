package com.trackit.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import java.util.Date

/**
 * Configuración de autenticación por token (sección 7.4 - Seguridad del
 * protocolo). En producción, JWT_SECRET debe venir de una variable de
 * entorno real (AWS Secrets Manager / Lambda env vars), nunca hardcodeado
 * como ocurría con el bot_token en kybusbot.yaml del bot original.
 */
object JwtConfig {
    private val secret = System.getenv("JWT_SECRET") ?: "dev-secret-cambiar-en-produccion"
    const val issuer = "trackit-api"
    const val audience = "trackit-app"
    const val realm = "trackit"
    private val algorithm = Algorithm.HMAC256(secret)

    val verifier = JWT.require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .build()

    fun generateToken(userId: Long, username: String): String = JWT.create()
        .withIssuer(issuer)
        .withAudience(audience)
        .withClaim("userId", userId)
        .withClaim("username", username)
        .withExpiresAt(Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000)) // 7 días
        .sign(algorithm)
}

fun Application.configureSecurity() {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = JwtConfig.realm
            verifier(JwtConfig.verifier)
            validate { credential ->
                val userId = credential.payload.getClaim("userId").asLong()
                if (userId != null) JWTPrincipal(credential.payload) else null
            }
            challenge { _, _ ->
                call.respond(
                    io.ktor.http.HttpStatusCode.Unauthorized,
                    com.trackit.models.ErrorResponse("Token inválido o expirado")
                )
            }
        }
    }
}

/** Extrae el userId autenticado desde el JWTPrincipal actual. */
fun JWTPrincipal.userId(): Long = payload.getClaim("userId").asLong()
