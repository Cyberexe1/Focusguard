import uuid
from datetime import datetime, timezone
from fastapi import APIRouter, Depends, HTTPException, status
from app.models import (
    CreateTaskRequest,
    UpdateTaskRequest,
    TaskResponse,
    AddSubTaskRequest,
    UpdateSubTaskRequest,
    CheckInRequest,
    SubTask,
)
from app.services import dynamodb_service as db
from app.services.bedrock_service import parse_and_score_task
from app.services.auth_service import get_current_user_id

router = APIRouter(prefix="/tasks", tags=["tasks"])


def _build_task_response(item: dict) -> TaskResponse:
    return TaskResponse(
        task_id=item["taskId"],
        user_id=item["userId"],
        title=item["title"],
        deadline=item["deadline"],
        effort_hours=float(item.get("effortHours", 0)),
        category=item.get("category", "Personal"),
        priority_score=int(item.get("priorityScore", 0)),
        priority_rank_reason=item.get("priorityRankReason", ""),
        status=item.get("status", "pending"),
        created_at=item.get("createdAt", ""),
        updated_at=item.get("updatedAt", ""),
        sub_tasks=item.get("subTasks", []),
        checkin_streak=int(item.get("checkinStreak", 0)),
    )


@router.post("", response_model=TaskResponse, status_code=status.HTTP_201_CREATED)
async def create_task(
    body: CreateTaskRequest,
    user_id: str = Depends(get_current_user_id),
):
    """
    Accepts natural language task text.
    Sends to Bedrock for parsing → stores result in DynamoDB.
    """
    parsed = parse_and_score_task(body.raw_text)

    task_id = str(uuid.uuid4())
    now = datetime.now(timezone.utc).isoformat()

    task_record = {
        "userId": user_id,
        "taskId": task_id,
        "title": parsed["title"],
        "deadline": parsed["deadline_iso"],
        "effortHours": str(parsed["effort_hours"]),   # DynamoDB stores as string
        "category": parsed["category"],
        "priorityScore": parsed["priority_score"],
        "priorityRankReason": parsed["priority_rank_reason"],
        "status": "pending",
        "createdAt": now,
        "updatedAt": now,
        "rawText": body.raw_text,
    }
    db.create_task(task_record)

    return _build_task_response(task_record)


@router.post("/voice", response_model=TaskResponse, status_code=status.HTTP_201_CREATED)
async def create_voice_task(
    body: CreateTaskRequest,
    user_id: str = Depends(get_current_user_id),
):
    """
    Same as POST /tasks — voice transcript is the raw_text.
    Kept as a separate endpoint to match Phase 1 spec.
    """
    return await create_task(body, user_id)


@router.get("", response_model=list[TaskResponse])
async def get_tasks(user_id: str = Depends(get_current_user_id)):
    """Returns all tasks for the authenticated user, sorted by priority_score desc."""
    items = db.get_tasks_for_user(user_id)
    return [_build_task_response(item) for item in items]


# ── Daily check-in endpoints (MUST be before /{task_id} to avoid route conflict) ──

@router.post("/checkin", status_code=status.HTTP_200_OK)
async def daily_checkin(
    body: CheckInRequest,
    user_id: str = Depends(get_current_user_id),
):
    """User confirms daily progress. Increments streak on each active task."""
    today = datetime.now(timezone.utc).strftime("%Y-%m-%d")
    tasks = db.get_tasks_for_user(user_id)
    active = [t for t in tasks if t.get("status") != "completed"]

    updated_count = 0
    for task in active:
        if task.get("lastCheckin") == today:
            continue
        streak = int(task.get("checkinStreak", 0)) + 1
        db.update_task(task["userId"], task["taskId"], {
            "lastCheckin": today,
            "checkinStreak": streak,
            "checkinNote": body.note,       # persist the note
            "updatedAt": datetime.now(timezone.utc).isoformat(),
        })
        updated_count += 1

    return {"checked_in": True, "date": today, "tasks_updated": updated_count, "note": body.note}


@router.get("/checkin/status", status_code=status.HTTP_200_OK)
async def checkin_status(user_id: str = Depends(get_current_user_id)):
    """Returns whether the user has checked in today."""
    today = datetime.now(timezone.utc).strftime("%Y-%m-%d")
    tasks = db.get_tasks_for_user(user_id)
    active = [t for t in tasks if t.get("status") != "completed"]
    checked_in = any(t.get("lastCheckin") == today for t in active)
    return {"checked_in_today": checked_in, "date": today, "active_tasks": len(active)}


