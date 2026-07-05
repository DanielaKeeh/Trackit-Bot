package com.trackit.app.ui.objects

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.remote.dto.HeuristicPredictionResponse
import com.trackit.app.data.remote.dto.TrackedObjectDto
import com.trackit.app.data.repository.ApiResult
import com.trackit.app.data.repository.ObjectRepository
import com.trackit.app.data.repository.PredictRepository
import kotlinx.coroutines.launch

sealed class ObjectsUiEvent {
    data class Error(val message: String) : ObjectsUiEvent()
    data class PredictionReady(val objectName: String, val response: HeuristicPredictionResponse) : ObjectsUiEvent()
}

class ObjectsViewModel(
    private val objectRepository: ObjectRepository,
    private val predictRepository: PredictRepository
) : ViewModel() {

    private val _objects = MutableLiveData<List<TrackedObjectDto>>(emptyList())
    val objects: LiveData<List<TrackedObjectDto>> = _objects

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _events = MutableLiveData<ObjectsUiEvent?>()
    val events: LiveData<ObjectsUiEvent?> = _events

    fun load() {
        _loading.value = true
        viewModelScope.launch {
            when (val result = objectRepository.list()) {
                is ApiResult.Success -> _objects.value = result.data
                is ApiResult.Failure -> _events.value = ObjectsUiEvent.Error(result.message)
            }
            _loading.value = false
        }
    }

    fun create(name: String, place: String) {
        viewModelScope.launch {
            when (val result = objectRepository.create(name, place)) {
                is ApiResult.Success -> load()
                is ApiResult.Failure -> _events.value = ObjectsUiEvent.Error(result.message)
            }
        }
    }

    fun update(name: String, newPlace: String) {
        viewModelScope.launch {
            when (val result = objectRepository.update(name, newPlace)) {
                is ApiResult.Success -> load()
                is ApiResult.Failure -> _events.value = ObjectsUiEvent.Error(result.message)
            }
        }
    }

    fun delete(name: String) {
        viewModelScope.launch {
            when (val result = objectRepository.delete(name)) {
                is ApiResult.Success -> load()
                is ApiResult.Failure -> _events.value = ObjectsUiEvent.Error(result.message)
            }
        }
    }

    fun predict(name: String) {
        viewModelScope.launch {
            when (val result = predictRepository.heuristic(name)) {
                is ApiResult.Success -> _events.value = ObjectsUiEvent.PredictionReady(name, result.data)
                is ApiResult.Failure -> _events.value = ObjectsUiEvent.Error(result.message)
            }
        }
    }

    fun consumeEvent() {
        _events.value = null
    }
}
