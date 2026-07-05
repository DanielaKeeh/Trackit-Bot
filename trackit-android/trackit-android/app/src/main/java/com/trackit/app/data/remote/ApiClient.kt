package com.trackit.app.data.remote

import com.trackit.app.BuildConfig
import com.trackit.app.data.local.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Agrega el header "Authorization: Bearer <token>" a cada request,
 * igual que espera JwtConfig en el backend (plugins/Security.kt).
 */
class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = sessionManager.token

        val request = if (!token.isNullOrBlank()) {
            original.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }
        return chain.proceed(request)
    }
}

object ApiClient {

    fun create(sessionManager: SessionManager): TrackitApiBundle {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return TrackitApiBundle(
            auth = retrofit.create(AuthApi::class.java),
            objects = retrofit.create(ObjectsApi::class.java),
            reminders = retrofit.create(RemindersApi::class.java),
            predict = retrofit.create(PredictApi::class.java)
        )
    }
}

/** Agrupa las cuatro interfaces Retrofit para no repetir la construcción en cada pantalla. */
data class TrackitApiBundle(
    val auth: AuthApi,
    val objects: ObjectsApi,
    val reminders: RemindersApi,
    val predict: PredictApi
)
