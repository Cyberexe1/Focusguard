package com.focusguard.app.ui.screens.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusguard.app.data.repository.TaskRepository
import com.focusguard.app.data.repository.TaskResult
import com.focusguard.app.domain.model.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddTaskUiState(
    val rawText: String = "",
    val isAnalyzing: Boolean = false,
    val isSaving: Boolean = false,
    val analyzedTask: Task? = null,
    val error: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class AddTaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTaskUiState())
    val uiState = _uiState.asStateFlow()

    fun onRawTextChange(value: String) = _uiState.update { it.copy(rawText = value, analyzedTask = null, error = null) }

    /** Sends raw text to Bedrock via /tasks endpoint for analysis — returns preview */
    fun analyzeTask() {
        val text = _uiState.value.rawText.trim()
        if (text.isBlank()) {
            _uiState.update { it.copy(error = "Please describe your task first.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, error = null) }
            when (val result = taskRepository.createTask(text)) {
                is TaskResult.Success -> _uiState.update {
                    it.copy(isAnalyzing = false, analyzedTask = result.data)
                }
                is TaskResult.Error -> _uiState.update {
                    it.copy(isAnalyzing = false, error = result.message)
                }
            }
        }
    }

    /** The task is already saved by analyzeTask() — just navigate home */
    fun confirmSave() = _uiState.update { it.copy(saved = true) }
}
