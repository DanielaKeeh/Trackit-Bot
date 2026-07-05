package com.trackit.predictor

import com.trackit.models.AkinatorQuestionDto
import java.time.LocalDateTime

/**
 * Motor de predicción guiada ("modo Akinator", sección 3.1 y 6.1 del protocolo).
 *
 * Extiende el heurístico de franja horaria + frecuencia (Predictor.kt) con un
 * pequeño árbol de preguntas. La idea, respaldada por Tullio et al. (2007)
 * citado en el protocolo, es que usuarios no técnicos prefieren una
 * conversación guiada en vez de una predicción silenciosa.
 *
 * El motor es "adaptativo": si tras la primera pregunta ya hay suficiente
 * confianza, no se sigue preguntando (igual que Akinator deja de preguntar
 * cuando ya está seguro).
 */
object AkinatorEngine {

    private const val CONFIDENCE_THRESHOLD = 70

    val QUESTION_FRANJA = AkinatorQuestionDto(
        id = "franja",
        text = "¿En qué momento del día sueles perder este objeto?",
        options = listOf("madrugada", "mañana", "tarde", "noche")
    )

    val QUESTION_USO = AkinatorQuestionDto(
        id = "ultimo_uso",
        text = "¿Hace cuánto lo usaste por última vez?",
        options = listOf("hoy", "ayer", "hace_mas_de_2_dias")
    )

    val QUESTION_ZONA = AkinatorQuestionDto(
        id = "zona",
        text = "¿Sueles usarlo dentro o fuera de casa?",
        options = listOf("dentro", "fuera")
    )

    private val ORDER = listOf(QUESTION_FRANJA, QUESTION_USO, QUESTION_ZONA)

    data class Resultado(
        val finished: Boolean,
        val nextQuestion: AkinatorQuestionDto? = null,
        val place: String? = null,
        val confidence: Int? = null,
        val message: String? = null
    )

    /**
     * @param historial registros históricos del objeto (mismo formato que Predictor)
     * @param step índice de la pregunta que el cliente ya respondió (0 = ninguna aún)
     * @param answers respuestas acumuladas, clave = id de pregunta
     */
    fun siguientePaso(
        historial: List<ObjectRecord>,
        step: Int,
        answers: Map<String, String>
    ): Resultado {
        if (historial.isEmpty()) {
            return Resultado(finished = true, message = "Sin historial para este objeto")
        }

        val (lugar, confianza) = puntuar(historial, answers)

        // Si ya alcanzamos confianza suficiente, o ya no quedan preguntas, cerramos.
        if (confianza >= CONFIDENCE_THRESHOLD || step >= ORDER.size) {
            return Resultado(finished = true, place = lugar, confidence = confianza)
        }

        val siguiente = ORDER[step]
        return Resultado(finished = false, nextQuestion = siguiente)
    }

    /**
     * Calcula un score por lugar combinando frecuencia histórica con
     * coincidencias de las respuestas dadas hasta ahora, y devuelve el lugar
     * con más puntos junto con una confianza normalizada 0-100.
     */
    private fun puntuar(historial: List<ObjectRecord>, answers: Map<String, String>): Pair<String, Int> {
        val scores = mutableMapOf<String, Double>()

        for (registro in historial) {
            var peso = 1.0

            answers["franja"]?.let { respuesta ->
                val franjaRespuesta = Franja.entries.find { it.name.equals(respuesta, ignoreCase = true) }
                val franjaRegistro = Franja.de(registro.updatedAt.hour)
                if (franjaRespuesta != null && franjaRespuesta == franjaRegistro) peso += 1.5
            }

            answers["ultimo_uso"]?.let { respuesta ->
                val diasDesdeRegistro = java.time.Duration.between(registro.updatedAt, LocalDateTime.now()).toDays()
                val coincide = when (respuesta) {
                    "hoy" -> diasDesdeRegistro <= 0
                    "ayer" -> diasDesdeRegistro in 0..1
                    "hace_mas_de_2_dias" -> diasDesdeRegistro > 2
                    else -> false
                }
                if (coincide) peso += 1.0
            }

            scores[registro.place] = (scores[registro.place] ?: 0.0) + peso
        }

        val total = scores.values.sum()
        val (lugar, puntos) = scores.maxByOrNull { it.value }!!.toPair()
        val confianza = if (total > 0) ((puntos / total) * 100).toInt() else 0
        return lugar to confianza
    }
}
