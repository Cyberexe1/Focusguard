"""
Planner Agent — uses Amazon Nova Lite via Bedrock Converse API
to generate daily schedules and emergency recovery plans.
"""

import json
import re
from decimal import Decimal
from datetime import datetime, timezone
from app.config import settings


def get_dynamodb():
    import boto3
    return boto3.resource("dynamodb", region_name=settings.aws_region)
from app.services.bedrock_service import get_bedrock_client


def generate_daily_schedule(
    tasks: list[dict],
    peak_hours: str = "19:00-22:00",
    available_hours: float = 6.0,
    target_date: str | None = None,
) -> dict:
    """
    Call Nova Lite to generate an optimized time-blocked daily plan.
    Falls back to rule-based scheduling if Bedrock is unavailable.
    """
    if not target_date:
        target_date = datetime.now(timezone.utc).strftime("%Y-%m-%d")

    # Only include pending/in_progress tasks
    active_tasks = [
        t for t in tasks
        if t.get("status", "pending") != "completed"
    ]

    if not active_tasks:
        return {
            "date": target_date,
            "schedule": [],
            "unscheduled": [],
            "warnings": ["No active tasks to schedule."],
        }

    # Sort by priority descending for the prompt
    active_tasks_sorted = sorted(
        active_tasks,
        key=lambda t: int(t.get("priorityScore", 0)),
        reverse=True,
    )

    tasks_summary = "\n".join([
        f"- [{t.get('priorityScore', 0)}pts] {t['title']} | "
        f"deadline: {t.get('deadline', 'unknown')} | "
        f"effort: {t.get('effortHours', 2)}h | "
        f"id: {t['taskId']}"
        for t in active_tasks_sorted[:8]   # cap at 8 tasks per prompt
    ])

    system_prompt = "You are a productivity scheduling AI. Return only valid JSON, no markdown."

    user_prompt = f"""Generate a time-blocked daily schedule for {target_date}.

Tasks (sorted by priority):
{tasks_summary}

Constraints:
- Available hours today: {available_hours}h
- User's peak productive hours: {peak_hours}
- Schedule high-priority tasks during peak hours
- Add 10-minute buffers between sessions
- Max 2-hour continuous work blocks

Return exactly this JSON (no extra text):
{{
  "date": "{target_date}",
  "schedule": [
    {{
      "startTime": "HH:MM",
      "endTime": "HH:MM",
      "taskId": "uuid or 'break'",
      "taskTitle": "string",
      "sessionType": "deep_work | review | break"
    }}
  ],
  "unscheduled": ["taskId1"],
  "warnings": ["string"]
}}"""

    try:
        from app.services.bedrock_service import generate_schedule_prompt
        raw = generate_schedule_prompt(tasks_summary, peak_hours, available_hours, target_date)
        return json.loads(raw)
    except Exception as e:
        print(f"[schedule_service] Bedrock failed ({e}), using fallback scheduler.")
        return _fallback_schedule(active_tasks_sorted, target_date, available_hours)


