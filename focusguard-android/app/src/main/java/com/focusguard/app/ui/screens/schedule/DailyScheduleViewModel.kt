package com.focusguard.app.ui.screens.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusguard.app.data.api.ScheduleDto
import com.focusguard.app.data.api.UserScheduleBlockDto
import com.focusguard.app.data.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class ScheduleUiState(
    val schedule: ScheduleDto? = null,
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val error: String? = null,
    val userBlocks: List<ScheduleBlockInput> = emptyList(),
    val blocksLoading: Boolean = false,
    val isSaving: Boolean = false,
)

@HiltViewModel
class DailyScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadTodaySchedule()
        loadUserBlocks()
    }

    fun loadTodaySchedule() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            scheduleRepository.getTodaySchedule()
                .onSuccess { s -> _uiState.update { it.copy(isLoading = false, schedule = s) } }
                .onFailure { _uiState.update { it.copy(isLoading = false) } } // no schedule yet is OK
        }
    }

    fun loadUserBlocks() {
        viewModelScope.launch {
            _uiState.update { it.copy(blocksLoading = true) }
            scheduleRepository.getBlocks(todayDate())
                .onSuccess { dtos ->
                    val blocks = dtos.mapIndexed { i, d ->
                        ScheduleBlockInput(id = i, name = d.name, startMin = d.startMin, endMin = d.endMin, repeatDays = d.repeatDays ?: 1)
                    }
                    _uiState.update { it.copy(blocksLoading = false, userBlocks = blocks) }
                }
                .onFailure { _uiState.update { it.copy(blocksLoading = false) } }
        }
    }

    /** Persist blocks to the backend, then update local state. onDone runs on completion. */
    fun saveUserBlocks(blocks: List<ScheduleBlockInput>, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val dtos = blocks.map { UserScheduleBlockDto(name = it.name, startMin = it.startMin ?: 0, endMin = it.endMin, repeatDays = it.repeatDays) }
            scheduleRepository.saveBlocks(todayDate(), dtos)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, userBlocks = blocks) }
                    onDone(true)
                }
                .onFailure { e ->
                    // Keep blocks locally even if the network save failed, so the UI isn't empty.
                    _uiState.update { it.copy(isSaving = false, userBlocks = blocks, error = e.message) }
                    onDone(false)
                }
        }
    }

    fun generateSchedule(availableHours: Float = 6f, peakHours: String = "19:00-22:00") {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null) }
            scheduleRepository.generateSchedule(availableHours, peakHours)
                .onSuccess { s -> _uiState.update { it.copy(isGenerating = false, schedule = s) } }
                .onFailure { e -> _uiState.update { it.copy(isGenerating = false, error = e.message) } }
        }
    }

    private fun todayDate(): String {
        val c = Calendar.getInstance()
        return "%04d-%02d-%02d".format(
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH),
        )
    }
}
