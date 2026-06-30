from fastapi import APIRouter, Depends, HTTPException, status
from app.services.auth_service import get_current_user_id
from app.services import dynamodb_service as db
from app.services.risk_service import compute_risk_score, save_risk_score

router = APIRouter(prefix="/tasks", tags=["risk"])


@router.get("/risk/all")
async def get_all_risk_scores(user_id: str = Depends(get_current_user_id)):
    """Compute and return risk scores for all active tasks. Read-only — no DB writes."""
    all_tasks = db.get_tasks_for_user(user_id)
    active_tasks = [t for t in all_tasks if t.get("status") != "completed"]

    results = []
    for task in active_tasks:
        score_data = compute_risk_score(task, all_tasks)
        results.append({
            "taskId": task["taskId"],
            "title": task["title"],
            "deadline": task.get("deadline"),
            **score_data,
        })

    results.sort(key=lambda r: r["risk_score"], reverse=True)
    return results


@router.get("/{task_id}/risk")
async def get_task_risk(
    task_id: str,
    user_id: str = Depends(get_current_user_id),
):
    task = db.get_task(user_id, task_id)
    if not task:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Task not found.")

    all_tasks = db.get_tasks_for_user(user_id)
    score_data = compute_risk_score(task, all_tasks)
    # Only persist on explicit single-task risk check, not bulk poll
    save_risk_score(user_id, task_id, score_data)

    return {
        "taskId": task_id,
        "title": task["title"],
        "deadline": task.get("deadline"),
        **score_data,
    }
