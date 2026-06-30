package com.focusguard.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusguard.app.data.api.DailyMetricsDto
import com.focusguard.app.data.api.HabitInsightsDto
import com.focusguard.app.data.api.WeeklyMetricsDto
import com.focusguard.app.data.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val daily: DailyMetricsDto? = null,
    val weekly: WeeklyMetricsDto? = null,
    val habits: HabitInsightsDto? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedTab: Int = 0,  // 0=Daily, 1=Weekly
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()

    init { loadAll() }

    fun selectTab(tab: Int) = _uiState.update { it.copy(selectedTab = tab) }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            // Load daily + weekly in parallel
            val dailyDeferred  = async { dashboardRepository.getDailyMetrics() }
            val weeklyDeferred = async { dashboardRepository.getWeeklyMetrics() }
            val habitsDeferred = async { dashboardRepository.getHabitInsights() }

            val daily  = dailyDeferred.await().getOrNull()
            val weekly = weeklyDeferred.await().getOrNull()
            val habits = habitsDeferred.await().getOrNull()

            _uiState.update { it.copy(isLoading = false, daily = daily, weekly = weekly, habits = habits) }
        }
    }
}
