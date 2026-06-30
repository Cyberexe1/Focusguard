import re
from pydantic import BaseModel, Field, field_validator
from typing import Optional
from enum import Enum


# ── Enums ─────────────────────────────────────────────────────────────────────

class TaskStatus(str, Enum):
    pending = "pending"
    in_progress = "in_progress"
    completed = "completed"


class EventType(str, Enum):
    user_event = "user_event"
    focus_block = "focus_block"
    deadline = "deadline"


class RecurrenceType(str, Enum):
    none = "none"
    daily = "daily"
    weekly = "weekly"


# ── Validators ────────────────────────────────────────────────────────────────

def _validate_phone(v: Optional[str]) -> Optional[str]:
    """Accepts E.164 format (+<country><number>) or empty/None."""
    if not v:
        return v
    if not re.fullmatch(r"\+[1-9]\d{6,14}", v):
        raise ValueError("Phone must be E.164 format, e.g. +918433654259")
    return v


# ── Auth models ───────────────────────────────────────────────────────────────

class RegisterRequest(BaseModel):
    name: str = Field(..., min_length=1, max_length=100)
    email: str = Field(..., min_length=3, max_length=254)
    password: str = Field(..., min_length=8, max_length=128)
    phone: Optional[str] = None

    @field_validator("phone")
    @classmethod
    def validate_phone(cls, v):
        return _validate_phone(v)


class LoginRequest(BaseModel):
    email: str
    password: str


class AuthResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user_id: str
    name: str = ""
    email: str = ""


# ── Task models ───────────────────────────────────────────────────────────────

class CreateTaskRequest(BaseModel):
    raw_text: str = Field(..., min_length=3, max_length=1000)


class UpdateTaskRequest(BaseModel):
    title: Optional[str] = Field(None, max_length=200)
    status: Optional[TaskStatus] = None
    effort_hours: Optional[float] = Field(None, ge=0, le=8760)
    deadline: Optional[str] = None


class TaskResponse(BaseModel):
    task_id: str
    user_id: str
    title: str
    deadline: str
    effort_hours: float
    category: str
    priority_score: int
    priority_rank_reason: str
    status: str
    created_at: str
    updated_at: str
    sub_tasks: list = []
    checkin_streak: int = 0


# ── Sub-task models ───────────────────────────────────────────────────────────

class SubTask(BaseModel):
    id: str = ""
    title: str = Field(..., min_length=1, max_length=200)
    done: bool = False


class AddSubTaskRequest(BaseModel):
    title: str = Field(..., min_length=1, max_length=200)


class UpdateSubTaskRequest(BaseModel):
    done: bool


# ── Daily check-in models ─────────────────────────────────────────────────────

class CheckInRequest(BaseModel):
    note: str = Field("", max_length=500)


# ── Schedule block models ─────────────────────────────────────────────────────

class UserBlock(BaseModel):
    name: str = Field(..., min_length=1, max_length=100)
    startMin: int = Field(..., ge=0, le=1439)   # 0 = 00:00, 1439 = 23:59
    endMin: Optional[int] = Field(None, ge=0, le=1439)
    repeat_days: int = Field(1, ge=1, le=30)

    @field_validator("endMin")
    @classmethod
    def end_after_start(cls, v, info):
        if v is not None and "startMin" in info.data and v <= info.data["startMin"]:
            raise ValueError("endMin must be after startMin")
        return v


class SaveBlocksRequest(BaseModel):
    date: str = Field(..., pattern=r"^\d{4}-\d{2}-\d{2}$")
    blocks: list[UserBlock] = Field(..., min_length=1, max_length=20)
