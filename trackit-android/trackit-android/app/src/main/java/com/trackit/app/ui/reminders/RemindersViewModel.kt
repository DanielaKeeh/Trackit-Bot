package com.trackit.app.ui.reminders

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.remote.dto.ReminderDto
import com.trackit.app.data.repository.ApiResult
import com.trackit.app.data.repository.ReminderRepository
import kotlinx.coroutines.launch

class RemindersViewModel(private val repository: ReminderRepository) : ViewModel() {

    private val _reminders = MutableLiveData<List<ReminderDto>>(emptyList())
    val reminders: LiveData<List<ReminderDto>> = _reminders

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun load() {
        _loading.value = true
        viewModelScope.launch {
            when (val result = repository.list()) {
                is ApiResult.Success -> _reminders.value = result.data
                is ApiResult.Failure -> _error.value = result.message
            }
            _loading.value = false
        }
    }

    fun create(message: String, hour: String, recurring: Boolean) {
        viewModelScope.launch {
            when (val result = repository.create(message, hour, recurring)) {
                is ApiResult.Success -> load()
                is ApiResult.Failure -> _error.value = result.message
            }
        }
    }

    fun toggleActive(id: Long, active: Boolean) {
        viewModelScope.launch {
            when (val result = repository.toggleActive(id, active)) {
                is ApiResult.Success -> load()
                is ApiResult.Failure -> _error.value = result.message
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            when (val result = repository.delete(id)) {
                is ApiResult.Success -> load()
                is ApiResult.Failure -> _error.value = result.message
            }
        }
    }

    fun consumeError() {
        _error.value = null
    }
}
