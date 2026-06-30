"""
Bedrock service — Amazon Nova Lite via invoke_model (works on all boto3 versions).
Fallback: keyword-based offline parser if Bedrock is unreachable.
"""

import boto3
import json
import re
from datetime import datetime, timedelta, timezone
from app.config import settings


def _get_client():
    return boto3.client("bedrock-runtime", region_name=settings.aws_region)


# Public alias used by other services
get_bedrock_client = _get_client


def _invoke(system_prompt: str, user_prompt: str, max_tokens: int = 512, temperature: float = 0.1) -> str:
    """
    Unified invoke helper. Tries Converse API first (boto3 >= 1.28.57),
    falls back to invoke_model with Nova Lite native format.
    Returns the raw text response.
    """
    client = _get_client()

    # ── Try Converse API (cleaner, model-agnostic) ─────────────────────────
    try:
        response = client.converse(
            modelId=settings.bedrock_model_id,
            system=[{"text": system_prompt}],
            messages=[{"role": "user", "content": [{"text": user_prompt}]}],
            inferenceConfig={"maxTokens": max_tokens, "temperature": temperature},
        )
        return response["output"]["message"]["content"][0]["text"]
    except AttributeError:
        pass  # boto3 too old for converse — fall through to invoke_model

    # ── Fall back to invoke_model with Nova Lite native format ────────────
    # Nova Lite uses the same messages format as Claude via invoke_model
    body = json.dumps({
        "messages": [
            {
                "role": "user",
                "content": [
                    {"text": system_prompt + "\n\n" + user_prompt}
                ],
            }
        ],
        "inferenceConfig": {
            "maxTokens": max_tokens,
            "temperature": temperature,
        },
    })
    response = client.invoke_model(
        modelId=settings.bedrock_model_id,
        contentType="application/json",
        accept="application/json",
        body=body,
    )
    result = json.loads(response["body"].read())
    # Nova Lite invoke_model response shape
    return result["output"]["message"]["content"][0]["text"]


def parse_and_score_task(raw_text: str) -> dict:
    try:
        return _bedrock_parse(raw_text)
    except Exception as e:
        print(f"[bedrock_service] Bedrock unavailable ({type(e).__name__}: {e}). Using fallback.")
        return _fallback_parse(raw_text)


def _bedrock_parse(raw_text: str) -> dict:
    now_iso = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")

    system_prompt = (
        "You are a task analysis AI for a productivity app. "
        "Extract structured data from natural-language task descriptions. "
        "Always respond with valid JSON only — no markdown, no explanation."
    )

    user_prompt = f"""Analyze this task and return a JSON object.

Current time: {now_iso}
Task: "{raw_text}"

Priority score rules (0-100):
- 90-100: due today / urgent / exam / submission / asap
- 70-89: due within 2 days / interview / assignment deadline
- 40-69: due within a week
- 0-39: low urgency, personal, no clear deadline

Return exactly this JSON with no extra text:
{{
  "title": "concise task title under 60 chars",
  "deadline_iso": "ISO 8601 datetime, estimate if not stated, default 7 days from now",
  "effort_hours": 2.0,
  "category": "one of: Hackathon, Assignment, Interview, Exam, Meeting, Project, Personal, Reading, Exercise, Other",
  "priority_score": 75,
  "priority_rank_reason": "one sentence explaining the score"
}}"""

    output = _invoke(system_prompt, user_prompt, max_tokens=512, temperature=0.1)
    cleaned = re.sub(r"```(?:json)?|```", "", output).strip()
    parsed = json.loads(cleaned)
    parsed["priority_score"] = max(0, min(100, int(parsed.get("priority_score", 50))))
    parsed["effort_hours"] = float(parsed.get("effort_hours", 2.0))
    return parsed


