package com.trackit.app.data.remote

import com.trackit.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface AuthApi {
    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>
}

interface ObjectsApi {
    @GET("api/objects")
    suspend fun list(): Response<List<TrackedObjectDto>>

    @GET("api/objects/{name}")
    suspend fun getByName(@Path("name") name: String): Response<TrackedObjectDto>

    @POST("api/objects")
    suspend fun create(@Body body: CreateObjectRequest): Response<TrackedObjectDto>

    @PUT("api/objects/{name}")
    suspend fun update(@Path("name") name: String, @Body body: UpdateObjectRequest): Response<TrackedObjectDto>

    @DELETE("api/objects/{name}")
    suspend fun delete(@Path("name") name: String): Response<Unit>
}

interface RemindersApi {
    @GET("api/reminders")
    suspend fun list(): Response<List<ReminderDto>>

    @POST("api/reminders")
    suspend fun create(@Body body: CreateReminderRequest): Response<ReminderDto>

    @PUT("api/reminders/{id}")
    suspend fun update(@Path("id") id: Long, @Body body: UpdateReminderRequest): Response<ReminderDto>

    @DELETE("api/reminders/{id}")
    suspend fun delete(@Path("id") id: Long): Response<Unit>
}

interface PredictApi {
    @GET("api/predict/{name}")
    suspend fun heuristic(@Path("name") name: String): Response<HeuristicPredictionResponse>

    @POST("api/predict/akinator/start")
    suspend fun akinatorStart(@Body body: AkinatorStartRequest): Response<AkinatorStepResponse>

    @POST("api/predict/akinator/answer")
    suspend fun akinatorAnswer(@Body body: AkinatorAnswerRequest): Response<AkinatorStepResponse>
}
