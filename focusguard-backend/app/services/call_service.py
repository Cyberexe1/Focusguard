"""
Call Agent — Phase 3

Architecture (matches your existing n8n workflow):

  FocusGuard backend
      │  POST /calls/trigger/{task_id}
      │  → generates Bedrock call script
      │  → sends to n8n webhook (aegisai-alert)
      ▼
  n8n workflow (AegisAI Voice Alert via Twilio)
      │  Prepare Call Payload
      │  → POST http://localhost:8000/api/alerts/n8n-call   ← our new endpoint
      ▼
  FocusGuard /api/alerts/n8n-call
      │  Receives phone + message from n8n
      │  → Places Twilio outbound call with TwiML
      ▼
  Twilio → user's phone (+918433654259)
      │  User says yes/no
      ▼
  POST /webhooks/twilio/response/{task_id}
      │  → parse intent → update schedule
"""

import json
import uuid
import urllib.request
from datetime import datetime, timezone
import boto3
from app.config import settings
from app.services.bedrock_service import get_bedrock_client


# ── DynamoDB ──────────────────────────────────────────────────────────────────

def _db():
    return boto3.resource("dynamodb", region_name=settings.aws_region)


# ── Script generation ─────────────────────────────────────────────────────────

def generate_call_script(task: dict, risk_score: int, user_name: str = "there") -> str:
    """
    Use Bedrock Nova Lite to write a personalized call script.
    Falls back to a template if Bedrock is unavailable.
    """
    try:
        client = get_bedrock_client()
        system = (
            "You are an AI accountability assistant making a phone call. "
            "Be direct, warm, and under 45 seconds when spoken aloud. "
            "End with: 'Would you like me to generate an emergency focus schedule? Say yes or no.'"
        )
        user = (
            f"Write a phone call script for:\n"
            f"- User's name: {user_name}\n"
            f"- Task at risk: {task['title']}\n"
            f"- Deadline: {task.get('deadline', 'soon')[:10]}\n"
            f"- Risk score: {risk_score}/100\n"
            f"- Effort remaining: {task.get('effortHours', 2)}h\n\n"
            f"Return ONLY the spoken text."
        )
        response = client.converse(
            modelId=settings.bedrock_model_id,
            system=[{"text": system}],
            messages=[{"role": "user", "content": [{"text": user}]}],
            inferenceConfig={"maxTokens": 200, "temperature": 0.4},
        )
        return response["output"]["message"]["content"][0]["text"].strip()
    except Exception as e:
        print(f"[call_service] Bedrock script fallback ({e})")
        return (
            f"Hello {user_name}. This is FocusGuard AI. "
            f"Your task '{task.get('title', 'your task')}' is due on "
            f"{task.get('deadline', 'soon')[:10]}. "
            f"There is a {risk_score} percent chance you will miss this deadline. "
            f"Would you like me to generate an emergency focus schedule? Say yes or no."
        )


# ── n8n webhook trigger ───────────────────────────────────────────────────────

def trigger_via_n8n(
    user_phone: str,
    task: dict,
    risk_score: int,
    user_name: str,
    script: str,
) -> dict:
    """
    Send alert payload to your n8n webhook.
    n8n workflow processes it and calls back /api/alerts/n8n-call on our backend.

    Payload matches your n8n 'Prepare Call Payload' node's expected fields:
      phone, alertType, message, severity
    """
    if not settings.n8n_webhook_url:
        raise RuntimeError("N8N_WEBHOOK_URL not configured in .env")

    payload = {
        "phone": user_phone,
        "alertType": "Deadline Risk Alert",
        "message": script,
        "severity": "critical" if risk_score >= 90 else "warning",
        # Extra fields FocusGuard needs back from n8n response
        "taskId": task["taskId"],
        "taskTitle": task["title"],
        "riskScore": risk_score,
        "userName": user_name,
    }

    data = json.dumps(payload).encode()
    headers = {
        "Content-Type": "application/json",
        "X-Webhook-Secret": settings.n8n_webhook_secret,
    }

    req = urllib.request.Request(
        settings.n8n_webhook_url,
        data=data,
        headers=headers,
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=15) as resp:
        result = json.loads(resp.read().decode())

    return result


# ── /api/alerts/n8n-call handler (called by n8n → we place the Twilio call) ──

