# FocusGuard AI — The Last-Minute Life Saver

> **AI-powered deadline companion for students, professionals, and entrepreneurs.**
> Never miss a deadline again.

🌐 **Live Demo:** https://virtualpromptwars-eb2fd.web.app
📦 **Download APK:** https://storage.googleapis.com/focusguard-downloads/focusguard.apk
💻 **GitHub:** https://github.com/Cyberexe1/Focusguard
🔗 **Backend API:** https://6f0tbzcsr6.execute-api.us-east-1.amazonaws.com/health

---

## Problem Statement

Students, professionals, and entrepreneurs frequently miss deadlines, assignments, meetings, bill payments, interviews, and important commitments. Existing productivity tools rely on **passive reminders** that are easy to ignore and do little to help users actually complete their tasks.

Traditional tools tell you *what* you missed. FocusGuard tells you *what to do right now* — and calls your phone if you don't.

---

## Solution Overview

FocusGuard AI is a full-stack AI productivity companion consisting of three components:

| Component | Stack | Hosting |
|-----------|-------|---------|
| Android App | Kotlin, Jetpack Compose | APK via GCS |
| Backend API | Python FastAPI | AWS Lambda + API Gateway |
| Landing Page | React, Vite | Firebase Hosting |

The system works in a continuous loop:

```
User adds task (plain English)
    ↓
Amazon Nova Lite (Bedrock) parses → deadline, effort, category, priority score
    ↓
Daily check-in at 9 AM → user marks progress on sub-tasks
    ↓
9 PM check — did user update tasks?
    No → Push notification + Twilio voice call
    Yes → Streak incremented, risk score drops
    ↓
If risk score > 75 → AI generates Emergency Recovery Plan
    ↓
n8n webhook → Twilio outbound call → user speaks → intent parsed → schedule updated
```

---

## Key Features

### AI Task Triage
Type a task in plain English — *"Submit hackathon project before Sunday 2 PM urgent"*. Amazon Nova Lite parses the deadline, effort, category and assigns a priority score (0–100). No forms, no dropdowns.

### Sub-task Checklist with Daily Check-ins
Break any task into sub-tasks. Every day at **9 AM** a notification fires prompting the user to mark progress. Sub-task completion directly reduces the task's risk score — finishing work lowers urgency automatically.

### 9 PM Accountability Call
If the user hasn't updated their tasks by evening, FocusGuard:
1. Shows a high-priority push notification
2. Calls their phone via Twilio with a personalized AI-generated voice message
3. Listens for a yes/no response → generates an emergency recovery plan if they say yes

### Live Deadline Countdown
Home screen shows a stopwatch carousel (DAYS:HRS:MIN:SEC) for every active task, ticking every second. Tasks turn red when under 24 hours remaining.

### AI-Generated Daily Schedule
The Bedrock-powered planner generates a time-blocked daily schedule based on task priorities and peak productive hours. Users can also create manual schedule blocks on a visual hour-rail timeline — blocks persist to DynamoDB and fire notifications when each block starts.

### Focus Sprint
A 2-hour countdown ring per task with checkpoint logging. No progress at checkpoint → automatic escalation to emergency recovery mode.

### Emergency Recovery Plan
When risk exceeds threshold, Nova Lite generates a compressed recovery plan — core work, testing, and submission phases — with all non-critical tasks postponed.

### Risk Radar
Every task gets a live risk score computed from deadline proximity, effort remaining, and sub-task completion percentage. Visible on every task card and detail screen.

### Habit Intelligence
Consistency heatmap, peak productivity hours detection, effort underestimation tracking, and AI recommendations — all powered by sprint and check-in history.

### Voice Task Capture
Speak a task naturally. On-device Android SpeechRecognizer transcribes it → sent to Bedrock for parsing → saved as a structured task.

### Persistent Login + Real User Data
Session stored in DataStore Preferences. Survives app restarts. Settings screen shows real name, email, and check-in streak.

---

## Technologies Used

### AI / ML
| Technology | Usage |
|------------|-------|
| **Amazon Nova Lite** (Bedrock) | Task parsing, priority scoring, call script generation, intent classification, schedule generation, emergency plans |
| **Amazon Bedrock Converse API** | Unified interface for all Nova Lite interactions |

