# FocusGuard AI — AWS Setup Guide (Phase 1)

Three AWS services are needed: **IAM**, **DynamoDB**, and **Bedrock**.
Everything below is free-tier or near-zero cost for a hackathon.

---

## Step 1 — Create an AWS Account

1. Go to https://aws.amazon.com → **Create an AWS Account**
2. Complete sign-up (credit card required but not charged for free tier)
3. Choose **Basic (Free)** support plan

---

## Step 2 — Create an IAM User for the Backend

Never use your root account credentials in code.

1. In the AWS Console search bar type **IAM** → open it
2. Left sidebar → **Users** → **Create user**
3. Username: `focusguard-backend`
4. Check **Provide user access to the AWS Management Console** → No (API only)
5. Click **Next: Permissions**
6. Select **Attach policies directly**
7. Search for and attach these two policies:
   - `AmazonDynamoDBFullAccess`
   - `AmazonBedrockFullAccess`
8. Click **Next** → **Create user**
9. Click the user you just created → **Security credentials** tab
10. Under **Access keys** → **Create access key**
11. Use case: **Application running outside AWS**
12. Copy the **Access key ID** and **Secret access key** — you will only see the secret once
13. Paste both into your `.env` file:
    ```
    AWS_ACCESS_KEY_ID=AKIA...
    AWS_SECRET_ACCESS_KEY=...
    ```

---

## Step 3 — Enable Bedrock Model Access

By default, Claude models are not enabled. You must request access.

1. In the AWS Console, set your region to **us-east-1** (top-right dropdown)
2. Search for **Amazon Bedrock** → open it
3. Left sidebar → **Model access**
4. Click **Manage model access**
5. Find **Anthropic** → check:
   - ✅ **Claude Instant** (model ID: `anthropic.claude-instant-v1`) — fastest and cheapest
   - ✅ **Claude 3 Haiku** (optional, better quality, slightly more expensive)
6. Click **Save changes**
7. Wait 1–5 minutes until status shows **Access granted**

> **Cost:** Claude Instant is ~$0.0008 per 1K input tokens. A single task parse uses ~400 tokens = $0.0003 per call. Negligible for a hackathon.

---

## Step 4 — Create DynamoDB Tables

Once your `.env` is configured, run:

```bash
cd focusguard-backend
pip install -r requirements.txt

# Windows CMD
set PYTHONPATH=. && python scripts/create_tables.py

# Windows PowerShell
$env:PYTHONPATH="."; python scripts/create_tables.py

# macOS / Linux
PYTHONPATH=. python scripts/create_tables.py
```

This creates:
- `focusguard_users` — with a GSI on `email` for login lookups
- `focusguard_tasks` — composite key (userId + taskId)

Both use **PAY_PER_REQUEST** billing — you pay per read/write, no hourly cost when idle.

**To verify tables were created:**
1. AWS Console → **DynamoDB** → **Tables**
2. You should see `focusguard_users` and `focusguard_tasks`

---

## Step 5 — Configure Your .env File

```bash
cd focusguard-backend
cp .env.example .env
```

Edit `.env`:

```env
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=AKIA...your key...
AWS_SECRET_ACCESS_KEY=...your secret...

DYNAMODB_USERS_TABLE=focusguard_users
DYNAMODB_TASKS_TABLE=focusguard_tasks

BEDROCK_MODEL_ID=anthropic.claude-instant-v1

# Generate a strong secret: python -c "import secrets; print(secrets.token_hex(32))"
JWT_SECRET=your_generated_secret_here
JWT_ALGORITHM=HS256
JWT_EXPIRE_DAYS=7
```

Generate a secure JWT secret:
```bash
python -c "import secrets; print(secrets.token_hex(32))"
```

---

## Step 6 — Run the Backend

```bash
cd focusguard-backend
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

Test it:
```bash
curl http://localhost:8000/health
# Expected: {"status":"ok","service":"focusguard-api","phase":1}
```

Interactive API docs:
- http://localhost:8000/docs   (Swagger UI)
- http://localhost:8000/redoc  (ReDoc)

---

## Step 7 — Connect the Android App

In `focusguard-android/local.properties`:
```
API_BASE_URL=http://10.0.2.2:8000
```

`10.0.2.2` is the Android emulator's alias for your laptop's `localhost`.

If testing on a **physical device**, use your laptop's local IP instead:
```
API_BASE_URL=http://192.168.1.x:8000
```
(find your IP with `ipconfig` on Windows)

---

## Quick Verification Checklist

```
[ ] AWS account created
[ ] IAM user focusguard-backend created with DynamoDB + Bedrock access
[ ] Access key ID and secret copied into .env
[ ] Bedrock Claude Instant access granted (us-east-1)
[ ] python scripts/create_tables.py ran successfully
[ ] uvicorn main:app --reload started without errors
[ ] GET /health returns 200
[ ] POST /auth/register creates a user (check DynamoDB console)
[ ] POST /auth/login returns a JWT token
[ ] POST /tasks with a description returns a task with priority_score
```

---

## AWS Free Tier Limits (relevant to Phase 1)

| Service | Free Tier | Typical hackathon usage |
|---|---|---|
| DynamoDB | 25 GB storage, 25 read/write units/month | Uses < 1 MB |
| Bedrock (Claude Instant) | No free tier — pay per token | ~$0.10 total for demo |
| IAM | Always free | N/A |

Total expected AWS cost for a hackathon demo: **< $1.00**

---

## Troubleshooting

**`NoCredentialsError`** — `.env` not loaded or wrong key values. Check with:
```python
import boto3
boto3.client("sts").get_caller_identity()
```

**`ResourceNotFoundException` on DynamoDB** — run `create_tables.py` first.

**`AccessDeniedException` on Bedrock** — model access not approved yet. Wait 5 min and retry, or check AWS Console → Bedrock → Model access.

**Android can't reach backend** — make sure `uvicorn` is running and `API_BASE_URL` uses `10.0.2.2` not `localhost`.