def generate_schedule_prompt(tasks_summary: str, peak_hours: str, available_hours: float, target_date: str) -> str:
    """Helper used by schedule_service to generate schedule via Bedrock."""
    system = "You are a productivity scheduling AI. Return only valid JSON, no markdown."
    user = f"""Generate a time-blocked daily schedule for {target_date}.

Tasks (sorted by priority):
{tasks_summary}

Constraints:
- Available hours today: {available_hours}h
- User's peak productive hours: {peak_hours}
- Schedule high-priority tasks during peak hours
- Add 10-minute buffers between sessions
- Max 2-hour continuous work blocks

Return exactly this JSON:
{{
  "date": "{target_date}",
  "schedule": [
    {{
      "startTime": "HH:MM",
      "endTime": "HH:MM",
      "taskId": "uuid",
      "taskTitle": "string",
      "sessionType": "deep_work | review | break"
    }}
  ],
  "unscheduled": ["taskId1"],
  "warnings": ["string"]
}}"""
    try:
        output = _invoke(system, user, max_tokens=1024, temperature=0.2)
        cleaned = re.sub(r"```(?:json)?|```", "", output).strip()
        return cleaned
    except Exception as e:
        raise RuntimeError(f"Bedrock schedule generation failed: {e}")


def generate_habit_insights_prompt(behavioral_data: dict) -> str:
    """Helper used by habit_service."""
    system = "You are a productivity coach. Return only valid JSON."
    user = f"""Analyze this user's productivity patterns and generate insights.

Data:
- Peak active hours: {behavioral_data['peakHoursRange']}
- Effort underestimation: {behavioral_data['avgUnderestimation'] * 100:.0f}%
- Deadline miss rate: {behavioral_data['missRate'] * 100:.0f}%
- Consistency score: {behavioral_data['consistency']}%
- Completed {behavioral_data['completedTasks']}/{behavioral_data['totalTasks']} tasks

Return exactly this JSON:
{{
  "peakProductivityHours": ["{behavioral_data['peakHoursRange']}"],
  "avgEffortUnderestimation": {behavioral_data['avgUnderestimation']},
  "consistencyScore": {behavioral_data['consistency']},
  "recommendations": [
    "actionable recommendation 1",
    "actionable recommendation 2",
    "actionable recommendation 3"
  ]
}}"""
    output = _invoke(system, user, max_tokens=512, temperature=0.3)
    cleaned = re.sub(r"```(?:json)?|```", "", output).strip()
    return cleaned


def _fallback_parse(raw_text: str) -> dict:
    now = datetime.now(timezone.utc)
    lower = raw_text.lower()

    if any(w in lower for w in ["urgent", "asap", "today", "now", "immediately"]):
        score, deadline = 93, now + timedelta(hours=6)
    elif any(w in lower for w in ["tomorrow", "submission", "submit", "exam", "interview"]):
        score, deadline = 80, now + timedelta(days=1)
    elif any(w in lower for w in ["deadline", "due", "assignment", "homework"]):
        score, deadline = 65, now + timedelta(days=2)
    elif any(w in lower for w in ["this week", "week", "next"]):
        score, deadline = 42, now + timedelta(days=5)
    else:
        score, deadline = 35, now + timedelta(days=7)

    if any(w in lower for w in ["hackathon", "hack"]):
        category = "Hackathon"
    elif any(w in lower for w in ["assignment", "homework", "dbms", "lab"]):
        category = "Assignment"
    elif any(w in lower for w in ["interview"]):
        category = "Interview"
    elif any(w in lower for w in ["exam", "test", "quiz", "midterm"]):
        category = "Exam"
    elif any(w in lower for w in ["meeting", "call", "sync"]):
        category = "Meeting"
    elif any(w in lower for w in ["project", "sprint", "feature", "build"]):
        category = "Project"
    else:
        category = "Personal"

    effort = 2.0
    m = re.search(r"(\d+(?:\.\d+)?)\s*h(?:ours?)?", lower)
    if m:
        effort = float(m.group(1))

    title = raw_text.strip().rstrip(".")
    title = (title[0].upper() + title[1:]) if title else "New Task"

    return {
        "title": title[:60],
        "deadline_iso": deadline.isoformat(),
        "effort_hours": effort,
        "category": category,
        "priority_score": score,
        "priority_rank_reason": "Score estimated offline from task keywords (Bedrock unavailable).",
    }
