import uuid
import pytest
from datetime import datetime, timedelta
from fastapi.testclient import TestClient
from main import app
from database import get_db
from services.voice.inquiry_reminder_policy import reminder_policy
from services.voice.voice_service import voice_service
from services.voice.models import CallScenario, CallStatus

client = TestClient(app)


def create_test_store_with_inquiries(
    shop_id: str,
    total_inquiries: int,
    unanswered_count: int,
    is_open: int = 1,
    holiday_mode: int = 0,
    voice_reminders_enabled: int = 1,
    phone: str = "02188889900"
):
    """Seed test shop and inquiry data for policy evaluation."""
    conn = get_db()
    cursor = conn.cursor()

    # Insert / update shop
    cursor.execute("""
    INSERT OR REPLACE INTO shop_settings (
        shop_id, shop_name, address, phone, is_open, open_time, close_time,
        accept_inquiries, holiday_mode, voice_reminders_enabled
    ) VALUES (?, 'فروشگاه تستی سناریو ب', 'تهران', ?, ?, '00:00', '23:59', 1, ?, ?)
    """, (shop_id, phone, is_open, holiday_mode, voice_reminders_enabled))

    # Clean existing inquiries and call logs for this test store
    cursor.execute("DELETE FROM inquiries WHERE shop_id = ?", (shop_id,))
    cursor.execute("DELETE FROM outbound_call_logs WHERE shop_id = ?", (shop_id,))

    now = datetime.utcnow()
    # Insert answered inquiries
    answered_count = max(0, total_inquiries - unanswered_count)
    for i in range(answered_count):
        inq_id = f"inq_ans_{shop_id}_{i}_{uuid.uuid4().hex[:6]}"
        cursor.execute("""
        INSERT INTO inquiries (
            inquiry_id, tracking_code, shop_id, product_title, customer_name,
            inquiry_type, status, created_at, expires_at
        ) VALUES (?, ?, ?, 'کالای پاسخ‌داده‌شده', 'مشتری', 'price_and_stock', 'ANSWERED', ?, ?)
        """, (inq_id, f"TRB-A-{uuid.uuid4().hex[:6]}", shop_id, now.strftime("%Y-%m-%d %H:%M:%S"), (now + timedelta(minutes=10)).strftime("%Y-%m-%d %H:%M:%S")))

    # Insert unanswered (expired) inquiries
    for i in range(unanswered_count):
        inq_id = f"inq_exp_{shop_id}_{i}_{uuid.uuid4().hex[:6]}"
        cursor.execute("""
        INSERT INTO inquiries (
            inquiry_id, tracking_code, shop_id, product_title, customer_name,
            inquiry_type, status, created_at, expires_at
        ) VALUES (?, ?, ?, 'کالای منقضی‌شده', 'مشتری', 'price_and_stock', 'EXPIRED', ?, ?)
        """, (inq_id, f"TRB-E-{uuid.uuid4().hex[:6]}", shop_id, now.strftime("%Y-%m-%d %H:%M:%S"), (now + timedelta(minutes=10)).strftime("%Y-%m-%d %H:%M:%S")))

    conn.commit()
    conn.close()


def test_scenario_b_ineligible_due_to_low_total_inquiries():
    """Shop with fewer inquiries than minimum threshold (10) must be ineligible."""
    shop_id = "shop_test_low_total"
    create_test_store_with_inquiries(shop_id, total_inquiries=5, unanswered_count=4)

    eval_res = reminder_policy.evaluate_store(shop_id)
    assert eval_res.eligible is False
    assert "کمتر از حداقل آستانه" in eval_res.reason


def test_scenario_b_ineligible_due_to_low_unanswered_rate():
    """Shop with high response rate (e.g. 10 total, 2 unanswered = 20%) must be ineligible."""
    shop_id = "shop_test_good_rate"
    create_test_store_with_inquiries(shop_id, total_inquiries=10, unanswered_count=2)

    eval_res = reminder_policy.evaluate_store(shop_id)
    assert eval_res.eligible is False
    assert "کمتر از حد نصاب" in eval_res.reason or "کمتر از آستانه مجاز" in eval_res.reason


def test_scenario_b_ineligible_when_opted_out():
    """Shop that opted out (voice_reminders_enabled = 0) must NOT receive reminder calls."""
    shop_id = "shop_test_opted_out"
    create_test_store_with_inquiries(
        shop_id,
        total_inquiries=12,
        unanswered_count=8,
        voice_reminders_enabled=0
    )

    eval_res = reminder_policy.evaluate_store(shop_id)
    assert eval_res.eligible is False
    assert "غیرفعال" in eval_res.reason or "Opt-Out" in eval_res.reason


def test_scenario_b_eligible_store_meets_all_thresholds():
    """Shop with 12 inquiries, 8 unanswered (66.7% rate >= 60%), open, and not opted out is eligible."""
    shop_id = "shop_test_eligible"
    create_test_store_with_inquiries(shop_id, total_inquiries=12, unanswered_count=8)

    eval_res = reminder_policy.evaluate_store(shop_id)
    assert eval_res.eligible is True
    assert eval_res.unanswered_count == 8
    assert eval_res.total_inquiries == 12


import asyncio

def test_scenario_b_dispatch_reminder_call():
    """Dispatching voice reminder call logs record in DB and places call via provider."""
    async def _run():
        shop_id = "shop_test_dispatch"
        create_test_store_with_inquiries(shop_id, total_inquiries=15, unanswered_count=10)

        eval_res = reminder_policy.evaluate_store(shop_id)
        assert eval_res.eligible is True

        call_result = await voice_service.dispatch_unanswered_inquiry_reminder(eval_res)
        assert call_result is not None
        assert call_result.status == CallStatus.CALL_REQUESTED

        # Verify cooldown is immediately triggered for subsequent evaluation
        second_eval = reminder_policy.evaluate_store(shop_id)
        assert second_eval.eligible is False
        assert "کول‌داون" in second_eval.reason
    asyncio.run(_run())


def test_scenario_b_admin_evaluate_endpoint():
    """Admin endpoint evaluates stores and returns metrics."""
    shop_id = "shop_test_admin_eval"
    create_test_store_with_inquiries(shop_id, total_inquiries=10, unanswered_count=7)

    resp = client.post(f"/api/admin/voice-reminders/evaluate?shop_id={shop_id}&dispatch=false")
    assert resp.status_code == 200
    data = resp.json()
    assert data["evaluated_count"] == 1
    assert data["eligible_count"] == 1
    assert data["results"][0]["unanswered_count"] == 7
