"""
Voice Call Router — Phase 3

Flow:
  1. POST /calls/trigger/{task_id}
       → generates Bedrock script
       → sends to n8n webhook (your AegisAI Voice Alert workflow)

  2. n8n workflow runs:
       Prepare Call Payload → POST /api/alerts/n8n-call (this backend)

  3. POST /api/alerts/n8n-call
       → places Twilio call with inline TwiML (no public webhook needed for script)
       → returns { callSid, callId }

  4. User speaks → Twilio STT → POST /webhooks/twilio/response/{task_id}
       → parse intent → update schedule

  5. POST /webhooks/twilio/status/{task_id}
       → update call record status in DynamoDB
"""

from fastapi import APIRouter, Depends, HTTPException, Request, Form, Response, status
from pydantic import BaseModel, Field
from app.services.auth_service import get_current_user_id
from app.services import dynamodb_service as db
from app.services import call_service, schedule_service
from app.services.risk_service import compute_risk_score
from app.config import settings

router = APIRouter(tags=["calls"])


# ── 1. Manual trigger — FocusGuard → n8n ─────────────────────────────────────

@router.post("/calls/trigger/{task_id}", status_code=status.HTTP_201_CREATED)
async def trigger_call(
    task_id: str,
    user_id: str = Depends(get_current_user_id),
):
    """
    Manually trigger an AI accountability call for a task.
    Sends payload to your n8n webhook which calls back /api/alerts/n8n-call.
    """
    task = db.get_task(user_id, task_id)
    if not task:
        raise HTTPException(status_code=404, detail="Task not found.")

    user = db.get_user_by_id(user_id)
    if not user:
        raise HTTPException(status_code=404, detail="User not found.")

    # Use user's registered phone, fall back to DEFAULT_ALERT_PHONE from .env
    phone = user.get("phone", "") or settings.default_alert_phone
    if not phone:
        raise HTTPException(
            status_code=400,
            detail="No phone number found. Add phone to your profile or set DEFAULT_ALERT_PHONE in .env",
        )

    user_name = user.get("name", "there").split()[0]
    all_tasks = db.get_tasks_for_user(user_id)
    risk_data = compute_risk_score(task, all_tasks)

    # Generate personalised call script via Bedrock
    script = call_service.generate_call_script(task, risk_data["risk_score"], user_name)

    # Send to n8n webhook — n8n will call back /api/alerts/n8n-call
    try:
        n8n_response = call_service.trigger_via_n8n(phone, task, risk_data["risk_score"], user_name, script)
    except Exception as e:
        # n8n not running? Fall back to direct Twilio call
        print(f"[calls] n8n unavailable ({e}), calling Twilio directly.")
        result = call_service.place_twilio_call(phone, task_id, user_id, script)
        return {**result, "taskId": task_id, "riskScore": risk_data["risk_score"], "method": "direct_twilio"}

    return {
        "n8nResponse": n8n_response,
        "taskId": task_id,
        "riskScore": risk_data["risk_score"],
        "phone": phone,
        "method": "n8n_workflow",
        "message": "Alert sent to n8n. Voice call will be placed shortly.",
    }


# ── 2. n8n calls back here to place the actual Twilio call ───────────────────

class N8nCallRequest(BaseModel):
    phone: str
    alertType: str = "Deadline Risk Alert"
    message: str = Field(..., min_length=1, max_length=2000)
    severity: str = "critical"
    taskId: str = ""
    userName: str = "User"


@router.post("/api/alerts/n8n-call")
async def n8n_call_handler(request: Request, body: N8nCallRequest):
    """
    Called by n8n's 'Trigger Voice Call' HTTP Request node.
    Requires X-Webhook-Secret header to match N8N_WEBHOOK_SECRET env var.
    """
    # ── Validate webhook secret ───────────────────────────────────────────
    secret = request.headers.get("X-Webhook-Secret", "")
    if not settings.n8n_webhook_secret or secret != settings.n8n_webhook_secret:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or missing webhook secret.",
        )

    # ── Validate phone number ─────────────────────────────────────────────
    import re
    if not re.fullmatch(r"\+[1-9]\d{6,14}", body.phone):
        raise HTTPException(status_code=400, detail="Invalid phone number format.")

    task_id = body.taskId or "unknown"

    # Resolve userId from call records or task lookup
    user_id = "system"
    if task_id != "unknown":
        try:
            from app.services.dynamodb_service import get_dynamodb, get_tasks_table
            # Scan focusguard_calls for an existing record with this taskId
            table = get_dynamodb().Table("focusguard_calls")
            resp = table.scan(
                FilterExpression=boto3.dynamodb.conditions.Attr("taskId").eq(task_id),
                Limit=1,
            )
            items = resp.get("Items", [])
            if items:
                user_id = items[0].get("userId", "system")
        except Exception:
            pass

    try:
        result = call_service.place_twilio_call(
            user_phone=body.phone,
            task_id=task_id,
            user_id=user_id,
            script=body.message,
        )
        return {"success": True, "callSid": result["callSid"], "callId": result["callId"]}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Twilio call failed: {e}")


