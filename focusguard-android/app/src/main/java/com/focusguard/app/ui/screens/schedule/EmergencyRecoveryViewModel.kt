package com.focusguard.app.ui.screens.schedule

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusguard.app.data.repository.ScheduleRepository
import com.focusguard.app.data.repository.TaskRepository
import com.focusguard.app.data.repository.TaskResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecoveryPlanPhase(
    val title: String,
    val timing: String,
    val description: String,
)

data class EmergencyUiState(
    val taskTitle: String = "",
    val taskDeadline: String = "",
    val phases: List<RecoveryPlanPhase> = emptyList(),
    val postponedTasks: List<String> = emptyList(),
    val warningMessage: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val accepted: Boolean = false,
)

@HiltViewModel
class EmergencyRecoveryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val scheduleRepository: ScheduleRepository,
    private val taskRepository: TaskRepository,
) : ViewModel() {

    private val taskId: String = checkNotNull(savedStateHandle["taskId"])
    private val _uiState = MutableStateFlow(EmergencyUiState())
    val uiState = _uiState.asStateFlow()

    init { loadPlan() }

    fun loadPlan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Load task title first
            when (val r = taskRepository.getTask(taskId)) {
                is TaskResult.Success -> _uiState.update {
                    it.copy(taskTitle = r.data.title, taskDeadline = r.data.deadline)
                }
                is TaskResult.Error -> { /* non-critical */ }
            }

            // Call emergency plan endpoint
            scheduleRepository.generateEmergencyPlan(taskId)
                .onSuccess { raw ->
                    @Suppress("UNCHECKED_CAST")
                    val planList = raw["plan"] as? List<Map<String, Any>> ?: emptyList()
                    val phases = planList.map { p ->
                        RecoveryPlanPhase(
                            title = p["phase"]?.toString() ?: "Phase",
                            timing = "${p["startOffset"] ?: "NOW"} · ${p["duration"] ?: ""}",
                            description = p["description"]?.toString() ?: "",
                        )
                    }
                    @Suppress("UNCHECKED_CAST")
                    val postponed = raw["postponed_task_ids"] as? List<String> ?: emptyList()
                    val warning = raw["warning_message"]?.toString() ?: ""
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            phases = phases,
                            postponedTasks = postponed,
                            warningMessage = warning,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }
}
