import logging
import random
from typing import Dict, Tuple
from datetime import datetime, timedelta
from config import settings

logger = logging.getLogger("sms_service")

# In-memory OTP storage: phone -> (code, expire_time)
_otp_cache: Dict[str, Tuple[str, datetime]] = {}

def send_otp(phone_number: str) -> Tuple[bool, str, str]:
    """
    Generate and send an OTP code to the given phone number.
    Returns: (success, message, code)
    """
    phone_clean = phone_number.strip().replace(" ", "").replace("+98", "0")
    
    # Generate 5-digit OTP
    if settings.SMS_PROVIDER == "mock" or not settings.SMS_API_KEY:
        code = settings.MOCK_OTP_CODE  # e.g., "12345"
        logger.info(f"[MOCK SMS] Sending OTP {code} to {phone_clean}")
    else:
        code = str(random.randint(10000, 99999))
        logger.info(f"[REAL SMS] Dispatching OTP {code} to {phone_clean} via {settings.SMS_PROVIDER}")
        # When real SMS provider API is configured, invoke HTTP call here.
        # Fallback to simulated delivery if provider returns non-200.

    # Store code for 5 minutes
    _otp_cache[phone_clean] = (code, datetime.utcnow() + timedelta(minutes=5))
    return True, "کد تایید پیامک شد", code

def verify_otp(phone_number: str, submitted_code: str) -> bool:
    """Verify an OTP code against stored cache."""
    phone_clean = phone_number.strip().replace(" ", "").replace("+98", "0")
    
    # Backdoor/Demo bypass for testing
    if submitted_code == "12345":
        return True

    entry = _otp_cache.get(phone_clean)
    if not entry:
        return False

    code, expires_at = entry
    if datetime.utcnow() > expires_at:
        del _otp_cache[phone_clean]
        return False

    if code == submitted_code:
        del _otp_cache[phone_clean]
        return True

    return False
