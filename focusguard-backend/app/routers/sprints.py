from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel
from typing import Optional
from app.services.auth_service import get_current_user_id
from app.services import sprint_service

router = APIRouter(prefix="/sprints", tags=["sprints"])


class StartSprintRequest(BaseModel):
    task_id: str
    duration_hours: float = 2.0


class CheckpointRequest(BaseModel):
    progress_made: bool


class EndSprintRequest(BaseModel):
    completion_percent: int = 100


@router.post("", status_code=status.HTTP_201_CREATED)
async def start_sprint(
    body: StartSprintRequest,
    user_id: str = Depends(get_current_user_id),
):
    sprint = sprint_service.start_sprint(user_id, body.task_id, body.duration_hours)
    return sprint


@router.put("/{sprint_id}/checkpoint")
async def log_checkpoint(
    sprint_id: str,
    body: CheckpointRequest,
    user_id: str = Depends(get_current_user_id),
):
    try:
        sprint = sprint_service.log_checkpoint(user_id, sprint_id, body.progress_made)
        # If no progress, return escalation flag so Android can trigger Level 2 push
        return {
            **sprint,
            "escalationRequired": not body.progress_made,
        }
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(e))


@router.put("/{sprint_id}/end")
async def end_sprint(
    sprint_id: str,
    body: EndSprintRequest,
    user_id: str = Depends(get_current_user_id),
):
    try:
        sprint = sprint_service.end_sprint(user_id, sprint_id, body.completion_percent)
        return sprint
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(e))


@router.get("")
async def list_sprints(user_id: str = Depends(get_current_user_id)):
    return sprint_service.get_sprints_for_user(user_id)


@router.get("/{sprint_id}")
async def get_sprint(
    sprint_id: str,
    user_id: str = Depends(get_current_user_id),
):
    sprint = sprint_service.get_sprint(user_id, sprint_id)
    if not sprint:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Sprint not found.")
    return sprint
