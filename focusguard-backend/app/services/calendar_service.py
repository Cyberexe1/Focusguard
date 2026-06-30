"""
FocusGuard Built-in Calendar — Phase 3

No OAuth. No Google API. No third-party keys.

Architecture:
- User events stored in focusguard_calendar_events DynamoDB table
- FocusGuard schedule blocks written as events automatically
- Export to .ics (RFC 5545) — works with Google Calendar, Outlook, Apple Calendar
- Conflict detection runs locally against stored events

Event types:
  - user_event   : manually added by the user (class, meeting, etc.)
  - focus_block  : written by FocusGuard schedule generator
  - deadline     : auto-created when a task deadline is set
"""

import uuid
from datetime import datetime, timezone, timedelta
from typing import Optional
import boto3
from app.config import settings


# ── DynamoDB helpers ──────────────────────────────────────────────────────────

def _db():
    return boto3.resource("dynamodb", region_name=settings.aws_region)


def _table():
    return _db().Table("focusguard_calendar_events")


# ── CRUD ──────────────────────────────────────────────────────────────────────

def create_event(
    user_id: str,
    title: str,
    start_iso: str,
    end_iso: str,
    event_type: str = "user_event",   # user_event | focus_block | deadline
    description: str = "",
    task_id: str = "",
    recurrence: str = "none",          # none | daily | weekly
) -> dict:
    event_id = str(uuid.uuid4())
    now = datetime.now(timezone.utc).isoformat()
    event = {
        "userId":      user_id,
        "eventId":     event_id,
        "title":       title,
        "startTime":   start_iso,
        "endTime":     end_iso,
        "eventType":   event_type,
        "description": description,
        "taskId":      task_id,
        "recurrence":  recurrence,
        "createdAt":   now,
        "updatedAt":   now,
    }
    _table().put_item(Item=event)
    return event


def get_events(user_id: str, date_from: str = "", date_to: str = "") -> list[dict]:
    """Return all events for a user, optionally filtered by date range."""
    table = _table()
    response = table.query(
        KeyConditionExpression=boto3.dynamodb.conditions.Key("userId").eq(user_id)
    )
    events = response.get("Items", [])

    if date_from or date_to:
        filtered = []
        for e in events:
            start = e.get("startTime", "")
            # Compare just the datetime prefix — handles both date-only and full ISO
            if date_from and start[:len(date_from)] < date_from[:len(start)]:
                continue
            if date_to and start[:len(date_to)] > date_to[:len(start)]:
                continue
            filtered.append(e)
        return sorted(filtered, key=lambda e: e.get("startTime", ""))

    return sorted(events, key=lambda e: e.get("startTime", ""))


def get_event(user_id: str, event_id: str) -> Optional[dict]:
    response = _table().get_item(Key={"userId": user_id, "eventId": event_id})
    return response.get("Item")


def update_event(user_id: str, event_id: str, updates: dict) -> Optional[dict]:
    table = _table()
    updates["updatedAt"] = datetime.now(timezone.utc).isoformat()
    expr_parts, expr_values, expr_names = [], {}, {}
    for k, v in updates.items():
        expr_names[f"#f_{k}"] = k
        expr_values[f":v_{k}"] = v
        expr_parts.append(f"#f_{k} = :v_{k}")
    response = table.update_item(
        Key={"userId": user_id, "eventId": event_id},
        UpdateExpression="SET " + ", ".join(expr_parts),
        ExpressionAttributeNames=expr_names,
        ExpressionAttributeValues=expr_values,
        ReturnValues="ALL_NEW",
    )
    return response.get("Attributes")


def delete_event(user_id: str, event_id: str) -> None:
    _table().delete_item(Key={"userId": user_id, "eventId": event_id})


def delete_focus_blocks(user_id: str, date: str) -> int:
    """Remove all FocusGuard-generated blocks for a given date (before regenerating)."""
    events = get_events(user_id, date_from=date, date_to=date + "T23:59:59")
    deleted = 0
    for e in events:
        if e.get("eventType") == "focus_block":
            delete_event(user_id, e["eventId"])
            deleted += 1
    return deleted


# ── Conflict detection ────────────────────────────────────────────────────────

