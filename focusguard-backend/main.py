from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.routers import auth, tasks, schedule, risk, sprints, habits, dashboard, calls, calendar, planning

app = FastAPI(
    title="FocusGuard AI — Backend API",
    description="Phase 1 + Phase 2 + Phase 3: The complete AI productivity companion.",
    version="3.0.0",
)

# CORS — allow_credentials=True is incompatible with allow_origins=["*"] per the
# CORS spec. Since we're mobile-first (Android) and don't use browser cookies,
# credentials mode is not needed. Use explicit origins list in production.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Phase 1
app.include_router(auth.router)
app.include_router(tasks.router)

# Phase 2
app.include_router(schedule.router)
app.include_router(risk.router)
app.include_router(sprints.router)
app.include_router(habits.router)
app.include_router(dashboard.router)

# Phase 3
app.include_router(calls.router)
app.include_router(calendar.router)
app.include_router(planning.router)


@app.get("/health")
async def health():
    return {
        "status": "ok",
        "service": "focusguard-api",
        "phase": 3,
        "version": "3.0.0",
    }
