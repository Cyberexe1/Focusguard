package com.focusguard.app.data.repository

import com.focusguard.app.data.api.CheckpointRequest
import com.focusguard.app.data.api.EndSprintRequest
import com.focusguard.app.data.api.FocusGuardApiService
import com.focusguard.app.data.api.SprintDto
import com.focusguard.app.data.api.StartSprintRequest
import com.focusguard.app.data.local.SessionDataStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SprintRepository @Inject constructor(
    private val api: FocusGuardApiService,
    private val sessionDataStore: SessionDataStore,
) {
    private suspend fun bearer(): String {
        val session = sessionDataStore.session.first() ?: throw IllegalStateException("Not logged in")
        return "Bearer ${session.accessToken}"
    }

    suspend fun startSprint(taskId: String, durationHours: Float = 2f): Result<SprintDto> =
        runCatching {
            val r = api.startSprint(bearer(), StartSprintRequest(taskId, durationHours))
            r.body() ?: error("Failed to start sprint (${r.code()})")
        }

    suspend fun logCheckpoint(sprintId: String, progressMade: Boolean): Result<SprintDto> =
        runCatching {
            val r = api.logCheckpoint(bearer(), sprintId, CheckpointRequest(progressMade))
            r.body() ?: error("Failed to log checkpoint (${r.code()})")
        }

    suspend fun endSprint(sprintId: String, completionPercent: Int): Result<SprintDto> =
        runCatching {
            val r = api.endSprint(bearer(), sprintId, EndSprintRequest(completionPercent))
            r.body() ?: error("Failed to end sprint (${r.code()})")
        }

    suspend fun getSprints(): Result<List<SprintDto>> =
        runCatching {
            val r = api.getSprints(bearer())
            r.body() ?: emptyList()
        }
}
