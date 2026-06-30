package com.focusguard.app.data.repository

import com.focusguard.app.data.api.EmergencyPlanRequest
import com.focusguard.app.data.api.FocusGuardApiService
import com.focusguard.app.data.api.GenerateScheduleRequest
import com.focusguard.app.data.api.SaveBlocksRequest
import com.focusguard.app.data.api.ScheduleDto
import com.focusguard.app.data.api.UserScheduleBlockDto
import com.focusguard.app.data.local.SessionDataStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepository @Inject constructor(
    private val api: FocusGuardApiService,
    private val sessionDataStore: SessionDataStore,
) {
    private suspend fun bearer(): String {
        val session = sessionDataStore.session.first() ?: throw IllegalStateException("Not logged in")
        return "Bearer ${session.accessToken}"
    }

    suspend fun generateSchedule(availableHours: Float = 6f, peakHours: String = "19:00-22:00"): Result<ScheduleDto> =
        runCatching {
            val r = api.generateSchedule(bearer(), GenerateScheduleRequest(availableHours = availableHours, peakHours = peakHours))
            r.body() ?: error("Empty response (${r.code()})")
        }

    suspend fun generateEmergencyPlan(taskId: String): Result<Map<String, Any>> =
        runCatching {
            val r = api.generateEmergencyPlan(bearer(), EmergencyPlanRequest(taskId))
            r.body() ?: error("Empty response (${r.code()})")
        }

    suspend fun getTodaySchedule(): Result<ScheduleDto> =
        runCatching {
            val r = api.getTodaySchedule(bearer())
            r.body() ?: error("No schedule found (${r.code()})")
        }

    suspend fun saveBlocks(date: String, blocks: List<UserScheduleBlockDto>): Result<Unit> =
        runCatching {
            val r = api.saveScheduleBlocks(bearer(), SaveBlocksRequest(date, blocks))
            if (!r.isSuccessful) error("Save failed (${r.code()})")
        }

    suspend fun getBlocks(date: String): Result<List<UserScheduleBlockDto>> =
        runCatching {
            val r = api.getScheduleBlocks(bearer(), date)
            r.body()?.blocks ?: error("No blocks (${r.code()})")
        }
}
