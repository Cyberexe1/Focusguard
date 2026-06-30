"""
Built-in Calendar Router — Phase 3

No OAuth. No Google API. No third-party keys required.

Features:
- CRUD for user events (classes, meetings, blocked time)
- Auto-write FocusGuard schedule blocks to calendar
- Conflict detection against stored events
- Export to .ics — importable by Google Calendar, Outlook, Apple Calendar
"""

from datetime import datetime, timezone, timedelta
from fastapi import APIRouter, Depends, HTTPException, Query, status
from fastapi.responses import PlainTextResponse
from pydantic import BaseModel
from typing import Optional
from app.services.auth_service import get_current_user_id
from app.services import calendar_service

router = APIRouter(prefix="/calendar", tags=["calendar"])


# ── Request models ────────────────────────────────────────────────────────────

class CreateEventRequest(BaseModel):
    title: str
    start_time: str              # ISO 8601: 2024-12-22T19:00:00
    end_time: str                # ISO 8601: 2024-12-22T21:00:00
    event_type: str = "user_event"   # user_event | deadline
    description: str = ""
    recurrence: str = "none"         # none | daily | weekly


class UpdateEventRequest(BaseModel):
    title: Optional[str] = None
    start_time: Optional[str] = None
    end_time: Optional[str] = None
    description: Optional[str] = None


class SyncScheduleRequest(BaseModel):
    schedule_blocks: list[dict]
    date: Optional[str] = None


# ── Event CRUD ────────────────────────────────────────────────────────────────

@router.post("/events", status_code=status.HTTP_201_CREATED)
async def create_event(
    body: CreateEventRequest,
    user_id: str = Depends(get_current_user_id),
):
    """Add a user event (class, meeting, blocked time) to the built-in calendar."""
    event = calendar_service.create_event(
        user_id=user_id,
        title=body.title,
        start_iso=body.start_time,
        end_iso=body.end_time,
        event_type=body.event_type,
        description=body.description,
        recurrence=body.recurrence,
    )
    return event


@router.get("/events")
async def get_events(
    user_id: str = Depends(get_current_user_id),
    date_from: str = Query(default=""),
    date_to: str = Query(default=""),
):
    """
    Get all calendar events for the user.
    Optionally filter by date range: ?date_from=2024-12-22&date_to=2024-12-29
    """
    if not date_from:
        date_from = ""   # No default — return all future and past events
    if not date_to:
        date_to = ""

    events = calendar_service.get_events(user_id, date_from=date_from, date_to=date_to)
    return {
        "eventCount": len(events),
        "dateFrom": date_from,
        "dateTo": date_to,
        "events": events,
    }


@router.get("/events/{event_id}")
async def get_event(
    event_id: str,
    user_id: str = Depends(get_current_user_id),
):
    event = calendar_service.get_event(user_id, event_id)
    if not event:
        raise HTTPException(status_code=404, detail="Event not found.")
    return event


@router.put("/events/{event_id}")
async def update_event(
    event_id: str,
    body: UpdateEventRequest,
    user_id: str = Depends(get_current_user_id),
):
    updates = {k: v for k, v in body.model_dump().items() if v is not None}
    # Map snake_case to stored field names
    if "start_time" in updates:
        updates["startTime"] = updates.pop("start_time")
    if "end_time" in updates:
        updates["endTime"] = updates.pop("end_time")

    updated = calendar_service.update_event(user_id, event_id, updates)
    if not updated:
        raise HTTPException(status_code=404, detail="Event not found.")
    return updated


@router.delete("/events/{event_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_event(
    event_id: str,
    user_id: str = Depends(get_current_user_id),
):
    event = calendar_service.get_event(user_id, event_id)
    if not event:
        raise HTTPException(status_code=404, detail="Event not found.")
    calendar_service.delete_event(user_id, event_id)


# ── Schedule sync ─────────────────────────────────────────────────────────────

@router.post("/sync")
async def sync_schedule(
    body: SyncScheduleRequest,
    user_id: str = Depends(get_current_user_id),
):
    """
    Write FocusGuard schedule blocks to the built-in calendar.
    Detects conflicts with existing user events before writing.
    Old focus blocks for the day are replaced.
    """
    target_date = body.date or datetime.now(timezone.utc).strftime("%Y-%m-%d")

    # Check conflicts against stored user events
    conflicts = calendar_service.detect_conflicts(user_id, body.schedule_blocks, target_date)

    # Write non-conflicting blocks
    written = calendar_service.write_schedule_to_calendar(
        user_id=user_id,
        schedule={"schedule": body.schedule_blocks},
        date=target_date,
    )

    return {
        "date": target_date,
        "blocksWritten": written,
        "conflictsDetected": len(conflicts),
        "conflicts": conflicts,
        "message": f"{written} focus block(s) added to calendar for {target_date}.",
    }


# ── Conflict check (read-only) ────────────────────────────────────────────────

@router.post("/conflicts")
async def check_conflicts(
    body: SyncScheduleRequest,
    user_id: str = Depends(get_current_user_id),
):
    """Check for conflicts without writing anything to the calendar."""
    target_date = body.date or datetime.now(timezone.utc).strftime("%Y-%m-%d")
    conflicts = calendar_service.detect_conflicts(user_id, body.schedule_blocks, target_date)
    return {
        "date": target_date,
        "conflictCount": len(conflicts),
        "conflicts": conflicts,
    }


# ── ICS export ────────────────────────────────────────────────────────────────

@router.get("/export.ics", response_class=PlainTextResponse)
async def export_ics(
    user_id: str = Depends(get_current_user_id),
    days: int = Query(default=7, ge=1, le=30),
):
    """
    Download a standard .ics calendar file.
    Import into Google Calendar, Outlook, or Apple Calendar.

    Google Calendar: Settings → Import & Export → Import → select the .ics file
    Outlook: File → Open & Export → Import/Export → Import an iCalendar
    Apple Calendar: File → Import
    """
    ics_content = calendar_service.export_ics(user_id, days_ahead=days)
    return PlainTextResponse(
        content=ics_content,
        media_type="text/calendar; charset=utf-8",
        headers={"Content-Disposition": 'attachment; filename="focusguard_schedule.ics"'},
    )
