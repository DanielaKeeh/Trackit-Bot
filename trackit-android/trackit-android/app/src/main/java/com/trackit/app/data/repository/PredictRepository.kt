package com.trackit.app.data.repository

import com.trackit.app.data.remote.PredictApi
import com.trackit.app.data.remote.dto.AkinatorAnswerRequest
import com.trackit.app.data.remote.dto.AkinatorStartRequest
import com.trackit.app.data.remote.dto.AkinatorStepResponse
import com.trackit.app.data.remote.dto.HeuristicPredictionResponse

class PredictRepository(private val api: PredictApi) {

    suspend fun heuristic(name: String): ApiResult<HeuristicPredictionResponse> =
        apiCall { api.heuristic(name) }

    suspend fun akinatorStart(objectName: String): ApiResult<AkinatorStepResponse> =
        apiCall { api.akinatorStart(AkinatorStartRequest(objectName)) }

    suspend fun akinatorAnswer(
        objectName: String,
        step: Int,
        answers: Map<String, String>
    ): ApiResult<AkinatorStepResponse> =
        apiCall { api.akinatorAnswer(AkinatorAnswerRequest(objectName, step, answers)) }
}
