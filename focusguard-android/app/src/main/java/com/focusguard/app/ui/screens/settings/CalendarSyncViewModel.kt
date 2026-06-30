package com.focusguard.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusguard.app.data.api.CalendarEventDto
import com.focusguard.app.data.repository.CalendarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

data class CalendarUiState(
    val events: List<CalendarEventDto> = emptyList(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val error: String? = null,
    val syncMessage: String? = null,
    val showAddEventSheet: Boolean = false,
)

@HiltViewModel
class CalendarSyncViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState = _uiState.asStateFlow()

    init { loadEvents() }

    fun loadEvents() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            calendarRepository.getUpcomingEvents(7)
                .onSuccess { events ->
                    _uiState.update { it.copy(isLoading = false, events = events) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun addEvent(title: String, startIso: String, endIso: String, description: String = "") {
        viewModelScope.launch {
            calendarRepository.addEvent(title, startIso, endIso, description = description)
                .onSuccess { loadEvents() }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            calendarRepository.deleteEvent(eventId)
                .onSuccess { loadEvents() }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun showAddEvent() = _uiState.update { it.copy(showAddEventSheet = true) }
    fun dismissAddEvent() = _uiState.update { it.copy(showAddEventSheet = false) }

    fun clearMessages() = _uiState.update { it.copy(error = null, syncMessage = null) }
}
