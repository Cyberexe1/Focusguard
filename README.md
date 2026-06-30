# FocusGuard AI

> **Your AI-powered deadline companion.** Never miss a deadline again.

FocusGuard AI triages your tasks with Amazon Nova Lite, counts down every deadline to the second, breaks work into sub-tasks with daily check-ins, and calls your phone when you are about to slip.

---

## What's Inside

```
Vibe2Ship/
├── focusguard-backend/     Python FastAPI — deployed to AWS Lambda
├── focusguard-android/     Kotlin Jetpack Compose Android app
└── focusguard-web/         React landing page (Vite)
```

---

## Live URLs

| Service | URL |
|---------|-----|
| Backend API | `https://6f0tbzcsr6.execute-api.us-east-1.amazonaws.com` |
| Health check | `https://6f0tbzcsr6.execute-api.us-east-1.amazonaws.com/health` |
| n8n webhook | `https://vikastiwari321.app.n8n.cloud/webhook/focusguard-alert` |

---

## Feature Overview

### Android App
- **AI Task Triage** — type a task in plain English, Amazon Nova Lite parses deadline, effort and category, scores priority 0–100
- **Sub-task Checklist** — break any task into steps, check them off daily, progress bar fills as you go
- **Daily Check-in** — 9 AM notification every morning, tap to confirm you worked; builds a streak counter
- **Live Deadline Countdown** — stopwatch carousel on the home screen ticking DAYS:HRS:MIN:SEC per task
- **Hour-Rail Schedule Timeline** — add timed blocks to a visual day planner, get a notification when each block starts, blocks persist to DynamoDB
- **Focus Sprint** — 2-hour countdown ring per task with checkpoint logging
- **Emergency Recovery** — AI-generated compressed recovery plan when risk spikes
- **Risk Radar** — every task gets a live risk score driven by deadline proximity and sub-task completion
- **Habit Intelligence** — consistency heatmap, peak productivity hours, AI recommendations
- **Dashboard** — daily/weekly productivity score, task completions, focus hours, sprint success rate
- **Voice Task Capture** — speak a task, on-device speech recognition transcribes it
- **Persistent Login** — session stored in DataStore, survives app restarts
- **Accountability Calls** — Twilio voice call via n8n when risk exceeds threshold

### Backend (FastAPI on Lambda)
| Route | Description |
|-------|-------------|
| `POST /auth/register` | Register, returns JWT + name + email |
| `POST /auth/login` | Login, returns JWT + name + email |
| `GET /tasks` | All tasks sorted by priority score |
| `POST /tasks` | Create task from raw text (Bedrock parses it) |
| `PUT /tasks/{id}` | Update status / effort / deadline |
| `DELETE /tasks/{id}` | Delete task |
| `POST /tasks/{id}/subtasks` | Add sub-task |
| `PUT /tasks/{id}/subtasks/{subId}` | Toggle sub-task done (recalculates priority) |
| `DELETE /tasks/{id}/subtasks/{subId}` | Remove sub-task |
| `POST /tasks/checkin` | Daily check-in — increments streak on active tasks |
| `GET /tasks/checkin/status` | Has user checked in today? |
| `POST /schedule/generate` | AI-generated daily schedule (Nova Lite) |
| `POST /schedule/blocks` | Save manual schedule blocks |
| `GET /schedule/blocks?date=` | Load saved schedule blocks |
| `GET /schedule/today` | Today's AI-generated schedule |
| `POST /sprints` | Start a focus sprint |
| `PUT /sprints/{id}/checkpoint` | Log sprint checkpoint |
| `PUT /sprints/{id}/end` | End sprint |
| `GET /habits/insights` | Habit analytics (peak hours, consistency) |
| `GET /dashboard/daily` | Daily productivity metrics |
| `GET /dashboard/weekly` | Weekly metrics |
| `GET /calendar/events` | Calendar events |
| `POST /calendar/events` | Create calendar event |

---

## Tech Stack

### Backend
- Python 3.11, FastAPI 0.115, Pydantic 2.9
- AWS Lambda (us-east-1), API Gateway HTTP API
- Amazon DynamoDB (8 tables)
- Amazon Bedrock — Nova Lite (`amazon.nova-lite-v1:0`)
- AWS Secrets Manager for credentials
- Twilio for voice calls
- n8n cloud for webhook orchestration
- Mangum for ASGI→Lambda adapter

### Android
- Kotlin, Jetpack Compose
- Hilt for dependency injection
- Retrofit 2 + Moshi for API calls
- DataStore Preferences for session persistence
- AlarmManager for local notifications
- Android SpeechRecognizer for voice input
- Navigation Compose

### Web
- React 18, Vite 5
- Pure CSS — claymorphism + glassmorphism design

---

## DynamoDB Tables

