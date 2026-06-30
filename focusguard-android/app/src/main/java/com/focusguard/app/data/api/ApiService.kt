package com.focusguard.app.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.*

// ═══════════════════════════════════════════════════════════════════════════
// REQUEST BODIES
// ═══════════════════════════════════════════════════════════════════════════

@JsonClass(generateAdapter = true)
data class RegisterRequest(val name: String, val email: String, val password: String, val phone: String? = null)

@JsonClass(generateAdapter = true)
data class LoginRequest(val email: String, val password: String)

@JsonClass(generateAdapter = true)
data class CreateTaskRequest(@Json(name = "raw_text") val rawText: String)

@JsonClass(generateAdapter = true)
data class UpdateTaskRequest(
    val title: String? = null,
    val status: String? = null,
    @Json(name = "effort_hours") val effortHours: Float? = null,
    val deadline: String? = null,
)

@JsonClass(generateAdapter = true)
data class GenerateScheduleRequest(
    val date: String? = null,
    @Json(name = "available_hours") val availableHours: Float = 6.0f,
    @Json(name = "peak_hours") val peakHours: String = "19:00-22:00",
)

@JsonClass(generateAdapter = true)
data class EmergencyPlanRequest(@Json(name = "critical_task_id") val criticalTaskId: String)

@JsonClass(generateAdapter = true)
data class UserScheduleBlockDto(
    val name: String,
    val startMin: Int,
    val endMin: Int? = null,
    @Json(name = "repeat_days") val repeatDays: Int? = 1,
)

@JsonClass(generateAdapter = true)
data class SaveBlocksRequest(
    val date: String,
    val blocks: List<UserScheduleBlockDto>,
)

@JsonClass(generateAdapter = true)
data class UserBlocksResponseDto(
    val date: String,
    val blocks: List<UserScheduleBlockDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class StartSprintRequest(
    @Json(name = "task_id") val taskId: String,
    @Json(name = "duration_hours") val durationHours: Float = 2.0f,
)

@JsonClass(generateAdapter = true)
data class CheckpointRequest(@Json(name = "progress_made") val progressMade: Boolean)

@JsonClass(generateAdapter = true)
data class EndSprintRequest(@Json(name = "completion_percent") val completionPercent: Int = 100)

@JsonClass(generateAdapter = true)
data class SubTaskDto(
    val id: String = "",
    val title: String,
    val done: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class AddSubTaskRequest(val title: String)

@JsonClass(generateAdapter = true)
data class UpdateSubTaskRequest(val done: Boolean)

@JsonClass(generateAdapter = true)
data class CheckInRequest(val note: String = "")

@JsonClass(generateAdapter = true)
data class CheckInStatusDto(
    @Json(name = "checked_in_today") val checkedInToday: Boolean,
    val date: String = "",
    @Json(name = "active_tasks") val activeTasks: Int = 0,
)

// ═══════════════════════════════════════════════════════════════════════════
// RESPONSE DTOs
// ═══════════════════════════════════════════════════════════════════════════

@JsonClass(generateAdapter = true)
data class AuthResponseDto(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "token_type") val tokenType: String,
    @Json(name = "user_id") val userId: String,
    val name: String = "",
    val email: String = "",
)

@JsonClass(generateAdapter = true)
data class TaskDto(
    @Json(name = "task_id") val taskId: String,
    @Json(name = "user_id") val userId: String,
    val title: String,
    val deadline: String,
    @Json(name = "effort_hours") val effortHours: Float,
    val category: String,
    @Json(name = "priority_score") val priorityScore: Int,
    val status: String,
    @Json(name = "priority_rank_reason") val priorityRankReason: String = "",
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "updated_at") val updatedAt: String = "",
    @Json(name = "sub_tasks") val subTasks: List<SubTaskDto> = emptyList(),
    @Json(name = "checkin_streak") val checkinStreak: Int = 0,
)

@JsonClass(generateAdapter = true)
data class RiskScoreDto(
    @Json(name = "task_id") val taskId: String = "",
    val title: String = "",
    val deadline: String = "",
    @Json(name = "risk_score") val riskScore: Int,
    val level: String,
    val factors: Map<String, Float> = emptyMap(),
)

