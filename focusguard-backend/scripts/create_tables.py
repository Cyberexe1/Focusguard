"""
Run once to create both DynamoDB tables with correct schema and GSI.
Usage:
    cd focusguard-backend
    python scripts/create_tables.py
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


def create_users_table(client):
    print(f"Creating table: {settings.dynamodb_users_table} ...")
    try:
        client.create_table(
            TableName=settings.dynamodb_users_table,
            # Primary key: userId (partition only)
            KeySchema=[
                {"AttributeName": "userId", "KeyType": "HASH"},
            ],
            AttributeDefinitions=[
                {"AttributeName": "userId",  "AttributeType": "S"},
                {"AttributeName": "email",   "AttributeType": "S"},
            ],
            # GSI on email — used by login to look up by email without full scan
            GlobalSecondaryIndexes=[
                {
                    "IndexName": "email-index",
                    "KeySchema": [{"AttributeName": "email", "KeyType": "HASH"}],
                    "Projection": {"ProjectionType": "ALL"},
                }
            ],
            BillingMode="PAY_PER_REQUEST",   # on-demand — scales to zero cost
        )
        print(f"  ✅ {settings.dynamodb_users_table} created.")
    except ClientError as e:
        if e.response["Error"]["Code"] == "ResourceInUseException":
            print(f"  ⚠️  {settings.dynamodb_users_table} already exists — skipping.")
        else:
            raise


def create_tasks_table(client):
    print(f"Creating table: {settings.dynamodb_tasks_table} ...")
    try:
        client.create_table(
            TableName=settings.dynamodb_tasks_table,
            # Composite key: userId (partition) + taskId (sort)
            KeySchema=[
                {"AttributeName": "userId", "KeyType": "HASH"},
                {"AttributeName": "taskId", "KeyType": "RANGE"},
            ],
            AttributeDefinitions=[
                {"AttributeName": "userId", "AttributeType": "S"},
                {"AttributeName": "taskId", "AttributeType": "S"},
            ],
            BillingMode="PAY_PER_REQUEST",
        )
        print(f"  ✅ {settings.dynamodb_tasks_table} created.")
    except ClientError as e:
        if e.response["Error"]["Code"] == "ResourceInUseException":
            print(f"  ⚠️  {settings.dynamodb_tasks_table} already exists — skipping.")
        else:
            raise


if __name__ == "__main__":
    client = get_client()
    create_users_table(client)
    create_tasks_table(client)
    print("\nDone. Both tables are ready.")
