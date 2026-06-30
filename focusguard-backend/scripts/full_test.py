"""
Full end-to-end test suite for FocusGuard AI backend.
Tests all 3 phases sequentially using real DynamoDB + Bedrock.

Usage:
    $env:PYTHONPATH="."; python scripts/full_test.py
"""

import json
import time
import uuid
import urllib.request
import urllib.error

BASE = "http://localhost:8000"
PASS = []
FAIL = []


# ── HTTP helpers ──────────────────────────────────────────────────────────────

def req(method: str, path: str, body=None, token: str = "", expect: int = 200) -> dict:
    url = BASE + path
    data = json.dumps(body).encode() if body else None
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    r = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(r) as resp:
            status = resp.status
            content = resp.read().decode()
            result = json.loads(content) if content else {}
            if status != expect:
                raise AssertionError(f"Expected {expect}, got {status}: {content[:200]}")
            return result
    except urllib.error.HTTPError as e:
        content = e.read().decode()
        raise AssertionError(f"HTTP {e.code}: {content[:300]}")


def test(name: str, fn):
    try:
        fn()
        print(f"  ✅  {name}")
        PASS.append(name)
    except Exception as e:
        print(f"  ❌  {name}: {e}")
        FAIL.append((name, str(e)))


# ── State shared across tests ─────────────────────────────────────────────────

state = {}
test_email = f"test_{uuid.uuid4().hex[:8]}@focusguard.ai"
test_password = "TestPass@123"


# ════════════════════════════════════════════════════════════════════════
# PHASE 1
# ════════════════════════════════════════════════════════════════════════

print("\n══════ PHASE 1: Auth + Tasks ══════\n")

def test_health():
    r = req("GET", "/health")
    assert r["status"] == "ok"
    assert r["phase"] == 3
test("GET /health", test_health)


def test_register():
    r = req("POST", "/auth/register", {"name": "Test User", "email": test_email, "password": test_password, "phone": "+15551234567"}, expect=201)
    assert "access_token" in r
    assert "user_id" in r
    state["token"] = r["access_token"]
    state["user_id"] = r["user_id"]
test("POST /auth/register", test_register)


def test_register_duplicate():
    try:
        req("POST", "/auth/register", {"name": "Test User", "email": test_email, "password": test_password}, expect=409)
        PASS.append("duplicate email → 409")
        print("  ✅  POST /auth/register — duplicate → 409")
    except AssertionError as e:
        if "409" not in str(e):
            FAIL.append(("duplicate email → 409", str(e)))
            print(f"  ❌  POST /auth/register — duplicate → 409: {e}")
        else:
            PASS.append("duplicate email → 409")
            print("  ✅  POST /auth/register — duplicate → 409")
test_register_duplicate()


def test_login():
    r = req("POST", "/auth/login", {"email": test_email, "password": test_password})
    assert "access_token" in r
    state["token"] = r["access_token"]
test("POST /auth/login", test_login)


def test_login_wrong_password():
    try:
        req("POST", "/auth/login", {"email": test_email, "password": "WrongPass"}, expect=401)
        print("  ✅  POST /auth/login — wrong password → 401")
        PASS.append("wrong password → 401")
    except AssertionError as e:
        if "401" not in str(e):
            FAIL.append(("wrong password → 401", str(e)))
            print(f"  ❌  {e}")
        else:
            PASS.append("wrong password → 401")
            print("  ✅  POST /auth/login — wrong password → 401")
test_login_wrong_password()


def test_create_task():
    r = req("POST", "/tasks",
            {"raw_text": "Submit hackathon project before Sunday 2 PM urgent"},
            token=state["token"], expect=201)
    assert "task_id" in r
    assert "priority_score" in r
    assert 0 <= r["priority_score"] <= 100
    assert r["title"]
    state["task_id"] = r["task_id"]
    state["task_priority"] = r["priority_score"]
test("POST /tasks (text → Bedrock parse)", test_create_task)


def test_create_voice_task():
    r = req("POST", "/tasks/voice",
            {"raw_text": "Remind me to finish DBMS assignment before Monday morning"},
            token=state["token"], expect=201)
    assert "task_id" in r
    assert r["priority_score"] >= 0
    state["task_id_2"] = r["task_id"]
test("POST /tasks/voice", test_create_voice_task)


def test_get_tasks():
    r = req("GET", "/tasks", token=state["token"])
    assert isinstance(r, list)
    assert len(r) >= 2
    # Verify sorted by priority descending
    scores = [t["priority_score"] for t in r]
    assert scores == sorted(scores, reverse=True), "Tasks not sorted by priority"
test("GET /tasks (sorted by priority)", test_get_tasks)


def test_get_task():
    r = req("GET", f"/tasks/{state['task_id']}", token=state["token"])
    assert r["task_id"] == state["task_id"]
test("GET /tasks/{id}", test_get_task)