| Table | Key |
|-------|-----|
| `focusguard_users` | `userId` (+ email-index GSI) |
| `focusguard_tasks` | `userId` + `taskId` |
| `focusguard_schedules` | `userId` + `date` |
| `focusguard_sprints` | `sprintId` |
| `focusguard_habits` | `userId` + `date` |
| `focusguard_risk_history` | `userId` + `taskId` |
| `focusguard_calls` | `callId` |
| `focusguard_calendar_events` | `userId` + `eventId` |

---

## Local Development

### Backend

```bash
cd focusguard-backend
python -m venv venv
venv\Scripts\activate          # Windows
pip install -r requirements.txt
cp .env.example .env           # fill in secrets
uvicorn main:app --reload
```

### Android

1. Open `focusguard-android/` in Android Studio
2. Set `API_BASE_URL` in `local.properties`:
   ```
   API_BASE_URL=https://6f0tbzcsr6.execute-api.us-east-1.amazonaws.com
   ```
3. Build and run:
   ```
   gradlew assembleDebug --no-configuration-cache
   adb install -r app\build\outputs\apk\debug\app-debug.apk
   ```

### Web

```bash
cd focusguard-web
npm install
npm run dev        # dev server at localhost:5173
npm run build      # production build → dist/
```

To link the APK download, copy your built APK to `focusguard-web/public/focusguard.apk`.

---

## Deploy Backend to AWS Lambda

> Full instructions in `focusguard-backend/DEPLOY_AWS.md`

Quick redeploy (run from `focusguard-backend/`):

```powershell
# 1. Sync source changes to lambda_build
Copy-Item app\routers\*.py lambda_build\app\routers\ -Force
Copy-Item app\*.py lambda_build\app\ -Force

# 2. Remove stale bytecode (CRITICAL — old .pyc overrides fixed .py)
Get-ChildItem lambda_build -Recurse -Directory -Filter __pycache__ | Remove-Item -Recurse -Force

# 3. Zip
python -c "import shutil; shutil.make_archive('focusguard_lambda','zip',root_dir='lambda_build')"

# 4. Upload and deploy
aws s3 cp focusguard_lambda.zip s3://focusguard-lambda-deploy/focusguard_lambda.zip --region us-east-1
aws lambda update-function-code --function-name focusguard-api --s3-bucket focusguard-lambda-deploy --s3-key focusguard_lambda.zip --region us-east-1
```

---

## Environment Variables (Lambda)

| Variable | Description |
|----------|-------------|
| `JWT_SECRET` | Secret key for JWT signing |
| `JWT_ALGORITHM` | `HS256` |
| `JWT_EXPIRE_DAYS` | `7` |
| `AWS_REGION_NAME` | `us-east-1` |
| `DYNAMODB_USERS_TABLE` | `focusguard_users` |
| `DYNAMODB_TASKS_TABLE` | `focusguard_tasks` |
| `BEDROCK_MODEL_ID` | `amazon.nova-lite-v1:0` |
| `TWILIO_ACCOUNT_SID` | From Twilio console |
| `TWILIO_AUTH_TOKEN` | From Twilio console |
| `TWILIO_PHONE_NUMBER` | Your Twilio number |
| `DEFAULT_ALERT_PHONE` | Phone to call for accountability |
| `N8N_WEBHOOK_URL` | n8n cloud webhook URL |
| `N8N_WEBHOOK_SECRET` | Shared secret for webhook auth |
| `API_PUBLIC_URL` | API Gateway invoke URL |

---

## Project Architecture

```
User → Android App
         │
         ├── Retrofit → API Gateway → Lambda → FastAPI
         │                                        │
         │                              ┌─────────┴──────────┐
         │                           DynamoDB           Amazon Bedrock
         │                                               (Nova Lite)
         │
         ├── AlarmManager → Local Notifications (9 AM check-in, schedule blocks)
         │
         └── On high risk → Lambda → n8n webhook → Twilio → Voice Call to user
```

---

## Known Gotchas

- **Never include `__pycache__` in the Lambda zip** — stale `.pyc` bytecode overrides fixed `.py` files silently
- **Never pass explicit AWS credentials to boto3 in Lambda** — use IAM role only (`region_name` only)
- **Gradle config cache must stay disabled** — `org.gradle.configuration-cache=false` in `gradle.properties`
- **`local.properties` is not read by `project.findProperty()`** — must load it explicitly with `java.util.Properties`
- **FastAPI route ordering matters** — fixed-path routes (`/checkin`, `/voice`) must come before wildcard routes (`/{task_id}`)

---

## IAM Roles

- **Lambda execution role** `focusguard-api-role-xtagdp4h` — has DynamoDB full access, Bedrock invoke, Secrets Manager read
- **Deploy user** `focusguard-backend` — has `AWSLambdaFullAccess` and S3 access to `focusguard-lambda-deploy`

---

## n8n Workflow

Import `focusguard-backend/n8n_workflow.json` into your n8n instance.

The workflow:
1. Receives a webhook from the backend with task details
2. Validates the shared secret header
3. Calls Twilio to place a voice call
4. Speaks an accountability message using TTS

---

Built for the Vibe2Ship hackathon.
