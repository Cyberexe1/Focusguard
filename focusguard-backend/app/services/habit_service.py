"""
Habit Intelligence Agent — Phase 2
Analyzes behavioral patterns from tasks and sprints.
Uses Nova Lite for insight generation; falls back to rule-based analysis.
"""

import json
import re
from collections import Counter
from datetime import datetime, timezone
from app.config import settings
from app.services.bedrock_service import get_bedrock_client


def analyze_habits(
    tasks: list[dict],
    sprints: list[dict],
) -> dict:
    """
    Generate productivity insights from historical task + sprint data.
    """
    if len(tasks) < 3:
        return {
            "peakProductivityHours": ["19:00-22:00"],
            "avgEffortUnderestimation": 0.0,
            "consistencyScore": 50,
            "recommendations": [
                "Complete at least 3 tasks to start generating personal insights.",
                "Start a Focus Sprint to track your work patterns.",
            ],
            "dataInsufficient": True,
        }

    # ── Extract behavioral signals ────────────────────────────────────────
    creation_hours = []
    for task in tasks:
        try:
            created = task.get("createdAt", "")
            if created:
                if created.endswith("Z"):
                    created = created[:-1] + "+00:00"
                dt = datetime.fromisoformat(created)
                creation_hours.append(dt.hour)
        except Exception:
            pass

    sprint_hours = []
    for sprint in sprints:
        try:
            start = sprint.get("startTime", "")
            if start:
                if start.endswith("Z"):
                    start = start[:-1] + "+00:00"
                dt = datetime.fromisoformat(start)
                sprint_hours.append(dt.hour)
        except Exception:
            pass

    completed_tasks = [t for t in tasks if t.get("status") == "completed"]
    missed = sum(1 for t in completed_tasks if _was_late(t))
    miss_rate = missed / max(len(completed_tasks), 1)

    # Effort underestimation: compare effortHours to actual sprint durations
    underestimations = []
    for sprint in sprints:
        try:
            task_id = sprint.get("taskId")
            task = next((t for t in tasks if t.get("taskId") == task_id), None)
            if task and sprint.get("actualHours"):
                estimated = float(task.get("effortHours", 0))
                actual = float(sprint["actualHours"])
                if estimated > 0:
                    underestimations.append((actual - estimated) / estimated)
        except Exception:
            pass
    avg_underestimation = sum(underestimations) / len(underestimations) if underestimations else 0.0

    # Consistency score — based on sprint completion rate
    completed_sprints = [s for s in sprints if s.get("completionPercent", 0) >= 80]
    consistency = int((len(completed_sprints) / max(len(sprints), 1)) * 100) if sprints else 60

    # Peak hours detection
    all_active_hours = creation_hours + sprint_hours
    if all_active_hours:
        hour_counts = Counter(all_active_hours)
        top_hours = [h for h, _ in hour_counts.most_common(3)]
        top_hours.sort()
        # Group consecutive hours into ranges
        peak_range = f"{min(top_hours):02d}:00-{(max(top_hours) + 1):02d}:00"
    else:
        peak_range = "19:00-22:00"

    behavioral_data = {
        "peakHoursRange": peak_range,
        "avgUnderestimation": round(avg_underestimation, 2),
        "missRate": round(miss_rate, 2),
        "consistency": consistency,
        "totalTasks": len(tasks),
        "completedTasks": len(completed_tasks),
        "totalSprints": len(sprints),
    }

    # Try Bedrock for richer insights
    try:
        return _bedrock_insights(behavioral_data)
    except Exception as e:
        print(f"[habit_service] Bedrock fallback ({e})")
        return _rule_based_insights(behavioral_data)


def _bedrock_insights(data: dict) -> dict:
    from app.services.bedrock_service import generate_habit_insights_prompt
    raw = generate_habit_insights_prompt(data)
    return json.loads(raw)


def _rule_based_insights(data: dict) -> dict:
    recs = []
    if data["avgUnderestimation"] > 0.2:
        recs.append(f"Add {int(data['avgUnderestimation'] * 100)}% buffer to your effort estimates — you consistently underestimate task duration.")
    if data["missRate"] > 0.3:
        recs.append("Your deadline miss rate is high. Start tasks at least 2 days before the deadline.")
    if data["consistency"] < 60:
        recs.append("Use Focus Sprints more consistently — your sprint completion rate is below 60%.")
    recs.append(f"Schedule your most important tasks during {data['peakHoursRange']} — your historically most active window.")
    if data["totalSprints"] < 3:
        recs.append("Use Focus Sprint mode for tasks over 2 hours to stay on track.")

    return {
        "peakProductivityHours": [data["peakHoursRange"]],
        "avgEffortUnderestimation": data["avgUnderestimation"],
        "consistencyScore": data["consistency"],
        "recommendations": recs[:3],
    }


def _was_late(task: dict) -> bool:
    try:
        deadline_str = task.get("deadline", "")
        updated_str = task.get("updatedAt", "")
        if not deadline_str or not updated_str:
            return False
        if deadline_str.endswith("Z"):
            deadline_str = deadline_str[:-1] + "+00:00"
        if updated_str.endswith("Z"):
            updated_str = updated_str[:-1] + "+00:00"
        dl = datetime.fromisoformat(deadline_str)
        updated = datetime.fromisoformat(updated_str)
        if dl.tzinfo is None:
            dl = dl.replace(tzinfo=timezone.utc)
        if updated.tzinfo is None:
            updated = updated.replace(tzinfo=timezone.utc)
        return updated > dl
    except Exception:
        return False


def save_habit_record(dynamodb_resource, user_id: str, date: str, insights: dict) -> None:
    from decimal import Decimal

    def _fix(obj):
        if isinstance(obj, float):
            return Decimal(str(obj))
        if isinstance(obj, dict):
            return {k: _fix(v) for k, v in obj.items()}
        if isinstance(obj, list):
            return [_fix(i) for i in obj]
        return obj

    table = dynamodb_resource.Table("focusguard_habits")
    table.put_item(Item={
        "userId": user_id,
        "date": date,
        "insights": _fix(insights),
        "updatedAt": datetime.now(timezone.utc).isoformat(),
    })
