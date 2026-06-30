"""Focus Sprint Manager — Phase 2"""

import uuid
from decimal import Decimal
from datetime import datetime, timezone
import boto3
from app.config import settings


def get_dynamodb():
    kwargs = {"region_name": settings.aws_region}
    return boto3.resource("dynamodb", **kwargs)


def start_sprint(user_id: str, task_id: str, duration_hours: float = 2.0) -> dict:
    sprint_id = str(uuid.uuid4())
    now = datetime.now(timezone.utc).isoformat()
    record = {
        "userId": user_id,
        "sprintId": sprint_id,
        "taskId": task_id,
        "durationHours": Decimal(str(duration_hours)),
        "startTime": now,
        "endTime": None,
        "checkpoints": [],
        "completionPercent": 0,
        "escalated": False,
        "status": "active",
    }
    get_dynamodb().Table("focusguard_sprints").put_item(Item=record)
    return record


def log_checkpoint(user_id: str, sprint_id: str, progress_made: bool) -> dict:
    table = get_dynamodb().Table("focusguard_sprints")
    response = table.get_item(Key={"userId": user_id, "sprintId": sprint_id})
    sprint = response.get("Item")
    if not sprint:
        raise ValueError("Sprint not found")

    now = datetime.now(timezone.utc).isoformat()
    checkpoints = list(sprint.get("checkpoints", []))
    checkpoints.append({"time": now, "progressReported": progress_made})

    updates = {
        "checkpoints": checkpoints,
        "escalated": not progress_made,
        "updatedAt": now,
    }
    table.update_item(
        Key={"userId": user_id, "sprintId": sprint_id},
        UpdateExpression="SET checkpoints = :c, escalated = :e, updatedAt = :u",
        ExpressionAttributeValues={
            ":c": checkpoints,
            ":e": not progress_made,
            ":u": now,
        },
    )
    sprint.update(updates)
    return sprint


def end_sprint(user_id: str, sprint_id: str, completion_percent: int) -> dict:
    now = datetime.now(timezone.utc).isoformat()
    table = get_dynamodb().Table("focusguard_sprints")
    table.update_item(
        Key={"userId": user_id, "sprintId": sprint_id},
        UpdateExpression="SET #s = :s, endTime = :e, completionPercent = :c, updatedAt = :u",
        ExpressionAttributeNames={"#s": "status"},
        ExpressionAttributeValues={
            ":s": "completed",
            ":e": now,
            ":c": completion_percent,
            ":u": now,
        },
    )
    response = table.get_item(Key={"userId": user_id, "sprintId": sprint_id})
    return response.get("Item", {})


def get_sprints_for_user(user_id: str) -> list[dict]:
    table = get_dynamodb().Table("focusguard_sprints")
    response = table.query(
        KeyConditionExpression=boto3.dynamodb.conditions.Key("userId").eq(user_id)
    )
    return response.get("Items", [])


def get_sprint(user_id: str, sprint_id: str) -> dict | None:
    table = get_dynamodb().Table("focusguard_sprints")
    response = table.get_item(Key={"userId": user_id, "sprintId": sprint_id})
    return response.get("Item")