### Backend
| Technology | Usage |
|------------|-------|
| **Python 3.11 + FastAPI** | REST API with 30+ endpoints |
| **AWS Lambda** | Serverless compute, auto-scaling |
| **AWS API Gateway** (HTTP API) | HTTPS routing to Lambda |
| **Amazon DynamoDB** | 8 tables for tasks, users, schedules, sprints, habits, calls, calendar, risk history |
| **AWS Secrets Manager** | Secure credential storage |
| **Mangum** | ASGI→Lambda adapter |
| **Pydantic v2** | Request validation, input sanitization |
| **python-jose + bcrypt** | JWT auth, password hashing |

### Android
| Technology | Usage |
|------------|-------|
| **Kotlin + Jetpack Compose** | UI, navigation, state management |
| **Hilt** | Dependency injection |
| **Retrofit 2 + Moshi** | Type-safe API calls |
| **DataStore Preferences** | Persistent session storage |
| **AlarmManager** | Exact alarm scheduling for 9 AM check-in and 9 PM deadline alert |
| **WorkManager** | Background worker for evening check-in status verification |
| **Android SpeechRecognizer** | On-device voice transcription |
| **Navigation Compose** | Multi-screen routing |

### Voice + Automation
| Technology | Usage |
|------------|-------|
| **Twilio** | Outbound voice calls with TwiML inline script |
| **Amazon Polly (Aditi)** | Indian English TTS for call scripts |
| **n8n Cloud** | Webhook orchestration between backend and Twilio |

### Google Technologies
| Technology | Usage |
|------------|-------|
| **Firebase Hosting** | Deployed landing page at https://virtualpromptwars-eb2fd.web.app |
| **Google Cloud Storage** | Public APK download at `gs://focusguard-downloads` |
| **Google Artifact Registry** | Docker image registry (provisioned, ready for Cloud Run) |
| **Google Cloud Build** | CI/CD pipeline for container builds |

### Web (Landing Page)
| Technology | Usage |
|------------|-------|
| **React 18 + Vite 5** | Landing page with APK download |
| **Pure CSS** | Claymorphism + glassmorphism design |

---

## Google Technologies Utilized

1. **Firebase Hosting** — The FocusGuard landing page is deployed on Firebase Hosting (`https://virtualpromptwars-eb2fd.web.app`), providing a global CDN, automatic HTTPS, and instant deployments.

2. **Google Cloud Storage** — The Android APK is stored in a public GCS bucket (`gs://focusguard-downloads`) with `allUsers` objectViewer IAM binding, enabling direct download links from the landing page without any backend serving the file.

3. **Google Artifact Registry** — Docker image repository provisioned at `us-central1-docker.pkg.dev/virtualpromptwars-eb2fd/focusguard/web` for containerized web deployments.

4. **Google Cloud Build** — Cloud Build pipeline configured for building and pushing container images to Artifact Registry.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    User (Android App)                       │
└──────────────────────┬──────────────────────────────────────┘
                       │ HTTPS (Retrofit)
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              AWS API Gateway (HTTP API)                      │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              AWS Lambda (Python 3.11 + FastAPI)              │
│                                                             │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │  Auth +     │  │  Bedrock     │  │   DynamoDB       │  │
│  │  JWT        │  │  Nova Lite   │  │   (8 tables)     │  │
│  └─────────────┘  └──────────────┘  └──────────────────┘  │
└──────────────────────┬──────────────────────────────────────┘
                       │ On high risk
                       ▼
┌──────────────────────────────────────────────────────────────┐
│           n8n Cloud webhook → Twilio voice call              │
│           User speaks → intent parsed → plan updated        │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│         Android Local (no server needed)                    │
│  AlarmManager → 9 AM check-in notification                  │
│  WorkManager  → 9 PM check → push notif + call trigger     │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│              Google Cloud (Landing Page + APK)              │
│  Firebase Hosting → virtualpromptwars-eb2fd.web.app         │
│  Cloud Storage   → gs://focusguard-downloads/focusguard.apk │
└──────────────────────────────────────────────────────────────┘
```

---

## API Endpoints (30+)

### Auth
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/register` | Register → returns JWT + name + email |
| POST | `/auth/login` | Login → returns JWT + name + email |

