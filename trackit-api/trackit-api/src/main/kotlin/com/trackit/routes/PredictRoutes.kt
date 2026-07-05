package com.trackit.routes

import com.trackit.models.AkinatorAnswerRequest
import com.trackit.models.AkinatorQuestionDto
import com.trackit.models.AkinatorStartRequest
import com.trackit.models.AkinatorStepResponse
import com.trackit.models.ErrorResponse
import com.trackit.models.HeuristicPredictionResponse
import com.trackit.plugins.userId
import com.trackit.predictor.AkinatorEngine
import com.trackit.predictor.ObjectRecord
import com.trackit.predictor.Predictor
import com.trackit.repositories.ObjectRepository
import com.trackit.repositories.TrackedObjectRecord
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Módulo /api/predict (equivalente a /PredecirObjeto en lib/predictor.rb,
 * extendido con el flujo guiado "modo Akinator" de la sección 3.1/6.1).
 *
 * IMPORTANTE: el heurístico actual solo tiene UN registro histórico por
 * objeto (name+place, se sobrescribe en cada actualización), igual que en
 * el bot original. Para que la franja horaria y el modo Akinator tengan
 * variedad real de datos que analizar, este módulo asume que en el futuro
 * se guardará un historial de ubicaciones por objeto en vez de solo la
 * última. Ver README, sección "Próximos pasos".
 */
fun Route.predictRoutes(objectRepository: ObjectRepository) {
    authenticate("auth-jwt") {
        route("/api/predict") {

            // Heurístico simple, sin preguntas (equivalente directo a /PredecirObjeto)
            get("/{name}") {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val name = call.parameters["name"]!!
                val objeto = objectRepository.buscarPorNombre(userId, name)

                if (objeto == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        HeuristicPredictionResponse(
                            objectName = name,
                            place = null,
                            confidence = null,
                            message = "No tengo historial de $name, regístralo primero"
                        )
                    )
                    return@get
                }

                val prediccion = Predictor(listOf(objeto.toObjectRecord())).predecir()
                call.respond(
                    HeuristicPredictionResponse(
                        objectName = name,
                        place = prediccion?.place,
                        confidence = prediccion?.confidence,
                        message = if (prediccion != null) {
                            "Creo que $name podría estar en: ${prediccion.place}"
                        } else {
                            "No tengo historial de $name, regístralo primero"
                        }
                    )
                )
            }

            // Modo Akinator: primer paso, solo el nombre del objeto
            post("/akinator/start") {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val body = call.receive<AkinatorStartRequest>()
                val objeto = objectRepository.buscarPorNombre(userId, body.objectName)

                if (objeto == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponse("No tengo historial de ${body.objectName}, regístralo primero")
                    )
                    return@post
                }

                val resultado = AkinatorEngine.siguientePaso(listOf(objeto.toObjectRecord()), step = 0, answers = emptyMap())
                call.respond(resultado.toDto())
            }

            // Modo Akinator: siguiente paso, con las respuestas acumuladas
            post("/akinator/answer") {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val body = call.receive<AkinatorAnswerRequest>()
                val objeto = objectRepository.buscarPorNombre(userId, body.objectName)

                if (objeto == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponse("No tengo historial de ${body.objectName}, regístralo primero")
                    )
                    return@post
                }

                val resultado = AkinatorEngine.siguientePaso(
                    listOf(objeto.toObjectRecord()),
                    step = body.step,
                    answers = body.answers
                )
                call.respond(resultado.toDto())
            }
        }
    }
}

private fun TrackedObjectRecord.toObjectRecord() = ObjectRecord(
    place = place,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun AkinatorEngine.Resultado.toDto() = AkinatorStepResponse(
    finished = finished,
    question = nextQuestion?.let { AkinatorQuestionDto(it.id, it.text, it.options) },
    place = place,
    confidence = confidence,
    message = message
)
