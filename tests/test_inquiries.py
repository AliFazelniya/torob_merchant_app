import uuid
from datetime import datetime

def test_inquiry_lifecycle_and_idempotency(client, auth_headers):
    # 1. Admin creates test inquiry
    shop_id = "shop_51d0c1a6edf81ba759726bab"
    create_resp = client.post("/api/admin/inquiries/create-test", json={
        "shop_id": shop_id,
        "product_title": "لپ‌تاپ لنوو ThinkPad E14",
        "recorded_price_toman": 34500000,
        "customer_name": "امیر قاسمی",
        "customer_phone": "09351234567",
        "inquiry_type": "price_and_stock",
        "user_note": "آیا گارانتی اصلی سازگار دارد؟"
    })
    assert create_resp.status_code == 200
    inq = create_resp.json()
    inquiry_id = inq["inquiry_id"]
    assert inq["status"] == "ACTIVE"

    # 2. Seller lists inquiries
    list_resp = client.get("/api/seller/inquiries?status=ACTIVE", headers=auth_headers)
    assert list_resp.status_code == 200
    inquiries = list_resp.json()
    assert any(i["inquiry_id"] == inquiry_id for i in inquiries)

    # 3. Seller submits response with unique client_response_id
    client_uuid = str(uuid.uuid4())
    resp1 = client.post(f"/api/seller/inquiries/{inquiry_id}/respond", headers=auth_headers, json={
        "client_response_id": client_uuid,
        "offered_price_toman": 34200000,
        "is_in_stock": True,
        "notes": "موجود است، با گارانتی ۲۴ ماهه سازگار ارقام تقدیم می‌شود.",
        "submitted_at": datetime.utcnow().strftime("%Y-%m-%d %H:%M:%S")
    })
    assert resp1.status_code == 200
    data1 = resp1.json()
    assert data1["is_duplicate"] is False
    assert data1["offered_price_toman"] == 34200000

    # 4. Duplicate prevention test: Resubmitting with SAME client_response_id (weak internet retry simulation)
    resp2 = client.post(f"/api/seller/inquiries/{inquiry_id}/respond", headers=auth_headers, json={
        "client_response_id": client_uuid,
        "offered_price_toman": 34200000,
        "is_in_stock": True,
        "notes": "تکرار پاسخ به دلیل قطعی اینترنت",
        "submitted_at": datetime.utcnow().strftime("%Y-%m-%d %H:%M:%S")
    })
    assert resp2.status_code == 200
    data2 = resp2.json()
    assert data2["is_duplicate"] is True
    assert data2["response_id"] == data1["response_id"]

    # 5. Verify inquiry is now marked as ANSWERED
    detail_resp = client.get(f"/api/seller/inquiries/{inquiry_id}", headers=auth_headers)
    assert detail_resp.status_code == 200
    assert detail_resp.json()["status"] == "ANSWERED"
