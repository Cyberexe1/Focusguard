package com.focusguard.app.data.repository

import com.focusguard.app.data.api.DailyMetricsDto
import com.focusguard.app.data.api.FocusGuardApiService
import com.focusguard.app.data.api.HabitInsightsDto
import com.focusguard.app.data.api.RiskScoreDto
import com.focusguard.app.data.api.WeeklyMetricsDto
import com.focusguard.app.data.local.SessionDataStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepository @Inject constructor(
    private val api: FocusGuardApiService,
    private val sessionDataStore: SessionDataStore,
) {
    private suspend fun bearer(): String {
        val session = sessionDataStore.session.first() ?: throw IllegalStateException("Not logged in")
        return "Bearer ${session.accessToken}"
    }

    suspend fun getDailyMetrics(): Result<DailyMetricsDto> =
        runCatching {
            val r = api.getDailyMetrics(bearer())
            r.body() ?: error("Failed (${r.code()})")
        }

    suspend fun getWeeklyMetrics(): Result<WeeklyMetricsDto> =
        runCatching {
            val r = api.getWeeklyMetrics(bearer())
            r.body() ?: error("Failed (${r.code()})")
        }

    suspend fun getHabitInsights(): Result<HabitInsightsDto> =
        runCatching {
            val r = api.getHabitInsights(bearer())
            r.body() ?: error("Failed (${r.code()})")
        }

    suspend fun getAllRiskScores(): Result<List<RiskScoreDto>> =
        runCatching {
            val r = api.getAllRiskScores(bearer())
            r.body() ?: emptyList()
        }
}