def detect_conflicts(user_id: str, schedule_blocks: list[dict], date: str) -> list[dict]:
    """
    Cross-reference proposed schedule blocks against stored user events.
    Returns list of conflicts.
    """
    user_events = get_events(
        user_id,
        date_from=f"{date}T00:00:00",
        date_to=f"{date}T23:59:59",
    )
    # Only check user_event types (not focus_blocks from FocusGuard itself)
    blocking_events = [e for e in user_events if e.get("eventType") == "user_event"]

    conflicts = []
    for block in schedule_blocks:
        if block.get("sessionType") == "break":
            continue
        b_start = f"{date}T{block.get('startTime', '00:00')}:00"
        b_end   = f"{date}T{block.get('endTime',   '00:00')}:00"

        for event in blocking_events:
            e_start = event.get("startTime", "")
            e_end   = event.get("endTime",   "")
            if _overlaps(b_start, b_end, e_start, e_end):
                conflicts.append({
                    "scheduleBlock":  block,
                    "conflictingEvent": {
                        "eventId": event["eventId"],
                        "title":   event["title"],
                        "start":   e_start,
                        "end":     e_end,
                    },
                })
    return conflicts


def _overlaps(s1: str, e1: str, s2: str, e2: str) -> bool:
    try:
        return s1 < e2 and s2 < e1
    except Exception:
        return False


# ── Write schedule blocks to calendar ────────────────────────────────────────

def write_schedule_to_calendar(user_id: str, schedule: dict, date: str) -> int:
    """
    Write a FocusGuard-generated schedule into the built-in calendar.
    Deletes old focus blocks for the day first to avoid duplicates.
    Returns number of events written.
    """
    delete_focus_blocks(user_id, date)

    blocks = schedule.get("schedule", [])
    written = 0
    for block in blocks:
        if block.get("sessionType") == "break":
            continue
        start_iso = f"{date}T{block.get('startTime', '08:00')}:00"
        end_iso   = f"{date}T{block.get('endTime',   '09:00')}:00"
        create_event(
            user_id=user_id,
            title=block.get("taskTitle", "Focus Session"),
            start_iso=start_iso,
            end_iso=end_iso,
            event_type="focus_block",
            description=f"FocusGuard auto-scheduled · {block.get('sessionType', 'deep_work')}",
            task_id=block.get("taskId", ""),
        )
        written += 1
    return written


# ── ICS export ────────────────────────────────────────────────────────────────

def export_ics(user_id: str, days_ahead: int = 7) -> str:
    """
    Generate RFC 5545 .ics file content for all events in the next N days.
    The user can download this and import into Google Calendar / Outlook / Apple Calendar.
    """
    now = datetime.now(timezone.utc)
    date_from = now.strftime("%Y-%m-%dT%H:%M:%S")
    date_to   = (now + timedelta(days=days_ahead)).strftime("%Y-%m-%dT%H:%M:%S")
    events = get_events(user_id, date_from=date_from, date_to=date_to)

    lines = [
        "BEGIN:VCALENDAR",
        "VERSION:2.0",
        "PRODID:-//FocusGuard AI//EN",
        "CALSCALE:GREGORIAN",
        "METHOD:PUBLISH",
        f"X-WR-CALNAME:FocusGuard AI Schedule",
        f"X-WR-CALDESC:Exported from FocusGuard AI productivity app",
    ]

    for event in events:
        start_dt = _parse_dt(event.get("startTime", ""))
        end_dt   = _parse_dt(event.get("endTime",   ""))
        uid = event.get("eventId", str(uuid.uuid4()))
        created = event.get("createdAt", now.isoformat())

        lines += [
            "BEGIN:VEVENT",
            f"UID:{uid}@focusguard.ai",
            f"DTSTAMP:{_to_ics_dt(now)}",
            f"DTSTART:{_to_ics_dt(start_dt)}",
            f"DTEND:{_to_ics_dt(end_dt)}",
            f"SUMMARY:{_ics_escape(event.get('title', 'FocusGuard Event'))}",
            f"DESCRIPTION:{_ics_escape(event.get('description', ''))}",
            f"CATEGORIES:{event.get('eventType', 'focus_block').upper()}",
            "END:VEVENT",
        ]

    lines.append("END:VCALENDAR")
    return "\r\n".join(lines)


def _parse_dt(iso_str: str) -> datetime:
    try:
        if iso_str.endswith("Z"):
            iso_str = iso_str[:-1] + "+00:00"
        dt = datetime.fromisoformat(iso_str)
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=timezone.utc)
        return dt
    except Exception:
        return datetime.now(timezone.utc)


def _to_ics_dt(dt: datetime) -> str:
    """Convert datetime to ICS format: 20240622T190000Z"""
    return dt.strftime("%Y%m%dT%H%M%SZ")


def _ics_escape(text: str) -> str:
    """Escape special characters per RFC 5545."""
    return text.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n")
