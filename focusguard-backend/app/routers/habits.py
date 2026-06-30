from datetime import datetime, timezone
from fastapi import APIRouter, Depends
from app.services.auth_service import get_current_user_id
from app.services import dynamodb_service as db
from app.services import habit_service, sprint_service

router = APIRouter(prefix="/habits", tags=["habits"])


@router.get("/insights")
async def get_habit_insights(user_id: str = Depends(get_current_user_id)):
    tasks = db.get_tasks_for_user(user_id)
    sprints = sprint_service.get_sprints_for_user(user_id)

    insights = habit_service.analyze_habits(tasks, sprints)

    # Cache insights in DynamoDB (1 record per day per user)
    from app.services.dynamodb_service import get_dynamodb
    today = datetime.now(timezone.utc).strftime("%Y-%m-%d")
    habit_service.save_habit_record(get_dynamodb(), user_id, today, insights)

    return insights