def test_update_task():
    r = req("PUT", f"/tasks/{state['task_id']}", {"status": "in_progress"}, token=state["token"])
    assert r["status"] == "in_progress"
test("PUT /tasks/{id} (status update)", test_update_task)


def test_unauthorized():
    try:
        req("GET", "/tasks", expect=403)
        FAIL.append(("no token → 403", "Expected 403 but got 200"))
        print("  ❌  GET /tasks no token → 403: got 200 instead")
    except AssertionError as e:
        if "403" in str(e) or "401" in str(e):
            PASS.append("no token → 401/403")
            print("  ✅  GET /tasks no token → 401/403")
        else:
            FAIL.append(("no token → 401/403", str(e)))
            print(f"  ❌  no token check: {e}")
test_unauthorized()


# ════════════════════════════════════════════════════════════════════════
# PHASE 2
# ════════════════════════════════════════════════════════════════════════

print("\n══════ PHASE 2: Risk + Schedule + Sprint + Habits + Dashboard ══════\n")


def test_risk_single():
    r = req("GET", f"/tasks/{state['task_id']}/risk", token=state["token"])
    assert "risk_score" in r
    assert 0 <= r["risk_score"] <= 100
    assert r["level"] in ("low", "medium", "high", "critical")
    assert "factors" in r
    state["risk_score"] = r["risk_score"]
test("GET /tasks/{id}/risk", test_risk_single)


def test_risk_all():
    r = req("GET", "/tasks/risk/all", token=state["token"])
    assert isinstance(r, list)
    assert len(r) >= 1
    for item in r:
        assert 0 <= item["risk_score"] <= 100
    # Verify sorted by risk descending
    scores = [t["risk_score"] for t in r]
    assert scores == sorted(scores, reverse=True), "Risk scores not sorted"
test("GET /tasks/risk/all (sorted)", test_risk_all)


def test_generate_schedule():
    r = req("POST", "/schedule/generate",
            {"available_hours": 6.0, "peak_hours": "19:00-22:00"},
            token=state["token"])
    assert "date" in r
    assert "schedule" in r
    assert isinstance(r["schedule"], list)
test("POST /schedule/generate (Bedrock planner)", test_generate_schedule)


def test_get_today_schedule():
    r = req("GET", "/schedule/today", token=state["token"])
    assert "date" in r or "message" in r
test("GET /schedule/today", test_get_today_schedule)


def test_emergency_plan():
    r = req("POST", "/schedule/emergency",
            {"critical_task_id": state["task_id"]},
            token=state["token"])
    assert "critical_task_id" in r or "plan" in r
test("POST /schedule/emergency", test_emergency_plan)


def test_start_sprint():
    r = req("POST", "/sprints",
            {"task_id": state["task_id"], "duration_hours": 2.0},
            token=state["token"], expect=201)
    assert "sprintId" in r
    assert r["status"] == "active"
    state["sprint_id"] = r["sprintId"]
test("POST /sprints (start)", test_start_sprint)


def test_sprint_checkpoint():
    r = req("PUT", f"/sprints/{state['sprint_id']}/checkpoint",
            {"progress_made": True},
            token=state["token"])
    assert "checkpoints" in r or "sprintId" in r
test("PUT /sprints/{id}/checkpoint", test_sprint_checkpoint)


def test_end_sprint():
    r = req("PUT", f"/sprints/{state['sprint_id']}/end",
            {"completion_percent": 75},
            token=state["token"])
    assert r.get("status") == "completed" or r.get("completionPercent") == 75
test("PUT /sprints/{id}/end", test_end_sprint)


def test_list_sprints():
    r = req("GET", "/sprints", token=state["token"])
    assert isinstance(r, list)
    assert len(r) >= 1
test("GET /sprints", test_list_sprints)


def test_habit_insights():
    r = req("GET", "/habits/insights", token=state["token"])
    assert "peakProductivityHours" in r
    assert "consistencyScore" in r
    assert "recommendations" in r
    assert isinstance(r["recommendations"], list)
test("GET /habits/insights", test_habit_insights)


def test_daily_dashboard():
    r = req("GET", "/dashboard/daily", token=state["token"])
    assert "tasksCompleted" in r
    assert "focusHours" in r
    assert "productivityScore" in r
    assert 0 <= r["productivityScore"] <= 100
test("GET /dashboard/daily", test_daily_dashboard)


def test_weekly_dashboard():
    r = req("GET", "/dashboard/weekly", token=state["token"])
    assert "weekStart" in r
    assert "weeklyFocusHours" in r
test("GET /dashboard/weekly", test_weekly_dashboard)


# ════════════════════════════════════════════════════════════════════════
# PHASE 3
# ════════════════════════════════════════════════════════════════════════

print("\n══════ PHASE 3: Calendar + Calls + Planning ══════\n")


