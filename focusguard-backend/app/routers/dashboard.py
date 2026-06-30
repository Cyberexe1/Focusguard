from datetime import datetime, timezone, timedelta
from fastapi import APIRouter, Depends
from app.services.auth_service import get_current_user_id
from app.services import dynamodb_service as db
from app.services import sprint_service
from app.services.risk_service import compute_risk_score

router = APIRouter(prefix="/dashboard", tags=["dashboard"])


@router.get("/daily")
async def daily_metrics(user_id: str = Depends(get_current_user_id)):
    all_tasks = db.get_tasks_for_user(user_id)
    sprints = sprint_service.get_sprints_for_user(user_id)
    today = datetime.now(timezone.utc).date()

    # Tasks completed today
    completed_today = [
        t for t in all_tasks
        if t.get("status") == "completed" and _is_today(t.get("updatedAt", ""), today)
    ]

    # Focus hours today (sum of completed sprint durations today)
    focus_hours = 0.0
    sprint_success_count = 0
    sprint_total_today = 0
    for sprint in sprints:
        if _is_today(sprint.get("startTime", ""), today):
            sprint_total_today += 1
            duration = float(sprint.get("durationHours", 2))
            pct = int(sprint.get("completionPercent", 0))
            if pct >= 80:
                sprint_success_count += 1
                focus_hours += duration * (pct / 100)

    # Deadlines saved today = tasks whose risk was > 75 but got completed today
    risk_scores = [compute_risk_score(t, all_tasks) for t in all_tasks]
    at_risk_completed = sum(
        1 for t, r in zip(all_tasks, risk_scores)
        if t.get("status") == "completed"
        and r["risk_score"] >= 75
        and _is_today(t.get("updatedAt", ""), today)
    )

    # Productivity score — weighted average of completion rate + sprint success
    total = len(all_tasks)
    completed_total = len([t for t in all_tasks if t.get("status") == "completed"])
    completion_rate = (completed_total / total * 100) if total > 0 else 0
    sprint_rate = (sprint_success_count / sprint_total_today * 100) if sprint_total_today > 0 else 0
    productivity_score = int(completion_rate * 0.6 + sprint_rate * 0.4)

    return {
        "date": today.isoformat(),
        "tasksCompleted": len(completed_today),
        "focusHours": round(focus_hours, 1),
        "deadlinesSaved": at_risk_completed,
        "sprintSuccessRate": round(sprint_rate, 1),
        "productivityScore": min(100, productivity_score),
        "totalTasks": total,
        "atRiskCount": sum(1 for r in risk_scores if r["risk_score"] >= 75),
    }


@router.get("/weekly")
async def weekly_metrics(user_id: str = Depends(get_current_user_id)):
    all_tasks = db.get_tasks_for_user(user_id)
    sprints = sprint_service.get_sprints_for_user(user_id)
    today = datetime.now(timezone.utc).date()
    week_start = today - timedelta(days=today.weekday())

    # Per-day breakdown
    daily_completions = {}
    for t in all_tasks:
        if t.get("status") == "completed":
            updated = t.get("updatedAt", "")
            try:
                if updated.endswith("Z"):
                    updated = updated[:-1] + "+00:00"
                d = datetime.fromisoformat(updated).date()
                if d >= week_start:
                    day_name = d.strftime("%A")
                    daily_completions[day_name] = daily_completions.get(day_name, 0) + 1
            except Exception:
                pass

    best_day = max(daily_completions, key=daily_completions.get) if daily_completions else "N/A"

    # Weekly focus hours
    weekly_focus = sum(
        float(s.get("durationHours", 2)) * (int(s.get("completionPercent", 0)) / 100)
        for s in sprints
        if _is_this_week(s.get("startTime", ""), week_start)
    )

    # Missed deadlines this week
    missed = sum(
        1 for t in all_tasks
        if t.get("status") != "completed" and _deadline_passed(t.get("deadline", ""))
    )

    return {
        "weekStart": week_start.isoformat(),
        "weeklyFocusHours": round(weekly_focus, 1),
        "dailyCompletions": daily_completions,
        "bestDay": best_day,
        "missedDeadlines": missed,
        "nearMissesRecovered": sum(1 for t in all_tasks if t.get("status") == "completed"),
    }


# ── Helpers ───────────────────────────────────────────────────────────────────

def _is_today(iso_str: str, today) -> bool:
    try:
        if not iso_str:
            return False
        if iso_str.endswith("Z"):
            iso_str = iso_str[:-1] + "+00:00"
        return datetime.fromisoformat(iso_str).date() == today
    except Exception:
        return False


def _is_this_week(iso_str: str, week_start) -> bool:
    try:
        if not iso_str:
            return False
        if iso_str.endswith("Z"):
            iso_str = iso_str[:-1] + "+00:00"
        return datetime.fromisoformat(iso_str).date() >= week_start
    except Exception:
        return False


def _deadline_passed(iso_str: str) -> bool:
    try:
        if not iso_str:
            return False
        if iso_str.endswith("Z"):
            iso_str = iso_str[:-1] + "+00:00"
        dl = datetime.fromisoformat(iso_str)
        if dl.tzinfo is None:
            dl = dl.replace(tzinfo=timezone.utc)
        return dl < datetime.now(timezone.utc)
    except Exception:
        return False