### Tasks
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/tasks` | Create task from plain English (Bedrock parses) |
| GET | `/tasks` | All tasks sorted by priority score |
| GET | `/tasks/{id}` | Single task with sub-tasks |
| PUT | `/tasks/{id}` | Update status / effort / deadline |
| DELETE | `/tasks/{id}` | Delete task |
| POST | `/tasks/{id}/subtasks` | Add sub-task |
| PUT | `/tasks/{id}/subtasks/{subId}` | Toggle done (adjusts priority score) |
| DELETE | `/tasks/{id}/subtasks/{subId}` | Remove sub-task |
| POST | `/tasks/checkin` | Daily check-in — increments streak |
| GET | `/tasks/checkin/status` | Has user checked in today? |
| GET | `/tasks/risk/all` | Risk scores for all active tasks |
| GET | `/tasks/{id}/risk` | Risk score for one task |

### Schedule
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/schedule/generate` | AI-generated schedule (Nova Lite) |
| POST | `/schedule/blocks` | Save manual schedule blocks |
| GET | `/schedule/blocks?date=` | Load saved blocks for date |
| GET | `/schedule/today` | Today's AI-generated schedule |
| POST | `/schedule/emergency` | Emergency recovery plan |

### Sprints, Habits, Dashboard, Calendar, Planning, Calls — see full list in `focusguard-backend/app/routers/`

---

## Local Development

### Backend
```bash
cd focusguard-backend
pip install -r requirements.txt
cp .env.example .env   # fill in secrets
uvicorn main:app --reload
```

### Android
```powershell
cd focusguard-android
# Set API_BASE_URL in local.properties
.\gradlew assembleDebug --no-configuration-cache
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### Web
```bash
cd focusguard-web
npm install
npm run dev         # localhost:5173
npm run build
firebase deploy --only hosting
```

---

## Redeploy Backend (Lambda)

```powershell
cd focusguard-backend

# 1. Sync changes to lambda_build
Copy-Item app\routers\*.py lambda_build\app\routers\ -Force
Copy-Item app\*.py lambda_build\app\ -Force
Copy-Item main.py lambda_build\main.py -Force

# 2. Remove stale bytecode (CRITICAL)
Get-ChildItem lambda_build -Recurse -Directory -Filter __pycache__ | Remove-Item -Recurse -Force

# 3. Zip + deploy
python -c "import shutil; shutil.make_archive('focusguard_lambda','zip',root_dir='lambda_build')"
aws s3 cp focusguard_lambda.zip s3://focusguard-lambda-deploy/focusguard_lambda.zip --region us-east-1
aws lambda update-function-code --function-name focusguard-api --s3-bucket focusguard-lambda-deploy --s3-key focusguard_lambda.zip --region us-east-1
```

## Update APK on GCS

```powershell
gcloud storage cp focusguard-android\app\build\outputs\apk\debug\app-debug.apk gs://focusguard-downloads/focusguard.apk
```

---

## Security Notes

- All endpoints require JWT Bearer token except `/auth/register`, `/auth/login`, `/health`
- `/api/alerts/n8n-call` validates `X-Webhook-Secret` header
- Twilio webhooks validate `X-Twilio-Signature` using `RequestValidator`
- Phone numbers validated to E.164 format at registration and call trigger
- `__pycache__` excluded from Lambda zip to prevent stale bytecode attacks
- `.env` and `local.properties` gitignored — secrets never committed

---

## Known Gotchas

- **Never include `__pycache__` in Lambda zip** — stale `.pyc` overrides fixed `.py` silently
- **Never pass explicit AWS credentials to boto3 in Lambda** — use IAM role only
- **Gradle config cache must stay disabled** — `org.gradle.configuration-cache=false`
- **FastAPI route ordering matters** — fixed paths (`/checkin`, `/voice`) must come before `/{task_id}`
- **CORS** — `allow_credentials=True` is incompatible with `allow_origins=["*"]`; use `allow_credentials=False`

---

## DynamoDB Tables

| Table | Partition Key | Sort Key |
|-------|--------------|----------|
| `focusguard_users` | `userId` | — |
| `focusguard_tasks` | `userId` | `taskId` |
| `focusguard_schedules` | `userId` | `date` |
| `focusguard_sprints` | `userId` | `sprintId` |
| `focusguard_habits` | `userId` | `date` |
| `focusguard_risk_history` | `userId` | `taskId` |
| `focusguard_calls` | `userId` | `callId` |
| `focusguard_calendar_events` | `userId` | `eventId` |

---

*Built for the Vibe2Ship Hackathon — Problem Statement: The Last-Minute Life Saver*
