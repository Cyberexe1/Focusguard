import uuid
from datetime import datetime, timezone
from fastapi import APIRouter, HTTPException, status
from app.models import RegisterRequest, LoginRequest, AuthResponse
from app.services import dynamodb_service as db
from app.services.auth_service import hash_password, verify_password, create_access_token

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/register", response_model=AuthResponse, status_code=status.HTTP_201_CREATED)
async def register(body: RegisterRequest):
    # Check if email already taken
    existing = db.get_user_by_email(body.email.lower().strip())
    if existing:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="An account with this email already exists.",
        )

    user_id = str(uuid.uuid4())
    now = datetime.now(timezone.utc).isoformat()

    user_record = {
        "userId": user_id,
        "email": body.email.lower().strip(),
        "passwordHash": hash_password(body.password),
        "name": body.name.strip(),
        "phone": body.phone or "",
        "createdAt": now,
    }
    db.create_user(user_record)

    token = create_access_token(user_id, body.email.lower().strip())
    return AuthResponse(
        access_token=token,
        user_id=user_id,
        name=body.name.strip(),
        email=body.email.lower().strip(),
    )


@router.post("/login", response_model=AuthResponse)
async def login(body: LoginRequest):
    user = db.get_user_by_email(body.email.lower().strip())

    if not user or not verify_password(body.password, user["passwordHash"]):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid email or password.",
        )

    token = create_access_token(user["userId"], user["email"])
    return AuthResponse(
        access_token=token,
        user_id=user["userId"],
        name=user.get("name", ""),
        email=user.get("email", ""),
    )
