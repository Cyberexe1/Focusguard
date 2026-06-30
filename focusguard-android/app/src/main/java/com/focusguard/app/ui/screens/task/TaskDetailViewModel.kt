package com.focusguard.app.ui.screens.task

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusguard.app.data.repository.TaskRepository
import com.focusguard.app.data.repository.TaskResult
import com.focusguard.app.domain.model.Task
import com.focusguard.app.domain.model.TaskStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskDetailUiState(
    val task: Task? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isCompleted: Boolean = false,
    val isSubTaskSaving: Boolean = false,
    val checkedInToday: Boolean = false,
    val checkInStreakMsg: String = "",
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
) : ViewModel() {

    private val taskId: String = checkNotNull(savedStateHandle["taskId"])

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadTask()
        loadCheckInStatus()
    }

    private fun loadTask() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = taskRepository.getTask(taskId)) {
                is TaskResult.Success -> _uiState.update { it.copy(isLoading = false, task = result.data) }
                is TaskResult.Error   -> _uiState.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }

    private fun loadCheckInStatus() {
        viewModelScope.launch {
            when (val result = taskRepository.getCheckInStatus()) {
                is TaskResult.Success -> _uiState.update { it.copy(checkedInToday = result.data.checkedInToday) }
                is TaskResult.Error   -> { /* non-critical */ }
            }
        }
    }

    fun markComplete() {
        viewModelScope.launch {
            when (val result = taskRepository.updateTaskStatus(taskId, TaskStatus.completed)) {
                is TaskResult.Success -> _uiState.update { it.copy(task = result.data, isCompleted = true) }
                is TaskResult.Error   -> _uiState.update { it.copy(error = result.message) }
            }
        }
    }

    // ── Sub-tasks ────────────────────────────────────────────────────────────

    fun addSubTask(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubTaskSaving = true) }
            when (val result = taskRepository.addSubTask(taskId, title.trim())) {
                is TaskResult.Success -> _uiState.update { it.copy(isSubTaskSaving = false, task = result.data) }
                is TaskResult.Error   -> _uiState.update { it.copy(isSubTaskSaving = false, error = result.message) }
            }
        }
    }

    fun toggleSubTask(subId: String, done: Boolean) {
        viewModelScope.launch {
            when (val result = taskRepository.toggleSubTask(taskId, subId, done)) {
                is TaskResult.Success -> _uiState.update { it.copy(task = result.data) }
                is TaskResult.Error   -> _uiState.update { it.copy(error = result.message) }
            }
        }
    }

    fun deleteSubTask(subId: String) {
        viewModelScope.launch {
            when (val result = taskRepository.deleteSubTask(taskId, subId)) {
                is TaskResult.Success -> _uiState.update { it.copy(task = result.data) }
                is TaskResult.Error   -> _uiState.update { it.copy(error = result.message) }
            }
        }
    }

    // ── Daily check-in ───────────────────────────────────────────────────────

    fun dailyCheckIn(note: String = "") {
        viewModelScope.launch {
            when (val result = taskRepository.dailyCheckIn(note)) {
                is TaskResult.Success -> {
                    val streak = (_uiState.value.task?.checkinStreak ?: 0) + 1
                    _uiState.update {
                        it.copy(
                            checkedInToday = true,
                            checkInStreakMsg = "🔥 $streak day streak! Keep it up.",
                        )
                    }
                    loadTask() // refresh streak counter from server
                }
                is TaskResult.Error -> _uiState.update { it.copy(error = result.message) }
            }
        }
    }
}
