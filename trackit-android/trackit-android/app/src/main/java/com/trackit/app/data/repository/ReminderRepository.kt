package com.trackit.app.data.repository

import com.trackit.app.data.remote.RemindersApi
import com.trackit.app.data.remote.dto.CreateReminderRequest
import com.trackit.app.data.remote.dto.ReminderDto
import com.trackit.app.data.remote.dto.UpdateReminderRequest

class ReminderRepository(private val api: RemindersApi) {

    suspend fun list(): ApiResult<List<ReminderDto>> = apiCall { api.list() }

    suspend fun create(message: String, hour: String, recurring: Boolean): ApiResult<ReminderDto> =
        apiCall { api.create(CreateReminderRequest(message, hour, recurring)) }

    suspend fun toggleActive(id: Long, active: Boolean): ApiResult<ReminderDto> =
        apiCall { api.update(id, UpdateReminderRequest(active = active)) }

    suspend fun delete(id: Long): ApiResult<Unit> = apiCallUnit { api.delete(id) }
}