@JsonClass(generateAdapter = true)
data class ScheduleBlockDto(
    @Json(name = "startTime") val startTime: String,
    @Json(name = "endTime") val endTime: String,
    @Json(name = "taskId") val taskId: String,
    @Json(name = "taskTitle") val taskTitle: String,
    @Json(name = "sessionType") val sessionType: String,
)

@JsonClass(generateAdapter = true)
data class ScheduleDto(
    val date: String,
    val schedule: List<ScheduleBlockDto>,
    val unscheduled: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class SprintDto(
    @Json(name = "sprintId") val sprintId: String,
    @Json(name = "taskId") val taskId: String,
    @Json(name = "startTime") val startTime: String,
    @Json(name = "endTime") val endTime: String? = null,
    @Json(name = "completionPercent") val completionPercent: Int = 0,
    val status: String = "active",
    val escalated: Boolean = false,
    @Json(name = "escalationRequired") val escalationRequired: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class HabitInsightsDto(
    @Json(name = "peakProductivityHours") val peakProductivityHours: List<String>,
    @Json(name = "avgEffortUnderestimation") val avgEffortUnderestimation: Float,
    @Json(name = "consistencyScore") val consistencyScore: Int,
    val recommendations: List<String>,
    @Json(name = "dataInsufficient") val dataInsufficient: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class DailyMetricsDto(
    val date: String,
    @Json(name = "tasksCompleted") val tasksCompleted: Int,
    @Json(name = "focusHours") val focusHours: Float,
    @Json(name = "deadlinesSaved") val deadlinesSaved: Int,
    @Json(name = "sprintSuccessRate") val sprintSuccessRate: Float,
    @Json(name = "productivityScore") val productivityScore: Int,
    @Json(name = "totalTasks") val totalTasks: Int,
    @Json(name = "atRiskCount") val atRiskCount: Int,
)

@JsonClass(generateAdapter = true)
data class WeeklyMetricsDto(
    @Json(name = "weekStart") val weekStart: String,
    @Json(name = "weeklyFocusHours") val weeklyFocusHours: Float,
    @Json(name = "dailyCompletions") val dailyCompletions: Map<String, Int>,
    @Json(name = "bestDay") val bestDay: String,
    @Json(name = "missedDeadlines") val missedDeadlines: Int,
    @Json(name = "nearMissesRecovered") val nearMissesRecovered: Int,
)

// ═══════════════════════════════════════════════════════════════════════════
// RETROFIT INTERFACE
// ═══════════════════════════════════════════════════════════════════════════

interface FocusGuardApiService {

    // ── Auth (Phase 1) ────────────────────────────────────────────────────
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponseDto>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponseDto>

    // ── Tasks (Phase 1) ───────────────────────────────────────────────────
    @POST("tasks")
    suspend fun createTask(@Header("Authorization") token: String, @Body request: CreateTaskRequest): Response<TaskDto>

    @POST("tasks/voice")
    suspend fun createVoiceTask(@Header("Authorization") token: String, @Body request: CreateTaskRequest): Response<TaskDto>

    @GET("tasks")
    suspend fun getTasks(@Header("Authorization") token: String): Response<List<TaskDto>>

    @GET("tasks/{id}")
    suspend fun getTask(@Header("Authorization") token: String, @Path("id") taskId: String): Response<TaskDto>

    @PUT("tasks/{id}")
    suspend fun updateTask(@Header("Authorization") token: String, @Path("id") taskId: String, @Body request: UpdateTaskRequest): Response<TaskDto>

    @DELETE("tasks/{id}")
    suspend fun deleteTask(@Header("Authorization") token: String, @Path("id") taskId: String): Response<Unit>

    // ── Sub-tasks ─────────────────────────────────────────────────────────────
    @POST("tasks/{id}/subtasks")
    suspend fun addSubTask(@Header("Authorization") token: String, @Path("id") taskId: String, @Body request: AddSubTaskRequest): Response<TaskDto>

    @PUT("tasks/{id}/subtasks/{subId}")
    suspend fun updateSubTask(@Header("Authorization") token: String, @Path("id") taskId: String, @Path("subId") subId: String, @Body request: UpdateSubTaskRequest): Response<TaskDto>

    @DELETE("tasks/{id}/subtasks/{subId}")
    suspend fun deleteSubTask(@Header("Authorization") token: String, @Path("id") taskId: String, @Path("subId") subId: String): Response<TaskDto>

    // ── Daily check-in ────────────────────────────────────────────────────────
    @POST("tasks/checkin")
    suspend fun dailyCheckIn(@Header("Authorization") token: String, @Body request: CheckInRequest): Response<Map<String, Any>>

    @GET("tasks/checkin/status")
    suspend fun checkInStatus(@Header("Authorization") token: String): Response<CheckInStatusDto>

    // ── Risk (Phase 2) ────────────────────────────────────────────────────
    @GET("tasks/risk/all")
    suspend fun getAllRiskScores(@Header("Authorization") token: String): Response<List<RiskScoreDto>>

    @GET("tasks/{id}/risk")
    suspend fun getTaskRisk(@Header("Authorization") token: String, @Path("id") taskId: String): Response<RiskScoreDto>

    // ── Schedule (Phase 2) ────────────────────────────────────────────────
    @POST("schedule/generate")
    suspend fun generateSchedule(@Header("Authorization") token: String, @Body request: GenerateScheduleRequest): Response<ScheduleDto>

    @POST("schedule/emergency")
    suspend fun generateEmergencyPlan(@Header("Authorization") token: String, @Body request: EmergencyPlanRequest): Response<Map<String, Any>>

    @GET("schedule/today")
    suspend fun getTodaySchedule(@Header("Authorization") token: String): Response<ScheduleDto>

    @POST("schedule/blocks")
    suspend fun saveScheduleBlocks(@Header("Authorization") token: String, @Body request: SaveBlocksRequest): Response<Map<String, Any>>

    @GET("schedule/blocks")
    suspend fun getScheduleBlocks(@Header("Authorization") token: String, @Query("date") date: String): Response<UserBlocksResponseDto>

    // ── Sprints (Phase 2) ─────────────────────────────────────────────────
    @POST("sprints")
    suspend fun startSprint(@Header("Authorization") token: String, @Body request: StartSprintRequest): Response<SprintDto>

    @PUT("sprints/{id}/checkpoint")
    suspend fun logCheckpoint(@Header("Authorization") token: String, @Path("id") sprintId: String, @Body request: CheckpointRequest): Response<SprintDto>

    @PUT("sprints/{id}/end")
    suspend fun endSprint(@Header("Authorization") token: String, @Path("id") sprintId: String, @Body request: EndSprintRequest): Response<SprintDto>

    @GET("sprints")
    suspend fun getSprints(@Header("Authorization") token: String): Response<List<SprintDto>>

    @GET("sprints/{id}")
    suspend fun getSprint(@Header("Authorization") token: String, @Path("id") sprintId: String): Response<SprintDto>

    // ── Habits (Phase 2) ──────────────────────────────────────────────────
    @GET("habits/insights")
    suspend fun getHabitInsights(@Header("Authorization") token: String): Response<HabitInsightsDto>

    // ── Dashboard (Phase 2) ───────────────────────────────────────────────
    @GET("dashboard/daily")
    suspend fun getDailyMetrics(@Header("Authorization") token: String): Response<DailyMetricsDto>

    @GET("dashboard/weekly")
    suspend fun getWeeklyMetrics(@Header("Authorization") token: String): Response<WeeklyMetricsDto>

    // ── Calls (Phase 3) ───────────────────────────────────────────────────
    @POST("calls/trigger/{taskId}")
    suspend fun triggerCall(@Header("Authorization") token: String, @Path("taskId") taskId: String): Response<Map<String, Any>>

    @GET("calls/history")
    suspend fun getCallHistory(@Header("Authorization") token: String): Response<List<Map<String, Any>>>

    // ── Planning (Phase 3) ────────────────────────────────────────────────
    @POST("planning/week")
    suspend fun planWeek(@Header("Authorization") token: String): Response<Map<String, Any>>

    @GET("planning/conflicts")
    suspend fun getConflicts(@Header("Authorization") token: String): Response<Map<String, Any>>
}
