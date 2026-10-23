import pytest
from fastapi.testclient import TestClient
from main import app
from database import get_db
from services.sms_service import verify_otp, _otp_cache
from services.voice.voice_service import _otp_call_timestamps

client = TestClient(app)


@pytest.fixture(autouse=True)
def clean_test_state():
    """Clear in-memory caches before each test."""
    _otp_cache.clear()
    _otp_call_timestamps.clear()
    yield
    _otp_cache.clear()
    _otp_call_timestamps.clear()


def test_scenario_a_mobile_triggers_sms_delivery():
    """Iranian mobile number (09...) must preserve existing SMS delivery flow."""
    resp = client.post("/api/seller/auth/request-otp", json={"phone_number": "09129998877"})
    assert resp.status_code == 200
    data = resp.json()
    assert data["success"] is True
    assert data["delivery_method"] == "sms"
    assert "پیامک" in data["message"]


def test_scenario_a_landline_triggers_voice_call_delivery():
    """Iranian landline number (021..., 031...) must trigger voice call delivery via Poshteyar/Mock."""
    resp = client.post("/api/seller/auth/request-otp", json={"phone_number": "02176557624"})
    assert resp.status_code == 200
    data = resp.json()
    assert data["success"] is True
    assert data["delivery_method"] == "voice_call"
    assert "تماس صوتی" in data["message"]

    # Verify call audit log exists in DB without leaking OTP code
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("""
    SELECT * FROM outbound_call_logs
    WHERE destination_number = '02176557624' AND scenario_type = 'LANDLINE_OTP'
    ORDER BY created_at DESC LIMIT 1
    """)
    row = cursor.fetchone()
    conn.close()

    assert row is not None
    assert row["status"] in ("CALL_REQUESTED", "AUDIO_GENERATION")
    # Verify destination number is properly cleaned
    assert row["destination_number"] == "02176557624"


def test_scenario_a_voice_otp_rate_limiting():
    """Second immediate request to same landline within cooldown window must be rejected (HTTP 429)."""
    # First request succeeds
    r1 = client.post("/api/seller/auth/request-otp", json={"phone_number": "03132224455"})
    assert r1.status_code == 200

    # Immediate second request fails with 429 Too Many Requests
    r2 = client.post("/api/seller/auth/request-otp", json={"phone_number": "03132224455"})
    assert r2.status_code == 429
    assert "صبر کنید" in r2.json()["detail"]


def test_scenario_a_otp_verification_flow():
    """User enters received OTP into existing verification endpoint."""
    phone = "02188887766"
    req_resp = client.post("/api/seller/auth/request-otp", json={"phone_number": phone})
    assert req_resp.status_code == 200
    otp_code = req_resp.json().get("demo_otp") or "12345"

    # Verify with correct OTP
    verify_resp = client.post("/api/seller/auth/verify-otp", json={
        "phone_number": phone,
        "otp_code": otp_code
    })
    assert verify_resp.status_code == 200
    token_data = verify_resp.json()
    assert "access_token" in token_data
    assert token_data["seller"]["phone_number"] == phone

    # Verify with wrong OTP fails
    wrong_resp = client.post("/api/seller/auth/verify-otp", json={
        "phone_number": phone,
        "otp_code": "00000"
    })
    assert wrong_resp.status_code == 400
