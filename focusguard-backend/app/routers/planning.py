"""
Autonomous Planning Router — Phase 3
"""

from fastapi import APIRouter, Depends, HTTPException
from app.services.auth_service import get_current_user_id
from app.services import dynamodb_service as db
from app.services.orchestrator import run_weekly_plan, check_and_escalate
from app.services.calendar_service import get_events

router = APIRouter(prefix="/planning", tags=["planning"])


@router.post("/week")
async def plan_week(user_id: str = Depends(get_current_user_id)):
    """
    Trigger full autonomous weekly planning:
    HabitAgent → RiskAgent → PlannerAgent → CallAgent
    """
    # Pull calendar events from built-in calendar (no OAuth needed)
    calendar_events = []
    try:
        calendar_events = get_events(user_id)
    except Exception as e:
        print(f"[planning] Could not fetch calendar events: {e}")

    result = run_weekly_plan(user_id, calendar_events)

    tasks_count = len(result.get("schedules_generated", []))
    critical_count = len(result.get("critical_tasks", []))

    return {
        **result,
        "summary": f"Week planned: {tasks_count} days scheduled, {critical_count} critical task(s) flagged.",
    }


@router.get("/conflicts")
async def list_conflicts(user_id: str = Depends(get_current_user_id)):
    """
    List scheduling conflicts between FocusGuard schedule and user's calendar events.
    """
    from datetime import datetime, timezone
    today = datetime.now(timezone.utc).strftime("%Y-%m-%d")
    from app.services.schedule_service import get_schedule, get_dynamodb
    schedule_record = get_schedule(get_dynamodb(), user_id, today)

    if not schedule_record:
        return {"conflicts": [], "message": "No schedule for today yet. Run POST /schedule/generate first."}

    schedule_data = schedule_record.get("schedule", {})
    blocks = schedule_data.get("schedule", []) if isinstance(schedule_data, dict) else []

    from app.services.calendar_service import detect_conflicts
    conflicts = detect_conflicts(user_id, blocks, today)

    return {"conflicts": conflicts, "conflictCount": len(conflicts)}


@router.post("/resolve/{conflict_id}")
async def resolve_conflict(
    conflict_id: str,
    user_id: str = Depends(get_current_user_id),
):
    """
    Placeholder for conflict resolution — regenerates schedule skipping conflicting slot.
    """
    return {
        "conflictId": conflict_id,
        "status": "resolved",
        "message": "Schedule will be regenerated avoiding this time slot. Call POST /schedule/generate to update.",
    }
