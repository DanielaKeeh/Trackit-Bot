package com.trackit.app.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * Guarda el token JWT emitido por /api/auth (ver Security.kt del backend).
 *
 * Nota: para un proyecto de producción real conviene usar
 * EncryptedSharedPreferences (androidx.security:security-crypto) en vez de
 * SharedPreferences normal, para que el token no quede legible si el celular
 * se pierde o se hace root. Se deja simple aquí por alcance del proyecto.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("trackit_session", Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var username: String?
        get() = prefs.getString(KEY_USERNAME, null)
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    fun isLoggedIn(): Boolean = !token.isNullOrBlank()

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USERNAME = "username"
    }
}
