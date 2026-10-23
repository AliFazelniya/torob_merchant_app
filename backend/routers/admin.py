import uuid
from datetime import datetime, timedelta
from typing import List, Optional
from fastapi import APIRouter, HTTPException
from database import get_db
from models import CreateTestInquiryRequest, InquiryDto, PushLogDto, SchedulerConfigDto
from services.scheduler import scheduler
from services.working_hours import is_shop_effectively_open

router = APIRouter(prefix="/admin", tags=["Admin Control Panel"])

@router.get("/inquiries", response_model=List[InquiryDto])
def get_all_inquiries(limit: int = 50):
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("""
    SELECT * FROM inquiries
    ORDER BY created_at DESC
    LIMIT ?
    """, (limit,))
    rows = cursor.fetchall()
    conn.close()

    now = datetime.utcnow()
    results = []
    for r in rows:
        expires_str = r["expires_at"]
        try:
            exp_dt = datetime.fromisoformat(expires_str.replace("Z", ""))
        except Exception:
            exp_dt = datetime.strptime(expires_str, "%Y-%m-%d %H:%M:%S")
        rem_sec = max(0, int((exp_dt - now).total_seconds()))

        results.append(InquiryDto(
            inquiry_id=r["inquiry_id"],
            tracking_code=r["tracking_code"],
            shop_id=r["shop_id"],
            product_id=r["product_id"],
            product_title=r["product_title"],
            product_image_url=r["product_image_url"],
            category_name=r["category_name"],
            recorded_price_toman=r["recorded_price_toman"],
            customer_name=r["customer_name"],
            customer_phone=r["customer_phone"],
            inquiry_type=r["inquiry_type"],
            user_note=r["user_note"],
            status=r["status"],
            created_at=r["created_at"],
            expires_at=r["expires_at"],
            remaining_seconds=rem_sec
        ))
    return results

@router.post("/inquiries/create-test", response_model=InquiryDto)
async def create_test_inquiry(payload: CreateTestInquiryRequest):
    """
    Simulates customer sending an inquiry to a store.
    Triggers Time Zero flow: registers inquiry and dispatches initial push.
    """
    conn = get_db()
    cursor = conn.cursor()

    inquiry_id = f"inq_{uuid.uuid4().hex[:10]}"
    tracking_code = f"TRB-{uuid.uuid4().hex[:6].upper()}"
    now = datetime.utcnow()
    now_str = now.strftime("%Y-%m-%d %H:%M:%S")
    
    # Expiration is default 10 minutes from now (or fetched from config)
    _, expiry_secs = scheduler.get_timeouts()
    expires_dt = now + timedelta(seconds=expiry_secs)
    expires_str = expires_dt.strftime("%Y-%m-%d %H:%M:%S")

    cursor.execute("""
    INSERT INTO inquiries (
        inquiry_id, tracking_code, shop_id, product_id, product_title,
        category_name, recorded_price_toman, customer_name, customer_phone,
        inquiry_type, user_note, status, created_at, expires_at
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?)
    """, (
        inquiry_id, tracking_code, payload.shop_id, payload.product_id,
        payload.product_title, payload.category_name, payload.recorded_price_toman,
        payload.customer_name, payload.customer_phone, payload.inquiry_type,
        payload.user_note, now_str, expires_str
    ))
    conn.commit()
    conn.close()

    # Trigger Time Zero schedule and push
    await scheduler.register_inquiry_time_zero(
        inquiry_id=inquiry_id,
        shop_id=payload.shop_id,
        product_title=payload.product_title
    )

    return InquiryDto(
        inquiry_id=inquiry_id,
        tracking_code=tracking_code,
        shop_id=payload.shop_id,
        product_id=payload.product_id,
        product_title=payload.product_title,
        product_image_url=None,
        category_name=payload.category_name,
        recorded_price_toman=payload.recorded_price_toman,
        customer_name=payload.customer_name,
        customer_phone=payload.customer_phone,
        inquiry_type=payload.inquiry_type,
        user_note=payload.user_note,
        status="ACTIVE",
        created_at=now_str,
        expires_at=expires_str,
        remaining_seconds=expiry_secs
    )

@router.get("/push/logs", response_model=List[PushLogDto])
def get_all_push_logs(limit: int = 100):
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("""
    SELECT * FROM push_audit_logs
    ORDER BY dispatched_at DESC
    LIMIT ?
    """, (limit,))
    rows = cursor.fetchall()
    conn.close()

    return [
        PushLogDto(
            log_id=r["log_id"],
            inquiry_id=r["inquiry_id"],
            shop_id=r["shop_id"],
            fcm_token=r["fcm_token"],
            notification_type=r["notification_type"],
            title=r["title"],
            body=r["body"],
            payload_json=r["payload_json"],
            status=r["status"],
            error_detail=r["error_detail"],
            dispatched_at=r["dispatched_at"]
        ) for r in rows
    ]

@router.get("/scheduler/config", response_model=SchedulerConfigDto)
def get_scheduler_config():
    reminder_secs, expiry_secs = scheduler.get_timeouts()
    return SchedulerConfigDto(
        reminder_timeout_seconds=reminder_secs,
        expiration_timeout_seconds=expiry_secs
    )

@router.put("/scheduler/config", response_model=SchedulerConfigDto)
def update_scheduler_config(payload: SchedulerConfigDto):
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("""
    UPDATE scheduler_configs
    SET config_value = ?, updated_at = CURRENT_TIMESTAMP
    WHERE config_key = 'reminder_timeout_seconds'
    """, (str(payload.reminder_timeout_seconds),))
    cursor.execute("""
    UPDATE scheduler_configs
    SET config_value = ?, updated_at = CURRENT_TIMESTAMP
    WHERE config_key = 'expiration_timeout_seconds'
    """, (str(payload.expiration_timeout_seconds),))
    conn.commit()
    conn.close()

    return payload

@router.get("/shops")
def get_admin_shops():
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("""
    SELECT s.shop_id, s.shop_name, s.address, s.phone, s.is_open, s.open_time, s.close_time, s.holiday_mode,
           COUNT(d.device_id) as active_devices_count
    FROM shop_settings s
    LEFT JOIN sellers sel ON s.shop_id = sel.shop_id
    LEFT JOIN seller_devices d ON sel.seller_id = d.seller_id AND d.notifications_enabled = 1
    GROUP BY s.shop_id
    """)
    rows = cursor.fetchall()
    conn.close()

    results = []
    for r in rows:
        d = dict(r)
        is_open_setting = bool(d.get("is_open", 1))
        holiday_mode = bool(d.get("holiday_mode", 0))
        open_time = d.get("open_time") or "09:00"
        close_time = d.get("close_time") or "21:00"

        effective_open, within_hours, status_msg = is_shop_effectively_open(
            is_open_setting=is_open_setting,
            holiday_mode=holiday_mode,
            open_time=open_time,
            close_time=close_time
        )
        d["manual_is_open"] = is_open_setting
        d["is_open"] = effective_open  # Never True outside working hours!
        d["is_within_working_hours"] = within_hours
        d["status_message"] = status_msg
        results.append(d)

    return results
