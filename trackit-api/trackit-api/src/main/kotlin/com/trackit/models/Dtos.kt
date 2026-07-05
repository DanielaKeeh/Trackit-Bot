package com.trackit.models

import kotlinx.serialization.Serializable

// ---------- Auth ----------

@Serializable
data class RegisterRequest(val username: String, val password: String)

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class AuthResponse(val token: String, val username: String)

// ---------- Objects ----------

@Serializable
data class TrackedObjectDto(
    val id: Long,
    val name: String,
    val place: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateObjectRequest(val name: String, val place: String)

@Serializable
data class UpdateObjectRequest(val place: String)

// ---------- Reminders ----------

@Serializable
data class ReminderDto(
    val id: Long,
    val message: String,
    val hour: String,
    val recurring: Boolean,
    val daysOfWeek: String?,
    val active: Boolean
)

@Serializable
data class CreateReminderRequest(
    val message: String,
    val hour: String,
    val recurring: Boolean = false,
    val daysOfWeek: String? = null
)

@Serializable
data class UpdateReminderRequest(
    val message: String? = null,
    val hour: String? = null,
    val recurring: Boolean? = null,
    val daysOfWeek: String? = null,
    val active: Boolean? = null
)

// ---------- Tasks (pendientes) ----------

@Serializable
data class TaskDto(
    val id: Long,
    val description: String,
    val done: Boolean,
    val dueDate: String?
)

@Serializable
data class CreateTaskRequest(val description: String, val dueDate: String? = null)

@Serializable
data class UpdateTaskRequest(val description: String? = null, val done: Boolean? = null, val dueDate: String? = null)

// ---------- Prediction ----------

@Serializable
data class HeuristicPredictionResponse(
    val objectName: String,
    val place: String?,
    val confidence: Int?,
    val message: String
)

@Serializable
data class AkinatorStartRequest(val objectName: String)

@Serializable
data class AkinatorAnswerRequest(
    val objectName: String,
    val step: Int,
    // respuestas acumuladas de pasos anteriores, clave = id de la pregunta
    val answers: Map<String, String> = emptyMap()
)

@Serializable
data class AkinatorQuestionDto(val id: String, val text: String, val options: List<String>)

@Serializable
data class AkinatorStepResponse(
    val finished: Boolean,
    val question: AkinatorQuestionDto? = null,
    val place: String? = null,
    val confidence: Int? = null,
    val message: String? = null
)

@Serializable
data class ErrorResponse(val error: String)
