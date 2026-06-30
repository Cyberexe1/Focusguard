from pydantic import BaseModel, EmailStr, Field
from typing import Optional
from enum import Enum


# ── Enums ─────────────────────────────────────────────────────────────────────

class TaskStatus(str, Enum):
    pending = "pending"
    in_progress = "in_progress"
    completed = "completed"


# ── Auth models ───────────────────────────────────────────────────────────────

class RegisterRequest(BaseModel):
    name: str = Field(..., min_length=1, max_length=100)
    email: str = Field(..., min_length=3, max_length=254)
    password: str = Field(..., min_length=8, max_length=128)
    phone: Optional[str] = None


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
    """
    Raw text describing the task in natural language.
    Bedrock will parse it into structured fields.
    """
    raw_text: str = Field(..., min_length=3, max_length=1000)


class UpdateTaskRequest(BaseModel):
    title: Optional[str] = None
    status: Optional[TaskStatus] = None
    effort_hours: Optional[float] = None
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
    """User confirms they worked on their tasks today."""
    note: str = ""
