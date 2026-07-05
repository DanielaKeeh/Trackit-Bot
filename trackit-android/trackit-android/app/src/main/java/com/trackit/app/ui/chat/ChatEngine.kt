package com.trackit.app.ui.chat

import com.trackit.app.data.remote.dto.AkinatorQuestionDto
import com.trackit.app.data.repository.ApiResult
import com.trackit.app.data.repository.ObjectRepository
import com.trackit.app.data.repository.PredictRepository
import com.trackit.app.data.repository.ReminderRepository

/**
 * Traduce texto de chat a llamadas del REST API, igual que hacía el bot de
 * Telegram con sus comandos (lib/commands/*.rb) — solo que ahora vive en la
 * app (capa de presentación), no en el backend. Ver el diagrama de
 * arquitectura: esta clase es la contraparte Kotlin/Android de lo que antes
 * era el enrutador de comandos de Kybus Bot.
 */
class ChatEngine(
    private val objectRepository: ObjectRepository,
    private val reminderRepository: ReminderRepository,
    private val predictRepository: PredictRepository
) {
    // Estado del flujo Akinator en curso (si el usuario está a mitad de una predicción guiada).
    private var pendingObjectName: String? = null
    private var pendingStep: Int = 0
    private var pendingAnswers: MutableMap<String, String> = mutableMapOf()
    private var pendingQuestion: AkinatorQuestionDto? = null

    suspend fun handle(rawInput: String): String {
        val input = rawInput.trim()
        if (input.isEmpty()) return "Escribe algo, o usa /Ayuda para ver los comandos."

        // Si hay una pregunta de Akinator pendiente y el usuario no mandó un comando nuevo,
        // se interpreta el mensaje como la respuesta a esa pregunta.
        if (pendingQuestion != null && !input.startsWith("/")) {
            return answerAkinator(input)
        }

        val parts = input.split(Regex("\\s+"))
        val command = parts[0].lowercase()

        return when {
            command == "/registrar" -> registrar(parts)
            command == "/buscar" -> buscar(parts)
            command == "/actualizar" -> actualizar(parts)
            command == "/eliminar" -> eliminar(parts)
            command == "/predecir" -> predecir(parts)
            command == "/akinator" -> iniciarAkinator(parts)
            command == "/verobjetos" -> verObjetos()
            command == "/recordatorio" -> recordatorio(parts)
            command == "/ayuda" -> ayuda()
            command == "/start" || input.equals("hola", ignoreCase = true) ->
                "¡Hola! Soy TrackitBot 📦\n¿En qué puedo ayudarte? Usa /Ayuda para ver los comandos."
            input.startsWith("/") -> "No entendí ese comando. Usa /Ayuda para ver los comandos disponibles."
            else -> "No entendí ese mensaje. Usa /Ayuda para ver los comandos disponibles."
        }
    }

    private suspend fun registrar(parts: List<String>): String {
        if (parts.size < 3) return "Formato: /Registrar nombre lugar"
        val name = parts[1]
        val place = parts.drop(2).joinToString(" ")
        return when (val result = objectRepository.create(name, place)) {
            is ApiResult.Success -> "Objeto ${result.data.name} registrado en ${result.data.place} ✅"
            is ApiResult.Failure -> result.message
        }
    }

    private suspend fun buscar(parts: List<String>): String {
        if (parts.size < 2) return "Formato: /Buscar nombre"
        val name = parts[1]
        return when (val result = objectRepository.getByName(name)) {
            is ApiResult.Success -> "Encontré ${result.data.name} - Lugar: ${result.data.place} 📍"
            is ApiResult.Failure -> "No encontré ningún objeto llamado $name"
        }
    }

    private suspend fun actualizar(parts: List<String>): String {
        if (parts.size < 3) return "Formato: /Actualizar nombre nuevo_lugar"
        val name = parts[1]
        val newPlace = parts.drop(2).joinToString(" ")
        return when (val result = objectRepository.update(name, newPlace)) {
            is ApiResult.Success -> "Objeto ${result.data.name} actualizado al lugar: ${result.data.place} ✅"
            is ApiResult.Failure -> "No encontré ningún objeto llamado $name"
        }
    }

    private suspend fun eliminar(parts: List<String>): String {
        if (parts.size < 2) return "Formato: /Eliminar nombre"
        val name = parts[1]
        return when (val result = objectRepository.delete(name)) {
            is ApiResult.Success -> "Objeto $name eliminado correctamente 🗑️"
            is ApiResult.Failure -> "No encontré ningún objeto llamado $name"
        }
    }

    private suspend fun predecir(parts: List<String>): String {
        if (parts.size < 2) return "Formato: /Predecir nombre"
        val name = parts[1]
        return when (val result = predictRepository.heuristic(name)) {
            is ApiResult.Success -> result.data.message +
                if (result.data.confidence != null) " (confianza ${result.data.confidence}%)" else ""
            is ApiResult.Failure -> result.message
        }
    }

    private suspend fun iniciarAkinator(parts: List<String>): String {
        if (parts.size < 2) return "Formato: /Akinator nombre"
        val name = parts[1]
        return when (val result = predictRepository.akinatorStart(name)) {
            is ApiResult.Success -> presentAkinatorStep(name, result.data)
            is ApiResult.Failure -> result.message
        }
    }

    private suspend fun answerAkinator(respuestaUsuario: String): String {
        val question = pendingQuestion ?: return "No hay ninguna predicción en curso."
        val name = pendingObjectName ?: return "No hay ninguna predicción en curso."

        val opcionElegida = question.options.firstOrNull {
            it.equals(respuestaUsuario.trim(), ignoreCase = true)
        } ?: question.options.firstOrNull {
            it.startsWith(respuestaUsuario.trim(), ignoreCase = true)
        }

        if (opcionElegida == null) {
            return "No reconocí esa respuesta. Opciones: ${question.options.joinToString(", ")}"
        }

        pendingAnswers[question.id] = opcionElegida
        pendingStep += 1

        return when (val result = predictRepository.akinatorAnswer(name, pendingStep, pendingAnswers)) {
            is ApiResult.Success -> presentAkinatorStep(name, result.data)
            is ApiResult.Failure -> {
                clearPending()
                result.message
            }
        }
    }

    private fun presentAkinatorStep(objectName: String, step: com.trackit.app.data.remote.dto.AkinatorStepResponse): String {
        return if (step.finished) {
            clearPending()
            if (step.place != null) {
                "🤔 Creo que $objectName podría estar en: ${step.place} (confianza ${step.confidence}%)"
            } else {
                step.message ?: "No tengo suficiente historial de $objectName todavía."
            }
        } else {
            pendingObjectName = objectName
            pendingQuestion = step.question
            val q = step.question!!
            "${q.text}\nOpciones: ${q.options.joinToString(", ")}"
        }
    }

    private fun clearPending() {
        pendingObjectName = null
        pendingStep = 0
        pendingAnswers = mutableMapOf()
        pendingQuestion = null
    }

    private suspend fun verObjetos(): String {
        return when (val result = objectRepository.list()) {
            is ApiResult.Success -> {
                if (result.data.isEmpty()) {
                    "No tienes objetos registrados"
                } else {
                    buildString {
                        append("Tus objetos registrados:\n")
                        result.data.forEachIndexed { index, obj ->
                            append("${index + 1}. ${obj.name} - Lugar: ${obj.place}\n")
                        }
                    }.trim()
                }
            }
            is ApiResult.Failure -> result.message
        }
    }

    private suspend fun recordatorio(parts: List<String>): String {
        if (parts.size < 3) return "Formato: /Recordatorio HH:mm mensaje"
        val hour = parts[1]
        if (!hour.matches(Regex("^\\d{2}:\\d{2}$"))) return "La hora debe tener formato HH:mm, ej. 07:30"
        val message = parts.drop(2).joinToString(" ")

        return when (val result = reminderRepository.create(message, hour, recurring = false)) {
            is ApiResult.Success -> "⏰ Recordatorio configurado para las ${result.data.hour}: \"${result.data.message}\""
            is ApiResult.Failure -> result.message
        }
    }

    private fun ayuda(): String = """
        👋 ¡Hola! Soy TrackitBot 🤖
        Te ayudo a recordar dónde dejaste tus cosas.

        📋 Comandos disponibles:
        📦 /Registrar nombre lugar — Registra un objeto
        📍 /Buscar nombre — Busca dónde dejaste algo
        📝 /VerObjetos — Lista todos tus objetos
        ✏️ /Actualizar nombre nuevo_lugar — Cambia el lugar
        🗑️ /Eliminar nombre — Elimina un objeto
        🔮 /Predecir nombre — Predice dónde está (heurístico directo)
        🎯 /Akinator nombre — Predice con preguntas guiadas
        ⏰ /Recordatorio HH:mm mensaje — Crea un recordatorio
    """.trimIndent()
}
