import boto3
from botocore.exceptions import ClientError
from app.config import settings


def get_dynamodb():
    """Return a DynamoDB resource using IAM role credentials (Lambda).
    Never pass explicit credentials — boto3 picks up AWS_ACCESS_KEY_ID,
    AWS_SECRET_ACCESS_KEY, and AWS_SESSION_TOKEN automatically from the
    Lambda execution environment, which all three together are valid.
    Passing only the first two (without the session token) causes
    UnrecognizedClientException.
    """
    return boto3.resource("dynamodb", region_name=settings.region)


def get_users_table():
    return get_dynamodb().Table(settings.dynamodb_users_table)


def get_tasks_table():
    return get_dynamodb().Table(settings.dynamodb_tasks_table)


# ── User operations ───────────────────────────────────────────────────────────

def get_user_by_email(email: str) -> dict | None:
    """Query focusguard_users via the email-index GSI."""
    table = get_users_table()
    try:
        response = table.query(
            IndexName="email-index",
            KeyConditionExpression=boto3.dynamodb.conditions.Key("email").eq(email),
        )
        items = response.get("Items", [])
        return items[0] if items else None
    except ClientError as e:
        raise RuntimeError(f"DynamoDB query failed: {e.response['Error']['Message']}")


def get_user_by_id(user_id: str) -> dict | None:
    table = get_users_table()
    response = table.get_item(Key={"userId": user_id})
    return response.get("Item")


def create_user(user: dict) -> None:
    """Put a user record. Raises if email already exists via GSI check."""
    table = get_users_table()
    table.put_item(Item=user)


# ── Task operations ───────────────────────────────────────────────────────────

def create_task(task: dict) -> None:
    get_tasks_table().put_item(Item=task)


def get_tasks_for_user(user_id: str) -> list[dict]:
    """Return all tasks for a user, sorted by priority_score descending."""
    table = get_tasks_table()
    response = table.query(
        KeyConditionExpression=boto3.dynamodb.conditions.Key("userId").eq(user_id),
    )
    items = response.get("Items", [])
    # Sort by priority score descending (highest urgency first)
    return sorted(items, key=lambda t: int(t.get("priorityScore", 0)), reverse=True)


def get_task(user_id: str, task_id: str) -> dict | None:
    table = get_tasks_table()
    response = table.get_item(Key={"userId": user_id, "taskId": task_id})
    return response.get("Item")


def update_task(user_id: str, task_id: str, updates: dict) -> dict | None:
    """Apply partial updates using UpdateExpression."""
    table = get_tasks_table()
    if not updates:
        return get_task(user_id, task_id)

    expr_parts = []
    expr_values = {}
    expr_names = {}

    for key, value in updates.items():
        safe_key = f"#f_{key}"
        val_key = f":v_{key}"
        expr_names[safe_key] = key
        expr_values[val_key] = value
        expr_parts.append(f"{safe_key} = {val_key}")

    update_expression = "SET " + ", ".join(expr_parts)

    response = table.update_item(
        Key={"userId": user_id, "taskId": task_id},
        UpdateExpression=update_expression,
        ExpressionAttributeNames=expr_names,
        ExpressionAttributeValues=expr_values,
        ReturnValues="ALL_NEW",
    )
    return response.get("Attributes")


def delete_task(user_id: str, task_id: str) -> None:
    get_tasks_table().delete_item(Key={"userId": user_id, "taskId": task_id})