def test_create_calendar_event():
    # Use a future date so it's not filtered out
    from datetime import datetime, timezone, timedelta
    future = (datetime.now(timezone.utc) + timedelta(days=2)).strftime("%Y-%m-%d")
    r = req("POST", "/calendar/events", {
        "title": "CS Lecture",
        "start_time": f"{future}T10:00:00",
        "end_time":   f"{future}T12:00:00",
        "event_type": "user_event",
        "description": "Database Systems lecture",
    }, token=state["token"], expect=201)
    assert "eventId" in r
    assert r["title"] == "CS Lecture"
    assert r["eventType"] == "user_event"
    state["event_id"] = r["eventId"]
    state["future_date"] = future
test("POST /calendar/events", test_create_calendar_event)


def test_get_calendar_events():
    r = req("GET", "/calendar/events", token=state["token"])
    assert "events" in r
    assert "eventCount" in r
    assert r["eventCount"] >= 1
test("GET /calendar/events", test_get_calendar_events)


def test_calendar_conflict_check():
    future = state.get("future_date", "2099-01-01")
    r = req("POST", "/calendar/conflicts", {
        "schedule_blocks": [
            {"startTime": "10:00", "endTime": "11:00", "taskId": state["task_id"],
             "taskTitle": "Study", "sessionType": "deep_work"}
        ],
        "date": future,
    }, token=state["token"])
    assert "conflicts" in r
    assert "conflictCount" in r
    # CS Lecture is 10:00-12:00 on future_date so this SHOULD conflict
    assert r["conflictCount"] >= 1, f"Expected conflict, got 0. Date: {future}"
test("POST /calendar/conflicts (conflict detected)", test_calendar_conflict_check)


def test_calendar_sync_schedule():
    future = state.get("future_date", "2099-01-01")
    r = req("POST", "/calendar/sync", {
        "schedule_blocks": [
            {"startTime": "19:00", "endTime": "21:00", "taskId": state["task_id"],
             "taskTitle": "Hackathon Work", "sessionType": "deep_work"},
            {"startTime": "21:10", "endTime": "22:00", "taskId": state["task_id_2"],
             "taskTitle": "DBMS Assignment", "sessionType": "deep_work"},
        ],
        "date": future,
    }, token=state["token"])
    assert "blocksWritten" in r
    assert r["blocksWritten"] >= 1
test("POST /calendar/sync (write focus blocks)", test_calendar_sync_schedule)


def test_ics_export():
    import urllib.request
    url = BASE + "/calendar/export.ics?days=30"
    headers_req = {"Authorization": f"Bearer {state['token']}"}
    r2 = urllib.request.Request(url, headers=headers_req)
    with urllib.request.urlopen(r2) as resp:
        content = resp.read().decode("utf-8", errors="replace")
    assert "BEGIN:VCALENDAR" in content, f"Missing VCALENDAR. Got: {content[:200]}"
    assert "END:VCALENDAR" in content
test("GET /calendar/export.ics (valid ICS format)", test_ics_export)


def test_update_calendar_event():
    r = req("PUT", f"/calendar/events/{state['event_id']}",
            {"title": "CS Lecture — Updated"},
            token=state["token"])
    assert r.get("title") == "CS Lecture — Updated"
test("PUT /calendar/events/{id}", test_update_calendar_event)


def test_weekly_plan():
    r = req("POST", "/planning/week", token=state["token"])
    assert "schedules_generated" in r
    assert "risk_scores" in r
    assert len(r["schedules_generated"]) == 7
test("POST /planning/week (full autonomous plan, 7 days)", test_weekly_plan)


def test_planning_conflicts():
    r = req("GET", "/planning/conflicts", token=state["token"])
    assert "conflicts" in r
test("GET /planning/conflicts", test_planning_conflicts)


def test_call_history():
    r = req("GET", "/calls/history", token=state["token"])
    assert isinstance(r, list)
test("GET /calls/history", test_call_history)


def test_delete_calendar_event():
    req("DELETE", f"/calendar/events/{state['event_id']}", token=state["token"], expect=204)
test("DELETE /calendar/events/{id}", test_delete_calendar_event)


def test_delete_task():
    req("DELETE", f"/tasks/{state['task_id_2']}", token=state["token"], expect=204)
test("DELETE /tasks/{id}", test_delete_task)


# ════════════════════════════════════════════════════════════════════════
# SUMMARY
# ════════════════════════════════════════════════════════════════════════

print()
print("═" * 55)
print(f"  PASSED : {len(PASS)}")
print(f"  FAILED : {len(FAIL)}")
print("═" * 55)

if FAIL:
    print("\nFailed tests:")
    for name, reason in FAIL:
        print(f"  ❌  {name}")
        print(f"      {reason}")
    import sys; sys.exit(1)
else:
    print("\n  All tests passed. Project is fully operational.")
