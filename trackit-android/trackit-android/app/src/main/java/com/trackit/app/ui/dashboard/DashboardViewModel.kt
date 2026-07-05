package com.trackit.app.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.remote.dto.ReminderDto
import com.trackit.app.data.remote.dto.TrackedObjectDto
import com.trackit.app.data.repository.ApiResult
import com.trackit.app.data.repository.ObjectRepository
import com.trackit.app.data.repository.ReminderRepository
import kotlinx.coroutines.launch

data class DashboardState(
    val objectsCount: Int = 0,
    val remindersCount: Int = 0,
    val recentObjects: List<TrackedObjectDto> = emptyList(),
    val upcomingReminders: List<ReminderDto> = emptyList()
)

class DashboardViewModel(
    private val objectRepository: ObjectRepository,
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    private val _state = MutableLiveData(DashboardState())
    val state: LiveData<DashboardState> = _state

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun load() {
        _loading.value = true
        viewModelScope.launch {
            val objectsResult = objectRepository.list()
            val remindersResult = reminderRepository.list()

            val objects = (objectsResult as? ApiResult.Success)?.data ?: emptyList()
            val reminders = (remindersResult as? ApiResult.Success)?.data ?: emptyList()

            _state.value = DashboardState(
                objectsCount = objects.size,
                remindersCount = reminders.count { it.active },
                recentObjects = objects.sortedByDescending { it.updatedAt }.take(5),
                upcomingReminders = reminders.filter { it.active }.sortedBy { it.hour }.take(5)
            )

            if (objectsResult is ApiResult.Failure) _error.value = objectsResult.message
            else if (remindersResult is ApiResult.Failure) _error.value = remindersResult.message

            _loading.value = false
        }
    }

    fun consumeError() {
        _error.value = null
    }
}