def place_twilio_call(
    user_phone: str,
    task_id: str,
    user_id: str,
    script: str,
) -> dict:
    """
    Called by n8n's 'Trigger Voice Call' node hitting /api/alerts/n8n-call.
    Places the actual Twilio outbound call.
    """
    if not settings.twilio_account_sid or not settings.twilio_auth_token:
        raise RuntimeError("Twilio credentials not set in .env")

    from twilio.rest import Client
    from twilio.twiml.voice_response import VoiceResponse, Gather

    client = Client(settings.twilio_account_sid, settings.twilio_auth_token)

    # Build TwiML inline so we don't need a public webhook URL during local dev
    vr = VoiceResponse()
    gather = Gather(
        input="speech",
        action=f"{settings.api_public_url}/webhooks/twilio/response/{task_id}",
        method="POST",
        speechTimeout="auto",
        language="en-IN",   # Indian English — matches +91 number
    )
    gather.say(script, voice="Polly.Aditi")   # Polly.Aditi = Indian English female voice
    vr.append(gather)
    vr.say(
        "I didn't catch that. I will review your schedule automatically. Goodbye.",
        voice="Polly.Aditi",
    )
    twiml_str = str(vr)

    call = client.calls.create(
        to=user_phone,
        from_=settings.twilio_phone_number,
        twiml=twiml_str,           # inline TwiML — no public webhook needed
        status_callback=f"{settings.api_public_url}/webhooks/twilio/status/{task_id}",
        status_callback_method="POST",
    )

    call_id = str(uuid.uuid4())
    now = datetime.now(timezone.utc).isoformat()
    record = {
        "userId": user_id,
        "callId": call_id,
        "taskId": task_id,
        "twilioCallSid": call.sid,
        "status": "initiated",
        "callScript": script,
        "userResponse": "",
        "parsedIntent": "",
        "actionTaken": "",
        "timestamp": now,
    }
    _db().Table("focusguard_calls").put_item(Item=record)
    return {"callSid": call.sid, "callId": call_id, "status": "initiated"}


# ── Intent parsing ────────────────────────────────────────────────────────────

def parse_user_intent(speech_result: str, task_id: str) -> str:
    """Classify: accept_schedule | decline | ambiguous"""
    text = (speech_result or "").lower().strip()

    yes_words = {"yes", "yeah", "yep", "sure", "ok", "okay", "please", "do it",
                 "go ahead", "create", "generate", "haan", "ha"}
    no_words  = {"no", "nope", "don't", "not now", "skip", "cancel", "stop",
                 "nahi", "na", "nah"}

    if any(w in text for w in yes_words):
        return "accept_schedule"
    if any(w in text for w in no_words):
        return "decline"

    # Bedrock fallback for ambiguous speech
    try:
        client = get_bedrock_client()
        response = client.converse(
            modelId=settings.bedrock_model_id,
            system=[{"text": "Classify: accept_schedule, decline, or ambiguous. Return only one word."}],
            messages=[{"role": "user", "content": [{"text": f'User said: "{speech_result}"\nIntent:'}]}],
            inferenceConfig={"maxTokens": 8, "temperature": 0.0},
        )
        intent = response["output"]["message"]["content"][0]["text"].strip().lower()
        if intent in ("accept_schedule", "decline", "ambiguous"):
            return intent
    except Exception:
        pass
    return "ambiguous"


# ── Call record helpers ───────────────────────────────────────────────────────

def update_call(user_id: str, call_id: str, updates: dict) -> None:
    table = _db().Table("focusguard_calls")
    updates["updatedAt"] = datetime.now(timezone.utc).isoformat()
    expr_parts, expr_values, expr_names = [], {}, {}
    for k, v in updates.items():
        expr_names[f"#f_{k}"] = k
        expr_values[f":v_{k}"] = v
        expr_parts.append(f"#f_{k} = :v_{k}")
    table.update_item(
        Key={"userId": user_id, "callId": call_id},
        UpdateExpression="SET " + ", ".join(expr_parts),
        ExpressionAttributeNames=expr_names,
        ExpressionAttributeValues=expr_values,
    )


def get_call_by_twilio_sid(twilio_call_sid: str) -> dict | None:
    table = _db().Table("focusguard_calls")
    response = table.scan(
        FilterExpression=boto3.dynamodb.conditions.Attr("twilioCallSid").eq(twilio_call_sid)
    )
    items = response.get("Items", [])
    return items[0] if items else None


def get_calls_for_user(user_id: str) -> list[dict]:
    table = _db().Table("focusguard_calls")
    response = table.query(
        KeyConditionExpression=boto3.dynamodb.conditions.Key("userId").eq(user_id)
    )
    return sorted(response.get("Items", []), key=lambda c: c.get("timestamp", ""), reverse=True)
