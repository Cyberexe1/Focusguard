package com.focusguard.app.data.repository

import com.focusguard.app.data.api.CalendarApiService
import com.focusguard.app.data.api.CalendarEventDto
import com.focusguard.app.data.api.CreateCalendarEventRequest
import com.focusguard.app.data.api.SyncResponse
import com.focusguard.app.data.api.SyncScheduleRequest
import com.focusguard.app.data.local.SessionDataStore
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRepository @Inject constructor(
    private val api: CalendarApiService,
    private val sessionDataStore: SessionDataStore,
) {
    private suspend fun bearer(): String {
        val session = sessionDataStore.session.first() ?: throw IllegalStateException("Not logged in")
        return "Bearer ${session.accessToken}"
    }

    suspend fun getUpcomingEvents(daysAhead: Int = 7): Result<List<CalendarEventDto>> =
        runCatching {
            val now = LocalDate.now(ZoneOffset.UTC)
            val from = now.format(DateTimeFormatter.ISO_LOCAL_DATE) + "T00:00:00"
            val to = now.plusDays(daysAhead.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE) + "T23:59:59"
            val r = api.getEvents(bearer(), dateFrom = from, dateTo = to)
            r.body()?.events ?: emptyList()
        }

    suspend fun addEvent(
        title: String,
        startIso: String,
        endIso: String,
        eventType: String = "user_event",
        description: String = "",
    ): Result<CalendarEventDto> =
        runCatching {
            val r = api.createEvent(
                bearer(),
                CreateCalendarEventRequest(
                    title = title,
                    startTime = startIso,
                    endTime = endIso,
                    eventType = eventType,
                    description = description,
                ),
            )
            r.body() ?: error("Failed to create event (${r.code()})")
        }

    suspend fun deleteEvent(eventId: String): Result<Unit> =
        runCatching {
            api.deleteEvent(bearer(), eventId)
        }

    suspend fun syncSchedule(blocks: List<Map<String, String>>, date: String): Result<SyncResponse> =
        runCatching {
            val r = api.syncSchedule(bearer(), SyncScheduleRequest(scheduleBlocks = blocks, date = date))
            r.body() ?: error("Sync failed (${r.code()})")
        }
}
