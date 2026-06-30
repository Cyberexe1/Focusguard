package com.focusguard.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusguard.app.data.local.SessionDataStore
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

data class HomeUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val userName: String = "",
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val sessionDataStore: SessionDataStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadUserName()
        loadTasks()
    }

    private fun loadUserName() {
        viewModelScope.launch {
            val session = sessionDataStore.currentSession()
            _uiState.update { it.copy(userName = session?.name?.ifBlank { "User" } ?: "User") }
        }
    }

    fun loadTasks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = taskRepository.getTasks()) {
                is TaskResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        tasks = result.data.sortedWith(
                            // Active tasks first (by priority), completed ones pushed to bottom
                            compareBy<Task> { it.status == TaskStatus.completed }
                                .thenByDescending { it.priorityScore }
                        ),
                    )
                }
                is TaskResult.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    val activeTasks get() = _uiState.value.tasks.count { it.status.name != "completed" }
    val atRiskTasks  get() = _uiState.value.tasks.count { it.priorityScore >= 80 }
}
