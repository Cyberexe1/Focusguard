from datetime import datetime, timezone
from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel
from typing import Optional
from app.services.auth_service import get_current_user_id
from app.services import dynamodb_service as db
from app.services import schedule_service
from app.services.risk_service import compute_risk_score

router = APIRouter(prefix="/schedule", tags=["schedule"])


class GenerateScheduleRequest(BaseModel):
    date: Optional[str] = None
    available_hours: float = 6.0
    peak_hours: str = "19:00-22:00"


class EmergencyPlanRequest(BaseModel):
    critical_task_id: str


class UserBlock(BaseModel):
    name: str
    startMin: int
    endMin: Optional[int] = None
    repeat_days: int = 1


class SaveBlocksRequest(BaseModel):
    date: str
    blocks: list[UserBlock]


@router.post("/generate")
async def generate_schedule(
    body: GenerateScheduleRequest,
    user_id: str = Depends(get_current_user_id),
):
    tasks = db.get_tasks_for_user(user_id)
    if not tasks:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="No tasks found. Add tasks first.")

    target_date = body.date or datetime.now(timezone.utc).strftime("%Y-%m-%d")

    result = schedule_service.generate_daily_schedule(
        tasks=tasks,
        peak_hours=body.peak_hours,
        available_hours=body.available_hours,
        target_date=target_date,
    )

    # Persist to DynamoDB
    from app.services.dynamodb_service import get_dynamodb
    schedule_service.save_schedule(get_dynamodb(), user_id, target_date, result, "daily")

    return result


@router.post("/emergency")
async def generate_emergency_plan(
    body: EmergencyPlanRequest,
    user_id: str = Depends(get_current_user_id),
):
    critical_task = db.get_task(user_id, body.critical_task_id)
    if not critical_task:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Task not found.")

    all_tasks = db.get_tasks_for_user(user_id)
    other_tasks = [t for t in all_tasks if t["taskId"] != body.critical_task_id and t.get("status") != "completed"]

    result = schedule_service.generate_emergency_plan(critical_task, other_tasks)

    # Mark the emergency plan in DynamoDB
    from app.services.dynamodb_service import get_dynamodb
    today = datetime.now(timezone.utc).strftime("%Y-%m-%d")
    schedule_service.save_schedule(get_dynamodb(), user_id, today, result, "emergency")

    return result


@router.get("/today")
async def get_today_schedule(user_id: str = Depends(get_current_user_id)):
    today = datetime.now(timezone.utc).strftime("%Y-%m-%d")
    from app.services.dynamodb_service import get_dynamodb
    record = schedule_service.get_schedule(get_dynamodb(), user_id, today)
    if not record:
        return {"message": "No schedule generated for today yet.", "date": today, "schedule": []}
    return record


# ── User-defined custom schedule blocks (manual timeline) ────────────────────

@router.post("/blocks")
async def save_user_blocks(
    body: SaveBlocksRequest,
    user_id: str = Depends(get_current_user_id),
):
    """Persist the user's manually-created schedule blocks for a given date."""
    from app.services.dynamodb_service import get_dynamodb
    table = get_dynamodb().Table("focusguard_schedules")
    table.put_item(Item={
        "userId": user_id,
        "date": f"userblocks#{body.date}",
        "type": "user_blocks",
        "blocks": [b.model_dump() for b in body.blocks],
        "createdAt": datetime.now(timezone.utc).isoformat(),
    })
    return {"saved": len(body.blocks), "date": body.date}


@router.get("/blocks")
async def get_user_blocks(
    date: str,
    user_id: str = Depends(get_current_user_id),
):
    """Return the user's manually-created schedule blocks for a given date."""
    from app.services.dynamodb_service import get_dynamodb
    table = get_dynamodb().Table("focusguard_schedules")
    resp = table.get_item(Key={"userId": user_id, "date": f"userblocks#{date}"})
    item = resp.get("Item")
    raw_blocks = item.get("blocks", []) if item else []
    blocks = []
    for b in raw_blocks:
        start = b.get("startMin")
        end = b.get("endMin")
        blocks.append({
            "name": b.get("name", ""),
            "startMin": int(start) if start is not None else 0,
            "endMin": int(end) if end is not None else None,
            "repeat_days": int(b.get("repeat_days", 1)),
        })
    return {"date": date, "blocks": blocks}
