package com.trackit.app.data.repository

import com.trackit.app.data.remote.ObjectsApi
import com.trackit.app.data.remote.dto.CreateObjectRequest
import com.trackit.app.data.remote.dto.TrackedObjectDto
import com.trackit.app.data.remote.dto.UpdateObjectRequest

class ObjectRepository(private val api: ObjectsApi) {

    suspend fun list(): ApiResult<List<TrackedObjectDto>> = apiCall { api.list() }

    suspend fun getByName(name: String): ApiResult<TrackedObjectDto> = apiCall { api.getByName(name) }

    suspend fun create(name: String, place: String): ApiResult<TrackedObjectDto> =
        apiCall { api.create(CreateObjectRequest(name, place)) }

    suspend fun update(name: String, newPlace: String): ApiResult<TrackedObjectDto> =
        apiCall { api.update(name, UpdateObjectRequest(newPlace)) }

    suspend fun delete(name: String): ApiResult<Unit> = apiCallUnit { api.delete(name) }
}
