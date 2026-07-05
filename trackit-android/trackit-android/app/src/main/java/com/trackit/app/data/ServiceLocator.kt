package com.trackit.app.data

import android.content.Context
import com.trackit.app.data.local.SessionManager
import com.trackit.app.data.remote.ApiClient
import com.trackit.app.data.remote.TrackitApiBundle
import com.trackit.app.data.repository.ObjectRepository
import com.trackit.app.data.repository.PredictRepository
import com.trackit.app.data.repository.ReminderRepository
import com.trackit.app.data.repository.UserRepository

/**
 * En vez de un framework de inyección de dependencias (Hilt/Dagger, fuera del
 * alcance de este proyecto escolar), un localizador simple con instancias
 * únicas por proceso. Suficiente para el tamaño de esta app.
 */
object ServiceLocator {

    @Volatile private var sessionManager: SessionManager? = null
    @Volatile private var apiBundle: TrackitApiBundle? = null

    fun sessionManager(context: Context): SessionManager =
        sessionManager ?: synchronized(this) {
            sessionManager ?: SessionManager(context.applicationContext).also { sessionManager = it }
        }

    private fun apiBundle(context: Context): TrackitApiBundle =
        apiBundle ?: synchronized(this) {
            apiBundle ?: ApiClient.create(sessionManager(context)).also { apiBundle = it }
        }

    fun userRepository(context: Context) = UserRepository(apiBundle(context).auth, sessionManager(context))
    fun objectRepository(context: Context) = ObjectRepository(apiBundle(context).objects)
    fun reminderRepository(context: Context) = ReminderRepository(apiBundle(context).reminders)
    fun predictRepository(context: Context) = PredictRepository(apiBundle(context).predict)
}
