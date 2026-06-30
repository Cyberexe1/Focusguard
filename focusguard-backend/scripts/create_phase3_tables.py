"""
Create Phase 3 DynamoDB tables.
Usage (from focusguard-backend):
    PowerShell:  $env:PYTHONPATH="."; python scripts/create_phase3_tables.py
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

    # focusguard_calls — PK: userId  SK: callId
    safe_create(client, "focusguard_calls", dict(
        TableName="focusguard_calls",
        KeySchema=[
            {"AttributeName": "userId", "KeyType": "HASH"},
            {"AttributeName": "callId", "KeyType": "RANGE"},
        ],
        AttributeDefinitions=[
            {"AttributeName": "userId", "AttributeType": "S"},
            {"AttributeName": "callId", "AttributeType": "S"},
        ],
        BillingMode="PAY_PER_REQUEST",
    ))

    # focusguard_calendar_tokens — removed (no Google OAuth needed)

    # focusguard_calendar_events — PK: userId  SK: eventId
    safe_create(client, "focusguard_calendar_events", dict(
        TableName="focusguard_calendar_events",
        KeySchema=[
            {"AttributeName": "userId",  "KeyType": "HASH"},
            {"AttributeName": "eventId", "KeyType": "RANGE"},
        ],
        AttributeDefinitions=[
            {"AttributeName": "userId",  "AttributeType": "S"},
            {"AttributeName": "eventId", "AttributeType": "S"},
        ],
        BillingMode="PAY_PER_REQUEST",
    ))

    print("\nAll Phase 3 tables created.")
