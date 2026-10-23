import asyncio
import pytest
from unittest.mock import patch, MagicMock
from services.voice.models import GeneratedAudio, OutboundCallOptions, CallStatus, CallScenario
from services.voice.outbound_call_provider import (
    MockOutboundCallProvider,
    PoshteyarOutboundCallProvider
)


def test_mock_outbound_call_provider():
    async def _run():
        provider = MockOutboundCallProvider()
        audio = GeneratedAudio(
            audio_content=b"test_audio",
            format="wav",
            duration_seconds=5.0,
            text="پیام تستی"
        )
        options = OutboundCallOptions(
            recipient_name="علی رضایی",
            topic="تست",
            scenario=CallScenario.LANDLINE_OTP
        )

        result = await provider.place_call("02188888888", audio, options)

        assert result.status == CallStatus.CALL_REQUESTED
        assert len(provider.dispatched_calls) == 1
        call_record = provider.dispatched_calls[0]
        assert call_record["destination_number"] == "02188888888"
        assert call_record["audio_text"] == "پیام تستی"
    asyncio.run(_run())


def test_mock_outbound_call_provider_simulated_failure():
    async def _run():
        provider = MockOutboundCallProvider()
        provider.force_failure_code = "SIMULATED_BUSY"
        audio = GeneratedAudio(text="تست")

        result = await provider.place_call("02188888888", audio)
        assert result.status == CallStatus.FAILED
        assert result.error_code == "SIMULATED_BUSY"
    asyncio.run(_run())


def test_poshteyar_provider_success():
    async def _run():
        provider = PoshteyarOutboundCallProvider(
            api_url="https://api.poshtyar.com/api/v1/external/calls/originate",
            api_key="poshtyar_live_test_key_12345",
            operator_id="test_operator_id_001"
        )
        audio = GeneratedAudio(text="سلام، کد ورود شما ۱ ، ۲ ، ۳ ، ۴ ، ۵ است.")
        options = OutboundCallOptions(
            recipient_name="فروشگاه تخت جمشید",
            topic="کد ورود",
            outbound_type="announcement",
            scenario=CallScenario.LANDLINE_OTP
        )

        mock_resp = {
            "status_code": 200,
            "body": {
                "status": "success",
                "message": "تماس خروجی با موفقیت برقرار شد",
                "call_id": "call_poshtyar_live_999"
            }
        }

        with patch.object(provider, "_sync_http_post", return_value=mock_resp):
            res = await provider.place_call("02176557624", audio, options)
            assert res.status == CallStatus.CALL_REQUESTED
            assert res.provider == "poshteyar"
            assert res.provider_call_id == "call_poshtyar_live_999"
    asyncio.run(_run())


def test_poshteyar_provider_error_mappings():
    async def _run():
        provider = PoshteyarOutboundCallProvider(
            api_url="https://api.poshtyar.com/api/v1/external/calls/originate",
            api_key="poshtyar_live_test_key",
            operator_id="test_operator_id"
        )
        audio = GeneratedAudio(text="تست")

        # 400 Bad Request
        with patch.object(provider, "_sync_http_post", return_value={
            "status_code": 400,
            "error": {"error": "شماره تماس و اپراتور الزامی است"}
        }):
            res = await provider.place_call("02188888888", audio)
            assert res.status == CallStatus.FAILED
            assert res.error_code == "BAD_REQUEST"

        # 401 Unauthorized
        with patch.object(provider, "_sync_http_post", return_value={
            "status_code": 401,
            "error": {"error": "کلید API نامعتبر است"}
        }):
            res = await provider.place_call("02188888888", audio)
            assert res.status == CallStatus.FAILED
            assert res.error_code == "UNAUTHORIZED"

        # 500 Insufficient Credit
        with patch.object(provider, "_sync_http_post", return_value={
            "status_code": 500,
            "error": {"error": "اعتبار حساب کافی نیست"}
        }):
            res = await provider.place_call("02188888888", audio)
            assert res.status == CallStatus.FAILED
            assert res.error_code == "INSUFFICIENT_CREDIT_OR_SERVER_ERROR"

        # Network Timeout (0)
        with patch.object(provider, "_sync_http_post", return_value={
            "status_code": 0,
            "error": {"message": "timed out"}
        }):
            res = await provider.place_call("02188888888", audio)
            assert res.status == CallStatus.FAILED
            assert res.error_code == "NETWORK_TIMEOUT"
    asyncio.run(_run())
