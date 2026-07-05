package com.trackit.app.data.remote.dto

// ---------- Auth ----------

data class RegisterRequest(val username: String, val password: String)
data class LoginRequest(val username: String, val password: String)
data class AuthResponse(val token: String, val username: String)

// ---------- Objects ----------

data class TrackedObjectDto(
    val id: Long,
    val name: String,
    val place: String,
    val createdAt: String,
    val updatedAt: String
)

data class CreateObjectRequest(val name: String, val place: String)
data class UpdateObjectRequest(val place: String)

// ---------- Reminders ----------

data class ReminderDto(
    val id: Long,
    val message: String,
    val hour: String,
    val recurring: Boolean,
    val daysOfWeek: String?,
    val active: Boolean
)

data class CreateReminderRequest(
    val message: String,
    val hour: String,
    val recurring: Boolean = false,
    val daysOfWeek: String? = null
)

data class UpdateReminderRequest(
    val message: String? = null,
    val hour: String? = null,
    val recurring: Boolean? = null,
    val daysOfWeek: String? = null,
    val active: Boolean? = null
)

// ---------- Prediction ----------

data class HeuristicPredictionResponse(
    val objectName: String,
    val place: String?,
    val confidence: Int?,
    val message: String
)

data class AkinatorStartRequest(val objectName: String)

data class AkinatorAnswerRequest(
    val objectName: String,
    val step: Int,
    val answers: Map<String, String> = emptyMap()
)

data class AkinatorQuestionDto(val id: String, val text: String, val options: List<String>)

data class AkinatorStepResponse(
    val finished: Boolean,
    val question: AkinatorQuestionDto? = null,
    val place: String? = null,
    val confidence: Int? = null,
    val message: String? = null
)

data class ErrorResponse(val error: String)
