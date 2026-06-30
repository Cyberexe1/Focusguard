from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",   # ignore unknown env vars Lambda injects
    )

    # AWS — credentials optional (Lambda uses IAM role automatically)
    # Use non-standard names so pydantic_settings does NOT accidentally pick up
    # Lambda's injected AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY env vars,
    # which would be passed to boto3 without AWS_SESSION_TOKEN and cause
    # UnrecognizedClientException on every call.
    aws_region: str = "us-east-1"
    aws_region_name: str = ""          # Lambda injects AWS_REGION — alias it
    fg_aws_access_key_id: str = ""     # only set this for local dev if needed
    fg_aws_secret_access_key: str = "" # only set this for local dev if needed

    @property
    def region(self) -> str:
        """Returns whichever region variable is set."""
        return self.aws_region_name or self.aws_region

    # DynamoDB
    dynamodb_users_table: str = "focusguard_users"
    dynamodb_tasks_table: str = "focusguard_tasks"

    # Bedrock
    bedrock_model_id: str = "amazon.nova-lite-v1:0"

    # JWT
    jwt_secret: str = "change_me_to_a_long_random_string_at_least_32_chars"
    jwt_algorithm: str = "HS256"
    jwt_expire_days: int = 7

    # ── Phase 3 ───────────────────────────────────────────────────────────
    twilio_account_sid: str = ""
    twilio_auth_token: str = ""
    twilio_phone_number: str = ""
    default_alert_phone: str = ""

    # n8n webhook URL
    n8n_webhook_url: str = "http://localhost:5678/webhook/focusguard-alert"
    n8n_webhook_secret: str = ""

    # Public URL for Twilio callbacks
    api_public_url: str = "http://localhost:8000"


settings = Settings()