# ── /{task_id} routes ─────────────────────────────────────────────────────────

@router.get("/{task_id}", response_model=TaskResponse)
async def get_task(
    task_id: str,
    user_id: str = Depends(get_current_user_id),
):
    item = db.get_task(user_id, task_id)
    if not item:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Task not found.")
    return _build_task_response(item)


@router.put("/{task_id}", response_model=TaskResponse)
async def update_task(
    task_id: str,
    body: UpdateTaskRequest,
    user_id: str = Depends(get_current_user_id),
):
    # Verify task belongs to user
    existing = db.get_task(user_id, task_id)
    if not existing:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Task not found.")

    updates: dict = {}
    if body.title is not None:
        updates["title"] = body.title
    if body.status is not None:
        updates["status"] = body.status.value
    if body.effort_hours is not None:
        updates["effortHours"] = str(body.effort_hours)
    if body.deadline is not None:
        updates["deadline"] = body.deadline

    updates["updatedAt"] = datetime.now(timezone.utc).isoformat()

    updated = db.update_task(user_id, task_id, updates)
    if not updated:
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="Update failed.")

    return _build_task_response(updated)


@router.delete("/{task_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_task(
    task_id: str,
    user_id: str = Depends(get_current_user_id),
):
    existing = db.get_task(user_id, task_id)
    if not existing:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Task not found.")
    db.delete_task(user_id, task_id)


# ── Sub-task endpoints ────────────────────────────────────────────────────────

@router.post("/{task_id}/subtasks", response_model=TaskResponse)
async def add_sub_task(
    task_id: str,
    body: AddSubTaskRequest,
    user_id: str = Depends(get_current_user_id),
):
    """Add a sub-task to an existing task."""
    item = db.get_task(user_id, task_id)
    if not item:
        raise HTTPException(status_code=404, detail="Task not found.")

    sub_tasks = list(item.get("subTasks", []))
    new_sub = {"id": str(uuid.uuid4()), "title": body.title, "done": False}
    sub_tasks.append(new_sub)

    updated = db.update_task(user_id, task_id, {
        "subTasks": sub_tasks,
        "updatedAt": datetime.now(timezone.utc).isoformat(),
    })
    return _build_task_response(updated)


@router.put("/{task_id}/subtasks/{sub_id}", response_model=TaskResponse)
async def update_sub_task(
    task_id: str,
    sub_id: str,
    body: UpdateSubTaskRequest,
    user_id: str = Depends(get_current_user_id),
):
    """Toggle a sub-task done/undone."""
    item = db.get_task(user_id, task_id)
    if not item:
        raise HTTPException(status_code=404, detail="Task not found.")

    sub_tasks = list(item.get("subTasks", []))
    found = False
    for st in sub_tasks:
        if st.get("id") == sub_id:
            st["done"] = body.done
            found = True
            break
    if not found:
        raise HTTPException(status_code=404, detail="Sub-task not found.")

    # Recalculate progress-based priority boost
    total = len(sub_tasks)
    done_count = sum(1 for st in sub_tasks if st.get("done"))
    completion_pct = (done_count / total * 100) if total > 0 else 0
    # Lower risk when more sub-tasks done — reduce priority score proportionally
    base_score = int(item.get("priorityScore", 50))
    adjusted_score = max(5, int(base_score * (1 - completion_pct / 200)))  # max 50% reduction

    updated = db.update_task(user_id, task_id, {
        "subTasks": sub_tasks,
        "priorityScore": adjusted_score,
        "updatedAt": datetime.now(timezone.utc).isoformat(),
    })
    return _build_task_response(updated)


@router.delete("/{task_id}/subtasks/{sub_id}", response_model=TaskResponse)
async def delete_sub_task(
    task_id: str,
    sub_id: str,
    user_id: str = Depends(get_current_user_id),
):
    """Remove a sub-task."""
    item = db.get_task(user_id, task_id)
    if not item:
        raise HTTPException(status_code=404, detail="Task not found.")

    sub_tasks = [st for st in item.get("subTasks", []) if st.get("id") != sub_id]
    updated = db.update_task(user_id, task_id, {
        "subTasks": sub_tasks,
        "updatedAt": datetime.now(timezone.utc).isoformat(),
    })
    return _build_task_response(updated)
