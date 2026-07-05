package com.trackit.app.data.repository

import com.google.gson.Gson
import com.trackit.app.data.remote.dto.ErrorResponse
import retrofit2.Response
import java.io.IOException

/**
 * Envuelve una llamada Retrofit y la convierte a ApiResult, evitando repetir
 * el mismo try/catch + parseo de error en cada repositorio.
 */
suspend fun <T> apiCall(block: suspend () -> Response<T>): ApiResult<T> {
    return try {
        val response = block()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) ApiResult.Success(body)
            else ApiResult.Failure("Respuesta vacía del servidor")
        } else {
            ApiResult.Failure(parseErrorBody(response), response.code())
        }
    } catch (e: IOException) {
        ApiResult.Failure("No se pudo conectar con el servidor")
    } catch (e: Exception) {
        ApiResult.Failure(e.message ?: "Error inesperado")
    }
}

/** Variante para endpoints que responden 200/204 sin body relevante (ej. DELETE). */
suspend fun apiCallUnit(block: suspend () -> Response<Unit>): ApiResult<Unit> {
    return try {
        val response = block()
        if (response.isSuccessful) ApiResult.Success(Unit)
        else ApiResult.Failure(parseErrorBody(response), response.code())
    } catch (e: IOException) {
        ApiResult.Failure("No se pudo conectar con el servidor")
    } catch (e: Exception) {
        ApiResult.Failure(e.message ?: "Error inesperado")
    }
}

private fun parseErrorBody(response: Response<*>): String = try {
    val errorBody = response.errorBody()?.string()
    Gson().fromJson(errorBody, ErrorResponse::class.java)?.error ?: "Error del servidor"
} catch (e: Exception) {
    "Error del servidor"
}
