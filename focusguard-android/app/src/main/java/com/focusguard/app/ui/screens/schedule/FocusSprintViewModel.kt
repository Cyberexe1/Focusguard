package com.focusguard.app.ui.screens.schedule

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusguard.app.data.api.SprintDto
import com.focusguard.app.data.repository.SprintRepository
import com.focusguard.app.data.repository.TaskRepository
import com.focusguard.app.data.repository.TaskResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SprintUiState(
    val sprint: SprintDto? = null,
    val taskTitle: String = "",
    val isStarting: Boolean = false,
    val isEnded: Boolean = false,
    val escalationRequired: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class FocusSprintViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sprintRepository: SprintRepository,
    private val taskRepository: TaskRepository,
) : ViewModel() {

    private val taskId: String = checkNotNull(savedStateHandle["taskId"])
    private val _uiState = MutableStateFlow(SprintUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadTaskTitle()
    }

    private fun loadTaskTitle() {
        viewModelScope.launch {
            when (val result = taskRepository.getTask(taskId)) {
                is TaskResult.Success -> _uiState.update { it.copy(taskTitle = result.data.title) }
                is TaskResult.Error -> { /* title stays empty, not critical */ }
            }
        }
    }

    fun startSprint(durationHours: Float = 2f) {
        viewModelScope.launch {
            _uiState.update { it.copy(isStarting = true) }
            sprintRepository.startSprint(taskId, durationHours)
                .onSuccess { s -> _uiState.update { it.copy(isStarting = false, sprint = s) } }
                .onFailure { e -> _uiState.update { it.copy(isStarting = false, error = e.message) } }
        }
    }

    fun logCheckpoint(progressMade: Boolean) {
        val sprintId = _uiState.value.sprint?.sprintId ?: return
        viewModelScope.launch {
            sprintRepository.logCheckpoint(sprintId, progressMade)
                .onSuccess { s ->
                    _uiState.update { it.copy(sprint = s, escalationRequired = s.escalationRequired) }
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun endSprint(completionPercent: Int) {
        val sprintId = _uiState.value.sprint?.sprintId ?: return
        viewModelScope.launch {
            sprintRepository.endSprint(sprintId, completionPercent)
                .onSuccess { _uiState.update { it.copy(isEnded = true) } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }
}
