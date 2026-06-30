"""
Multi-Agent Orchestrator — Phase 3

Coordinates four agents in sequence:
  1. HabitAgent  → refresh productivity patterns
  2. RiskAgent   → score all pending tasks
  3. PlannerAgent→ generate full week schedule (incorporating calendar + habits)
  4. CallAgent   → schedule calls for tasks at risk this week

Agents communicate through DynamoDB — not direct calls —
so each can run independently (or on separate Lambdas in production).
"""

from datetime import datetime, timezone, timedelta
from app.services import dynamodb_service as db
from app.services.risk_service import compute_risk_score, save_risk_score
from app.services.habit_service import analyze_habits, save_habit_record
from app.services.schedule_service import generate_daily_schedule, save_schedule
from app.services.dynamodb_service import get_dynamodb


def run_weekly_plan(user_id: str, calendar_events: list[dict] | None = None) -> dict:
    """
    Full autonomous weekly planning pipeline.
    Called manually via POST /planning/week or automatically on Monday 6 AM (Lambda).
    """
    now = datetime.now(timezone.utc)
    results = {
        "userId": user_id,
        "triggeredAt": now.isoformat(),
        "habit_insights": None,
        "risk_scores": [],
        "schedules_generated": [],
        "critical_tasks": [],
        "calls_scheduled": [],
    }

    # ── Step 1: Habit Agent ───────────────────────────────────────────────
    all_tasks = db.get_tasks_for_user(user_id)
    from app.services.sprint_service import get_sprints_for_user
    sprints = get_sprints_for_user(user_id)

    insights = analyze_habits(all_tasks, sprints)
    today_str = now.strftime("%Y-%m-%d")
    save_habit_record(get_dynamodb(), user_id, today_str, insights)
    results["habit_insights"] = insights

    peak_hours = insights.get("peakProductivityHours", ["19:00-22:00"])[0]

    # ── Step 2: Risk Agent ────────────────────────────────────────────────
    active_tasks = [t for t in all_tasks if t.get("status") != "completed"]
    risk_results = []
    critical_tasks = []

    for task in active_tasks:
        score_data = compute_risk_score(task, all_tasks)
        save_risk_score(user_id, task["taskId"], score_data)
        risk_results.append({"taskId": task["taskId"], "title": task["title"], **score_data})
        if score_data["risk_score"] >= 90:
            critical_tasks.append(task)

    results["risk_scores"] = risk_results
    results["critical_tasks"] = [t["title"] for t in critical_tasks]

    # ── Step 3: Planner Agent ─────────────────────────────────────────────
    # Build busy blocks from calendar events
    busy_note = ""
    if calendar_events:
        busy_note = f"Avoid scheduling during: {[e.get('summary', '') for e in calendar_events[:5]]}"

    for day_offset in range(7):
        target_date = (now + timedelta(days=day_offset)).strftime("%Y-%m-%d")
        schedule = generate_daily_schedule(
            tasks=active_tasks,
            peak_hours=peak_hours,
            available_hours=6.0,
            target_date=target_date,
        )
        save_schedule(get_dynamodb(), user_id, target_date, schedule, "weekly")
        results["schedules_generated"].append(target_date)

    # ── Step 4: Call Agent ────────────────────────────────────────────────
    # Log which tasks are queued for accountability calls (actual call
    # initiated via POST /calls/trigger/{taskId} or risk monitor)
    results["calls_scheduled"] = [
        {"taskId": t["taskId"], "title": t["title"]}
        for t in critical_tasks
    ]

    return results


def check_and_escalate(user_id: str, user_phone: str | None) -> list[dict]:
    """
    Called periodically (every 30 min in production via EventBridge).
    Checks risk scores and triggers Level 4 calls for tasks > 90.
    Returns list of actions taken.
    """
    actions = []
    all_tasks = db.get_tasks_for_user(user_id)
    active_tasks = [t for t in all_tasks if t.get("status") != "completed"]

    for task in active_tasks:
        score_data = compute_risk_score(task, all_tasks)
        risk = score_data["risk_score"]

        if risk >= 90 and user_phone:
            # Check if a call was already made recently to avoid spam
            from app.services.call_service import get_calls_for_user
            recent_calls = get_calls_for_user(user_id)
            task_calls = [c for c in recent_calls if c.get("taskId") == task["taskId"]]

            # Only call if no call in the last 2 hours
            should_call = True
            if task_calls:
                last_call_time_str = task_calls[0].get("timestamp", "")
                try:
                    if last_call_time_str.endswith("Z"):
                        last_call_time_str = last_call_time_str[:-1] + "+00:00"
                    last_call_time = datetime.fromisoformat(last_call_time_str)
                    if (datetime.now(timezone.utc) - last_call_time).total_seconds() < 7200:
                        should_call = False
                except Exception:
                    pass

            if should_call:
                actions.append({
                    "action": "call_triggered",
                    "taskId": task["taskId"],
                    "title": task["title"],
                    "riskScore": risk,
                    "phone": user_phone,
                })

        elif risk >= 95:
            # Level 5 — auto-deploy emergency plan (no call needed)
            actions.append({
                "action": "emergency_auto_deploy",
                "taskId": task["taskId"],
                "title": task["title"],
                "riskScore": risk,
            })

    return actions
