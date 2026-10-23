import os
from typing import Optional

class Settings:
    PROJECT_NAME: str = "Torob Seller Backend"
    API_V1_STR: str = "/api"
    SECRET_KEY: str = os.getenv("SECRET_KEY", "torob-seller-secret-key-production-change-me-32chars")
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60 * 24 * 7  # 7 days

    # Database
    DATABASE_PATH: str = os.getenv("DATABASE_PATH", os.path.join(os.path.dirname(__file__), "chatapp_seller.db"))
    MAIN_TOROB_DB: str = os.getenv("MAIN_TOROB_DB", os.path.join(os.path.dirname(__file__), "..", "..", "torob_agent.db"))

    # Scheduler Timeouts (in seconds)
    INITIAL_PUSH_TIMEOUT_SECONDS: int = int(os.getenv("INITIAL_PUSH_TIMEOUT_SECONDS", "0"))
    REMINDER_TIMEOUT_SECONDS: int = int(os.getenv("REMINDER_TIMEOUT_SECONDS", "300"))  # 5 minutes
    EXPIRATION_TIMEOUT_SECONDS: int = int(os.getenv("EXPIRATION_TIMEOUT_SECONDS", "600"))  # 10 minutes

    # Push Notification Service
    FCM_ENABLED: bool = os.getenv("FCM_ENABLED", "true").lower() in ("true", "1", "yes")
    FCM_PROJECT_ID: Optional[str] = os.getenv("FCM_PROJECT_ID", "torob-seller-app")
    FCM_SERVICE_ACCOUNT_FILE: Optional[str] = os.getenv("FCM_SERVICE_ACCOUNT_FILE", None)

    # SMS OTP Service
    SMS_PROVIDER: str = os.getenv("SMS_PROVIDER", "mock")  # mock, kavenegar, sms_ir
    SMS_API_KEY: Optional[str] = os.getenv("SMS_API_KEY", None)
    MOCK_OTP_CODE: str = os.getenv("MOCK_OTP_CODE", "12345")

settings = Settings()
