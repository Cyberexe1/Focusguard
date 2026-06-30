"""
Run once to create Phase 2 DynamoDB tables.
Usage (from focusguard-backend folder):
    PowerShell:  $env:PYTHONPATH="."; python scripts/create_phase2_tables.py
    CMD:         set PYTHONPATH=. && python scripts/create_phase2_tables.py
    Linux/Mac:   PYTHONPATH=. python scripts/create_phase2_tables.py
"""

import boto3
from botocore.exceptions import ClientError
from app.config import settings


def get_client():
    kwargs = {"region_name": settings.aws_region}
    if settings.aws_access_key_id:
        kwargs["aws_access_key_id"] = settings.aws_access_key_id
    if settings.aws_secret_access_key:
        kwargs["aws_secret_access_key"] = settings.aws_secret_access_key
    return boto3.client("dynamodb", **kwargs)


def safe_create(client, name: str, kwargs: dict):
    print(f"Creating table: {name} ...")
    try:
        client.create_table(**kwargs)
        print(f"  ✅ {name} created.")
    except ClientError as e:
        if e.response["Error"]["Code"] == "ResourceInUseException":
            print(f"  ⚠️  {name} already exists — skipping.")
        else:
            raise


if __name__ == "__main__":
    client = get_client()

    # ── focusguard_schedules ──────────────────────────────────────────────
    # PK: userId  SK: date (YYYY-MM-DD)
    safe_create(client, "focusguard_schedules", dict(
        TableName="focusguard_schedules",
        KeySchema=[
            {"AttributeName": "userId", "KeyType": "HASH"},
            {"AttributeName": "date",   "KeyType": "RANGE"},
        ],
        AttributeDefinitions=[
            {"AttributeName": "userId", "AttributeType": "S"},
            {"AttributeName": "date",   "AttributeType": "S"},
        ],
        BillingMode="PAY_PER_REQUEST",
    ))

    # ── focusguard_risk_history ───────────────────────────────────────────
    # PK: userId  SK: taskId#timestamp (composite sort key)
    safe_create(client, "focusguard_risk_history", dict(
        TableName="focusguard_risk_history",
        KeySchema=[
            {"AttributeName": "userId",           "KeyType": "HASH"},
            {"AttributeName": "taskIdTimestamp",  "KeyType": "RANGE"},
        ],
        AttributeDefinitions=[
            {"AttributeName": "userId",          "AttributeType": "S"},
            {"AttributeName": "taskIdTimestamp", "AttributeType": "S"},
        ],
        BillingMode="PAY_PER_REQUEST",
    ))

    # ── focusguard_sprints ────────────────────────────────────────────────
    # PK: userId  SK: sprintId
    safe_create(client, "focusguard_sprints", dict(
        TableName="focusguard_sprints",
        KeySchema=[
            {"AttributeName": "userId",   "KeyType": "HASH"},
            {"AttributeName": "sprintId", "KeyType": "RANGE"},
        ],
        AttributeDefinitions=[
            {"AttributeName": "userId",   "AttributeType": "S"},
            {"AttributeName": "sprintId", "AttributeType": "S"},
        ],
        BillingMode="PAY_PER_REQUEST",
    ))

    # ── focusguard_habits ────────────────────────────────────────────────
    # PK: userId  SK: date (YYYY-MM-DD)  — one record per user per day
    safe_create(client, "focusguard_habits", dict(
        TableName="focusguard_habits",
        KeySchema=[
            {"AttributeName": "userId", "KeyType": "HASH"},
            {"AttributeName": "date",   "KeyType": "RANGE"},
        ],
        AttributeDefinitions=[
            {"AttributeName": "userId", "AttributeType": "S"},
            {"AttributeName": "date",   "AttributeType": "S"},
        ],
        BillingMode="PAY_PER_REQUEST",
    ))

    print("\nAll Phase 2 tables created.")
