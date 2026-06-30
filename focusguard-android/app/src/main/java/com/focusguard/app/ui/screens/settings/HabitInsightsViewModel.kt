package com.focusguard.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusguard.app.data.api.HabitInsightsDto
import com.focusguard.app.data.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HabitInsightsUiState(
    val insights: HabitInsightsDto? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class HabitInsightsViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitInsightsUiState())
    val uiState = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            dashboardRepository.getHabitInsights()
                .onSuccess { data -> _uiState.update { it.copy(isLoading = false, insights = data) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }
}
