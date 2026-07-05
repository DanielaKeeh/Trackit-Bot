package com.trackit.predictor

import java.time.LocalDateTime

/**
 * Registro mínimo que el predictor necesita de un objeto rastreado.
 * Se mantiene desacoplado de la tabla Exposed para poder probarlo aislado,
 * igual que el Predictor original en Ruby (lib/predictor.rb) que solo
 * recibía el `historial` (arreglo de hashes).
 */
data class ObjectRecord(
    val place: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

enum class Franja(val rango: IntRange) {
    MADRUGADA(0..5),
    MANANA(6..11),
    TARDE(12..17),
    NOCHE(18..23);

    companion object {
        fun de(hora: Int): Franja? = entries.find { hora in it.rango }
    }
}

/**
 * Traducción directa del algoritmo heurístico de `lib/predictor.rb`:
 * 1. Se agrupan los registros históricos de un objeto por lugar.
 * 2. Se prioriza la franja horaria actual (madrugada/mañana/tarde/noche).
 * 3. Si no hay historial en esa franja, se cae al lugar más frecuente global.
 */
class Predictor(private val historial: List<ObjectRecord>) {

    fun predecir(ahora: LocalDateTime = LocalDateTime.now()): PrediccionSimple? {
        if (historial.isEmpty()) return null

        val franjaActual = Franja.de(ahora.hour)

        val porFranja = historial.filter { franja(horaDe(it)) == franjaActual }
        if (porFranja.isNotEmpty()) {
            val (lugar, confianza) = lugarMasFrecuenteConConfianza(porFranja)
            return PrediccionSimple(lugar, confianza, fuente = "franja_horaria")
        }

        val (lugar, confianza) = lugarMasFrecuenteConConfianza(historial)
        return PrediccionSimple(lugar, confianza, fuente = "frecuencia_general")
    }

    private fun franja(hora: Int): Franja? = Franja.de(hora)

    private fun horaDe(registro: ObjectRecord): Int =
        (registro.updatedAt).hour

    /**
     * Devuelve el lugar más frecuente junto con un nivel de confianza
     * (proporción de registros que corresponden a ese lugar dentro del
     * subconjunto analizado), redondeado a un entero 0-100.
     */
    private fun lugarMasFrecuenteConConfianza(registros: List<ObjectRecord>): Pair<String, Int> {
        val porLugar = registros.groupingBy { it.place }.eachCount()
        val (lugar, conteo) = porLugar.maxByOrNull { it.value }!!.toPair()
        val confianza = ((conteo.toDouble() / registros.size) * 100).toInt()
        return lugar to confianza
    }
}

data class PrediccionSimple(val place: String, val confidence: Int, val fuente: String)
