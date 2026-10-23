import asyncio
from database import get_db
from services.scheduler import scheduler

def test_scheduler_lifecycle_and_push(client, auth_headers):
    # 1. Register push token for seller
    reg_resp = client.post("/api/seller/push/register-token", headers=auth_headers, json={
        "fcm_token": "fcm_token_sample_test_abc123",
        "device_model": "Pixel 8 Pro",
        "os_version": "Android 14",
        "app_version": "1.0.0",
        "notifications_enabled": True
    })
    assert reg_resp.status_code == 200

    # 2. Update scheduler config to small test interval (2 seconds reminder, 4 seconds expiry)
    cfg_resp = client.put("/api/admin/scheduler/config", json={
        "reminder_timeout_seconds": 2,
        "expiration_timeout_seconds": 4
    })
    assert cfg_resp.status_code == 200

    # 3. Create test inquiry (triggers Time Zero push)
    shop_id = "shop_51d0c1a6edf81ba759726bab"
    inq_resp = client.post("/api/admin/inquiries/create-test", json={
        "shop_id": shop_id,
        "product_title": "اسپیکر جی‌بی‌ال Flip 6",
        "recorded_price_toman": 4900000,
        "customer_name": "پوریا کمالی",
        "inquiry_type": "price_and_stock"
    })
    assert inq_resp.status_code == 200
    inquiry_id = inq_resp.json()["inquiry_id"]

    # Verify Time Zero push log exists
    logs_resp = client.get("/api/seller/push/logs", headers=auth_headers)
    assert logs_resp.status_code == 200
    logs = logs_resp.json()
    assert any(l["inquiry_id"] == inquiry_id and l["notification_type"] == "INITIAL_INQUIRY" for l in logs)

    # 4. Trigger test push endpoint
    test_push = client.post("/api/seller/push/test", headers=auth_headers)
    assert test_push.status_code == 200
    assert test_push.json()["success"] is True

    # 5. Run scheduler tick to verify non-blocking execution
    asyncio.run(scheduler.run_scheduler_tick())

    # 6. Reset scheduler config back to default 300s & 600s
    client.put("/api/admin/scheduler/config", json={
        "reminder_timeout_seconds": 300,
        "expiration_timeout_seconds": 600
    })
