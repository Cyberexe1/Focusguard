package com.focusguard.app.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.*

// ── Request / Response models ─────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class CreateCalendarEventRequest(
    val title: String,
    @Json(name = "start_time") val startTime: String,   // ISO 8601
    @Json(name = "end_time") val endTime: String,       // ISO 8601
    @Json(name = "event_type") val eventType: String = "user_event",
    val description: String = "",
    val recurrence: String = "none",
)

@JsonClass(generateAdapter = true)
data class UpdateCalendarEventRequest(
    val title: String? = null,
    @Json(name = "start_time") val startTime: String? = null,
    @Json(name = "end_time") val endTime: String? = null,
    val description: String? = null,
)

@JsonClass(generateAdapter = true)
data class CalendarEventDto(
    @Json(name = "eventId") val eventId: String,
    @Json(name = "userId") val userId: String,
    val title: String,
    @Json(name = "startTime") val startTime: String,
    @Json(name = "endTime") val endTime: String,
    @Json(name = "eventType") val eventType: String,   // user_event | focus_block | deadline
    val description: String = "",
    @Json(name = "taskId") val taskId: String = "",
    val recurrence: String = "none",
    @Json(name = "createdAt") val createdAt: String = "",
)

@JsonClass(generateAdapter = true)
data class CalendarEventsResponse(
    @Json(name = "eventCount") val eventCount: Int,
    @Json(name = "dateFrom") val dateFrom: String,
    @Json(name = "dateTo") val dateTo: String,
    val events: List<CalendarEventDto>,
)

@JsonClass(generateAdapter = true)
data class SyncScheduleRequest(
    @Json(name = "schedule_blocks") val scheduleBlocks: List<Map<String, String>>,
    val date: String? = null,
)

@JsonClass(generateAdapter = true)
data class SyncResponse(
    val date: String,
    @Json(name = "blocksWritten") val blocksWritten: Int,
    @Json(name = "conflictsDetected") val conflictsDetected: Int,
    val message: String,
)

// ── Retrofit interface ────────────────────────────────────────────────────────

interface CalendarApiService {

    @POST("calendar/events")
    suspend fun createEvent(
        @Header("Authorization") token: String,
        @Body request: CreateCalendarEventRequest,
    ): Response<CalendarEventDto>

    @GET("calendar/events")
    suspend fun getEvents(
        @Header("Authorization") token: String,
        @Query("date_from") dateFrom: String = "",
        @Query("date_to") dateTo: String = "",
    ): Response<CalendarEventsResponse>

    @PUT("calendar/events/{id}")
    suspend fun updateEvent(
        @Header("Authorization") token: String,
        @Path("id") eventId: String,
        @Body request: UpdateCalendarEventRequest,
    ): Response<CalendarEventDto>

    @DELETE("calendar/events/{id}")
    suspend fun deleteEvent(
        @Header("Authorization") token: String,
        @Path("id") eventId: String,
    ): Response<Unit>

    @POST("calendar/sync")
    suspend fun syncSchedule(
        @Header("Authorization") token: String,
        @Body request: SyncScheduleRequest,
    ): Response<SyncResponse>

    @GET("calendar/export.ics")
    suspend fun exportIcs(
        @Header("Authorization") token: String,
        @Query("days") days: Int = 7,
    ): Response<String>
}
