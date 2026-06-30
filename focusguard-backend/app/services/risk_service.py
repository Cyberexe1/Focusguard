"""
Deadline Risk Prediction Engine — Phase 2

Risk Score (0-100) =
  (Time Pressure Factor   × 0.40)
+ (Progress Deficit Factor× 0.30)
+ (Historical Miss Rate   × 0.20)
+ (Task Load Factor       × 0.10)
"""

from datetime import datetime, timezone
from decimal import Decimal
from decimal import Decimal
import boto3
from app.config import settings


def get_dynamodb():
    kwargs = {"region_name": settings.aws_region}
    return boto3.resource("dynamodb", **kwargs)


def compute_risk_score(
    task: dict,
    all_user_tasks: list[dict],
) -> dict:
    """
    Compute risk score for a single task.
    Returns dict with score + contributing factors.
    """
    now = datetime.now(timezone.utc)

    # ── 1. Time Pressure (weight 0.40) ──────────────────────────────────
    try:
        deadline_str = task.get("deadline", "")
        # Handle both naive and aware ISO strings
        if deadline_str.endswith("Z"):
            deadline_str = deadline_str[:-1] + "+00:00"
        deadline = datetime.fromisoformat(deadline_str)
        if deadline.tzinfo is None:
            deadline = deadline.replace(tzinfo=timezone.utc)

        hours_remaining = (deadline - now).total_seconds() / 3600
        effort_remaining = float(task.get("effortHours", 2))

        if hours_remaining <= 0:
            time_pressure = 1.0   # already past deadline
        elif effort_remaining <= 0:
            time_pressure = 0.0   # done
        else:
            time_pressure = min(1.0, effort_remaining / max(hours_remaining, 0.1))
    except Exception:
        time_pressure = 0.5

    # ── 2. Progress Deficit (weight 0.30) ────────────────────────────────
    status = task.get("status", "pending")
    if status == "completed":
        progress_deficit = 0.0
    elif status == "in_progress":
        progress_deficit = 0.4   # assume ~60% done when in_progress
    else:
        progress_deficit = 0.9   # pending = no progress

    # ── 3. Historical Miss Rate (weight 0.20) ────────────────────────────
    completed = [t for t in all_user_tasks if t.get("status") == "completed"]
    overdue_completed = 0
    for t in completed:
        try:
            dl_str = t.get("deadline", "")
            if dl_str.endswith("Z"):
                dl_str = dl_str[:-1] + "+00:00"
            updated_str = t.get("updatedAt", "")
            if updated_str.endswith("Z"):
                updated_str = updated_str[:-1] + "+00:00"
            dl = datetime.fromisoformat(dl_str)
            updated = datetime.fromisoformat(updated_str)
            if dl.tzinfo is None:
                dl = dl.replace(tzinfo=timezone.utc)
            if updated.tzinfo is None:
                updated = updated.replace(tzinfo=timezone.utc)
            if updated > dl:
                overdue_completed += 1
        except Exception:
            pass

    if len(completed) == 0:
        historical_miss_rate = 0.3   # no history → assume moderate risk
    else:
        historical_miss_rate = min(1.0, overdue_completed / len(completed))

    # ── 4. Task Load Factor (weight 0.10) ────────────────────────────────
    pending_tasks = [
        t for t in all_user_tasks
        if t.get("status") != "completed" and t.get("taskId") != task.get("taskId")
    ]
    task_load = min(1.0, len(pending_tasks) / 10)

    # ── Weighted score ────────────────────────────────────────────────────
    raw_score = (
        time_pressure        * 0.40
        + progress_deficit   * 0.30
        + historical_miss_rate * 0.20
        + task_load          * 0.10
    )
    risk_score = int(round(raw_score * 100))
    risk_score = max(0, min(100, risk_score))

    return {
        "risk_score": risk_score,
        "factors": {
            "time_pressure": round(time_pressure * 100, 1),
            "progress_deficit": round(progress_deficit * 100, 1),
            "historical_miss_rate": round(historical_miss_rate * 100, 1),
            "task_load": round(task_load * 100, 1),
        },
        "level": _risk_level(risk_score),
    }


def _risk_level(score: int) -> str:
    if score >= 90:
        return "critical"
    elif score >= 75:
        return "high"
    elif score >= 60:
        return "medium"
    else:
        return "low"


def save_risk_score(user_id: str, task_id: str, score_data: dict) -> None:
    """Persist risk score to focusguard_risk_history for trend tracking."""
    now = datetime.now(timezone.utc).isoformat()
    table = get_dynamodb().Table("focusguard_risk_history")
    table.put_item(Item={
        "userId": user_id,
        "taskIdTimestamp": f"{task_id}#{now}",
        "taskId": task_id,
        "riskScore": score_data["risk_score"],
        "level": score_data["level"],
        "factors": {k: Decimal(str(v)) for k, v in score_data["factors"].items()},
        "timestamp": now,
    })
