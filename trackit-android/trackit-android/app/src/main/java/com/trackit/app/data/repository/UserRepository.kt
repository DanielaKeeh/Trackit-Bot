package com.trackit.app.data.repository

import com.trackit.app.data.local.SessionManager
import com.trackit.app.data.remote.AuthApi
import com.trackit.app.data.remote.dto.LoginRequest
import com.trackit.app.data.remote.dto.RegisterRequest

class UserRepository(private val api: AuthApi, private val session: SessionManager) {

    suspend fun register(username: String, password: String): ApiResult<String> {
        val result = apiCall { api.register(RegisterRequest(username, password)) }
        if (result is ApiResult.Success) saveSession(result.data.token, username)
        return mapToToken(result)
    }

    suspend fun login(username: String, password: String): ApiResult<String> {
        val result = apiCall { api.login(LoginRequest(username, password)) }
        if (result is ApiResult.Success) saveSession(result.data.token, username)
        return mapToToken(result)
    }

    private fun mapToToken(result: ApiResult<com.trackit.app.data.remote.dto.AuthResponse>): ApiResult<String> =
        when (result) {
            is ApiResult.Success -> ApiResult.Success(result.data.token)
            is ApiResult.Failure -> ApiResult.Failure(result.message, result.code)
        }

    private fun saveSession(token: String, username: String) {
        session.token = token
        session.username = username
    }

    fun logout() = session.clear()

    fun isLoggedIn(): Boolean = session.isLoggedIn()

    fun currentUsername(): String? = session.username
}