def generate_emergency_plan(
    critical_task: dict,
    other_tasks: list[dict],
) -> dict:
    """
    Compressed recovery plan when risk > 75.
    Front-loads the critical task and postpones everything else.
    """
    system_prompt = "You are an emergency productivity planner. Return only valid JSON."

    user_prompt = f"""EMERGENCY: User is at high risk of missing a deadline.

Critical task: {critical_task['title']}
Deadline: {critical_task.get('deadline', 'unknown')}
Effort remaining: {critical_task.get('effortHours', 2)}h

Other tasks to postpone: {[t['title'] for t in other_tasks[:5]]}

Create a compressed recovery plan. Return exactly this JSON:
{{
  "critical_task_id": "{critical_task['taskId']}",
  "plan": [
    {{
      "phase": "Core Work",
      "startOffset": "NOW",
      "duration": "2h",
      "description": "what to focus on"
    }}
  ],
  "postponed_task_ids": ["id1", "id2"],
  "warning_message": "string shown to user"
}}"""

    try:
        from app.services.bedrock_service import generate_schedule_prompt
        # Emergency plan via same schedule prompt but with tight constraint note
        constraint = f"EMERGENCY: focus ONLY on {critical_task['title']} — compress into {float(critical_task.get('effortHours', 4))}h"
        tasks_summary_str = f"- [CRITICAL] {critical_task['title']} | deadline: {critical_task.get('deadline')} | effort: {critical_task.get('effortHours', 4)}h"
        raw = generate_schedule_prompt(tasks_summary_str + f"\nNote: {constraint}", "NOW", float(critical_task.get("effortHours", 4)), "TODAY")
        schedule_data = json.loads(raw)
        return {
            "critical_task_id": critical_task["taskId"],
            "plan": [{"phase": b.get("taskTitle", "Work"), "startOffset": b.get("startTime", "NOW"), "duration": f"{b.get('endTime','')}h", "description": b.get("sessionType","")} for b in schedule_data.get("schedule", [])],
            "postponed_task_ids": [t["taskId"] for t in other_tasks],
            "warning_message": f"⚠️ Emergency mode. Focus only on: {critical_task['title']}",
        }
    except Exception as e:
        print(f"[schedule_service] Emergency plan fallback ({e})")
        effort = float(critical_task.get("effortHours", 4))
        return {
            "critical_task_id": critical_task["taskId"],
            "plan": [
                {"phase": "Core Features", "startOffset": "NOW", "duration": f"{effort * 0.5:.1f}h", "description": "Focus on essential deliverables only."},
                {"phase": "Testing & Fix", "startOffset": f"+{effort * 0.5:.1f}h", "duration": f"{effort * 0.3:.1f}h", "description": "Quick test and fix critical bugs."},
                {"phase": "Submit", "startOffset": f"+{effort * 0.8:.1f}h", "duration": "30m", "description": "Final review and submission."},
            ],
            "postponed_task_ids": [t["taskId"] for t in other_tasks],
            "warning_message": f"⚠️ Emergency mode active. All other tasks postponed. Focus only on: {critical_task['title']}",
        }


def save_schedule(dynamodb_resource, user_id: str, date: str, schedule_data: dict, plan_type: str = "daily") -> None:
    from datetime import datetime, timezone
    # DynamoDB cannot store float — convert to Decimal recursively
    schedule_clean = _floats_to_decimal(schedule_data)
    table = dynamodb_resource.Table("focusguard_schedules")
    table.put_item(Item={
        "userId": user_id,
        "date": date,
        "schedule": schedule_clean,
        "type": plan_type,
        "createdAt": datetime.now(timezone.utc).isoformat(),
    })


def get_schedule(dynamodb_resource, user_id: str, date: str) -> dict | None:
    table = dynamodb_resource.Table("focusguard_schedules")
    response = table.get_item(Key={"userId": user_id, "date": date})
    return response.get("Item")


def _floats_to_decimal(obj):
    """Recursively convert float → Decimal for DynamoDB compatibility."""
    if isinstance(obj, float):
        return Decimal(str(obj))
    if isinstance(obj, dict):
        return {k: _floats_to_decimal(v) for k, v in obj.items()}
    if isinstance(obj, list):
        return [_floats_to_decimal(i) for i in obj]
    return obj


# ── Rule-based fallback ───────────────────────────────────────────────────────

def _fallback_schedule(tasks: list[dict], date: str, available_hours: float) -> dict:
    """Simple sequential scheduler when Bedrock is unavailable."""
    schedule = []
    unscheduled = []

    # Start scheduling at 08:00
    current_hour = 8
    current_minute = 0
    remaining_hours = available_hours

    for task in tasks:
        effort = min(float(task.get("effortHours", 2)), 2.0)   # cap at 2h per block
        if remaining_hours < effort:
            unscheduled.append(task["taskId"])
            continue

        start = f"{current_hour:02d}:{current_minute:02d}"
        total_minutes = current_hour * 60 + current_minute + int(effort * 60)
        end_h = total_minutes // 60
        end_m = total_minutes % 60
        end = f"{end_h:02d}:{end_m:02d}"

        schedule.append({
            "startTime": start,
            "endTime": end,
            "taskId": task["taskId"],
            "taskTitle": task["title"],
            "sessionType": "deep_work",
        })

        # 10-minute buffer
        current_hour = end_h
        current_minute = end_m + 10
        if current_minute >= 60:
            current_hour += 1
            current_minute -= 60
        remaining_hours -= effort

    return {
        "date": date,
        "schedule": schedule,
        "unscheduled": unscheduled,
        "warnings": [] if not unscheduled else [f"{len(unscheduled)} task(s) could not fit in today's schedule."],
    }