# ── 3. Call history ───────────────────────────────────────────────────────────

@router.get("/calls/history")
async def call_history(user_id: str = Depends(get_current_user_id)):
    return call_service.get_calls_for_user(user_id)


# ── 4. Twilio webhooks ────────────────────────────────────────────────────────

@router.post("/webhooks/twilio/response/{task_id}")
async def handle_twilio_response(
    task_id: str,
    request: Request,
    SpeechResult: str = Form(default=""),
    CallSid: str = Form(default=""),
):
    """Twilio sends STT result here after user speaks."""
    from twilio.twiml.voice_response import VoiceResponse
    from twilio.request_validator import RequestValidator

    # ── Validate Twilio signature ─────────────────────────────────────────
    validator = RequestValidator(settings.twilio_auth_token)
    signature = request.headers.get("X-Twilio-Signature", "")
    url = str(request.url)
    form_data = dict(await request.form())
    if settings.twilio_auth_token and not validator.validate(url, form_data, signature):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Invalid Twilio signature.")

    intent = call_service.parse_user_intent(SpeechResult, task_id)
    call_record = call_service.get_call_by_twilio_sid(CallSid) or {}
    user_id = call_record.get("userId", "")
    action_taken = ""

    vr = VoiceResponse()

    if intent == "accept_schedule":
        action_taken = "emergency_plan_generated"
        if user_id and user_id != "system":
            all_tasks = db.get_tasks_for_user(user_id)
            task = db.get_task(user_id, task_id)
            if task:
                other = [t for t in all_tasks if t["taskId"] != task_id and t.get("status") != "completed"]
                schedule_service.generate_emergency_plan(task, other)
        vr.say(
            "Perfect. I have generated an emergency focus schedule. "
            "Open the FocusGuard app to see your plan. You can do this!",
            voice="Polly.Aditi",
        )

    elif intent == "decline":
        action_taken = "declined_re_escalate_90min"
        vr.say(
            "Understood. I will check back in 90 minutes. "
            "Please try to make progress. Goodbye.",
            voice="Polly.Aditi",
        )

    else:
        action_taken = "ambiguous_no_action"
        vr.say(
            "I wasn't sure about your response. "
            "Open FocusGuard to manage your schedule manually. Goodbye.",
            voice="Polly.Aditi",
        )

    # Update call record
    if user_id and call_record.get("callId"):
        call_service.update_call(user_id, call_record["callId"], {
            "userResponse": SpeechResult,
            "parsedIntent": intent,
            "actionTaken": action_taken,
            "status": "completed",
        })

    return Response(content=str(vr), media_type="application/xml")


@router.post("/webhooks/twilio/status/{task_id}")
async def twilio_status_update(
    task_id: str,
    request: Request,
    CallSid: str = Form(default=""),
    CallStatus: str = Form(default=""),
):
    """Twilio sends call lifecycle updates here (ringing, answered, completed, failed)."""
    from twilio.request_validator import RequestValidator

    # ── Validate Twilio signature ─────────────────────────────────────────
    validator = RequestValidator(settings.twilio_auth_token)
    signature = request.headers.get("X-Twilio-Signature", "")
    url = str(request.url)
    form_data = dict(await request.form())
    if settings.twilio_auth_token and not validator.validate(url, form_data, signature):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Invalid Twilio signature.")

    call_record = call_service.get_call_by_twilio_sid(CallSid)
    if call_record:
        call_service.update_call(
            call_record["userId"],
            call_record["callId"],
            {"status": CallStatus.lower()},
        )
    return Response(content="", media_type="text/plain")
