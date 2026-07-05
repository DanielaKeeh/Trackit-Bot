package com.trackit.app.data.repository

/** Resultado simple de una llamada al API, sin depender de librerías extra. */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Failure(val message: String, val code: Int? = null) : ApiResult<Nothing>()
}
