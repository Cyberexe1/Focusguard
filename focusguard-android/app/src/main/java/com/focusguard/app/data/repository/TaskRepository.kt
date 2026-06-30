package com.focusguard.app.data.repository

import com.focusguard.app.data.api.AddSubTaskRequest
import com.focusguard.app.data.api.CheckInRequest
import com.focusguard.app.data.api.CheckInStatusDto
import com.focusguard.app.data.api.CreateTaskRequest
import com.focusguard.app.data.api.FocusGuardApiService
import com.focusguard.app.data.api.TaskDto
import com.focusguard.app.data.api.UpdateSubTaskRequest
import com.focusguard.app.data.api.UpdateTaskRequest
import com.focusguard.app.data.local.SessionDataStore
import com.focusguard.app.domain.model.SubTask
import com.focusguard.app.domain.model.Task
import com.focusguard.app.domain.model.TaskStatus
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

sealed class TaskResult<out T> {
    data class Success<T>(val data: T) : TaskResult<T>()
    data class Error(val message: String) : TaskResult<Nothing>()
}

@Singleton
class TaskRepository @Inject constructor(
    private val api: FocusGuardApiService,
    private val sessionDataStore: SessionDataStore,
) {
    private suspend fun bearerToken(): String {
        val session = sessionDataStore.session.first()
            ?: throw IllegalStateException("No active session")
        return "Bearer ${session.accessToken}"
    }

    suspend fun getTasks(): TaskResult<List<Task>> {
        return try {
            val response = api.getTasks(bearerToken())
            if (response.isSuccessful) {
                TaskResult.Success(response.body()!!.map { it.toDomain() })
            } else {
                TaskResult.Error("Failed to load tasks (${response.code()})")
            }
        } catch (e: Exception) {
            TaskResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    suspend fun getTask(taskId: String): TaskResult<Task> = try {
        val response = api.getTask(bearerToken(), taskId)
        if (response.isSuccessful) {
            TaskResult.Success(response.body()!!.toDomain())
        } else {
            TaskResult.Error("Task not found")
        }
    } catch (e: Exception) {
        TaskResult.Error("Network error: ${e.localizedMessage}")
    }

    suspend fun createTask(rawText: String): TaskResult<Task> {
        return try {
            val response = api.createTask(
                token = bearerToken(),
                request = CreateTaskRequest(rawText = rawText),
            )
            if (response.isSuccessful) {
                TaskResult.Success(response.body()!!.toDomain())
            } else {
                TaskResult.Error("Failed to create task (${response.code()})")
            }
        } catch (e: Exception) {
            TaskResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    suspend fun createVoiceTask(transcript: String): TaskResult<Task> = try {
        val response = api.createVoiceTask(
            token = bearerToken(),
            request = CreateTaskRequest(rawText = transcript),
        )
        if (response.isSuccessful) {
            TaskResult.Success(response.body()!!.toDomain())
        } else {
            TaskResult.Error("Failed to create voice task (${response.code()})")
        }
    } catch (e: Exception) {
        TaskResult.Error("Network error: ${e.localizedMessage}")
    }

    suspend fun updateTaskStatus(taskId: String, status: TaskStatus): TaskResult<Task> = try {
        val response = api.updateTask(
            token = bearerToken(),
            taskId = taskId,
            request = UpdateTaskRequest(status = status.name),
        )
        if (response.isSuccessful) {
            TaskResult.Success(response.body()!!.toDomain())
        } else {
            TaskResult.Error("Failed to update task")
        }
    } catch (e: Exception) {
        TaskResult.Error("Network error: ${e.localizedMessage}")
    }

    suspend fun deleteTask(taskId: String): TaskResult<Unit> = try {
        val response = api.deleteTask(bearerToken(), taskId)
        if (response.isSuccessful) TaskResult.Success(Unit)
        else TaskResult.Error("Failed to delete task")
    } catch (e: Exception) {
        TaskResult.Error("Network error: ${e.localizedMessage}")
    }

    // ── Mapper ──────────────────────────────────────────────────────────────
    private fun TaskDto.toDomain() = Task(
        taskId = taskId,
        userId = userId,
        title = title,
        deadline = deadline,
        effortHours = effortHours,
        category = category,
        priorityScore = priorityScore,
        status = runCatching { TaskStatus.valueOf(status) }.getOrDefault(TaskStatus.pending),
        priorityRankReason = priorityRankReason,
        createdAt = createdAt,
        updatedAt = updatedAt,
        subTasks = subTasks.map { SubTask(id = it.id, title = it.title, done = it.done) },
        checkinStreak = checkinStreak,
    )

    // ── Sub-task operations ──────────────────────────────────────────────────

    suspend fun addSubTask(taskId: String, title: String): TaskResult<Task> = try {
        val response = api.addSubTask(bearerToken(), taskId, AddSubTaskRequest(title))
        if (response.isSuccessful) TaskResult.Success(response.body()!!.toDomain())
        else TaskResult.Error("Failed to add sub-task (${response.code()})")
    } catch (e: Exception) {
        TaskResult.Error("Network error: ${e.localizedMessage}")
    }

    suspend fun toggleSubTask(taskId: String, subId: String, done: Boolean): TaskResult<Task> = try {
        val response = api.updateSubTask(bearerToken(), taskId, subId, UpdateSubTaskRequest(done))
        if (response.isSuccessful) TaskResult.Success(response.body()!!.toDomain())
        else TaskResult.Error("Failed to update sub-task (${response.code()})")
    } catch (e: Exception) {
        TaskResult.Error("Network error: ${e.localizedMessage}")
    }

    suspend fun deleteSubTask(taskId: String, subId: String): TaskResult<Task> = try {
        val response = api.deleteSubTask(bearerToken(), taskId, subId)
        if (response.isSuccessful) TaskResult.Success(response.body()!!.toDomain())
        else TaskResult.Error("Failed to delete sub-task (${response.code()})")
    } catch (e: Exception) {
        TaskResult.Error("Network error: ${e.localizedMessage}")
    }

    // ── Daily check-in ───────────────────────────────────────────────────────

    suspend fun dailyCheckIn(note: String = ""): TaskResult<Unit> = try {
        val response = api.dailyCheckIn(bearerToken(), CheckInRequest(note))
        if (response.isSuccessful) TaskResult.Success(Unit)
        else TaskResult.Error("Check-in failed (${response.code()})")
    } catch (e: Exception) {
        TaskResult.Error("Network error: ${e.localizedMessage}")
    }

    suspend fun getCheckInStatus(): TaskResult<CheckInStatusDto> = try {
        val response = api.checkInStatus(bearerToken())
        if (response.isSuccessful) TaskResult.Success(response.body()!!)
        else TaskResult.Error("Failed (${response.code()})")
    } catch (e: Exception) {
        TaskResult.Error("Network error: ${e.localizedMessage}")
    }
}
