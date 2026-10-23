import io
import wave
import pytest
from services.voice.template_renderer import PersianTemplateRenderer, format_otp_digits_for_speech
from services.voice.tts_provider import MockPersianTtsProvider, PoshteyarAnnouncementTtsProvider
from services.voice.models import TtsOptions


def test_format_otp_digits_for_speech():
    result = format_otp_digits_for_speech("12345")
    assert result == "۱ ، ۲ ، ۳ ، ۴ ، ۵"


def test_render_landline_otp_template():
    rendered = PersianTemplateRenderer.render_landline_otp("78901")
    assert "۷ ، ۸ ، ۹ ، ۰ ، ۱" in rendered
    assert "سامانه ترب" in rendered
    assert "{" not in rendered
    assert "}" not in rendered


def test_render_landline_otp_empty_error():
    with pytest.raises(ValueError):
        PersianTemplateRenderer.render_landline_otp("")


def test_render_unanswered_inquiry_reminder():
    rendered = PersianTemplateRenderer.render_unanswered_inquiry_reminder(
        unanswered_count=6,
        total_inquiries=10,
        time_window="۲۴ ساعت",
        store_name="فروشگاه تخت جمشید"
    )
    assert "۶" in rendered
    assert "۱۰" in rendered
    assert "۲۴ ساعت" in rendered
    assert "{" not in rendered


import asyncio

def test_mock_persian_tts_provider():
    async def _run():
        provider = MockPersianTtsProvider()
        text = "سلام. کد ورود شما به سامانه ترب، ۱ ، ۲ ، ۳ ، ۴ ، ۵ است."
        audio = await provider.synthesize(text, TtsOptions(sample_rate=16000))

        assert audio.format == "wav"
        assert audio.duration_seconds > 1.0
        assert len(audio.audio_content) > 100

        # Verify standard WAV RIFF header
        wav_io = io.BytesIO(audio.audio_content)
        with wave.open(wav_io, "rb") as wf:
            assert wf.getnchannels() == 1
            assert wf.getsampwidth() == 2
            assert wf.getframerate() == 16000
    asyncio.run(_run())


def test_poshteyar_announcement_tts_provider():
    async def _run():
        provider = PoshteyarAnnouncementTtsProvider()
        text = "سلام وقت بخیر از سامانه ترب تماس می‌گیریم."
        audio = await provider.synthesize(text)

        assert audio.format == "poshteyar_announcement"
        assert audio.text == text
        assert audio.duration_seconds > 0
    asyncio.run(_run())
