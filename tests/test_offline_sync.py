import uuid
from datetime import datetime

def test_batch_offline_sync(client, auth_headers):
    shop_id = "shop_51d0c1a6edf81ba759726bab"

    # Create 2 test inquiries
    inq1 = client.post("/api/admin/inquiries/create-test", json={
        "shop_id": shop_id,
        "product_title": "هدفون سونی WH-1000XM5",
        "recorded_price_toman": 16500000,
        "customer_name": "نیما راد",
        "inquiry_type": "price_and_stock"
    }).json()

    inq2 = client.post("/api/admin/inquiries/create-test", json={
        "shop_id": shop_id,
        "product_title": "ساعت هوشمند اپل واچ سری ۹",
        "recorded_price_toman": 22000000,
        "customer_name": "مریم احمدی",
        "inquiry_type": "price_and_stock"
    }).json()

    # Simulate offline queue flush
    uuid1 = str(uuid.uuid4())
    uuid2 = str(uuid.uuid4())
    batch_payload = {
        "items": [
            {
                "inquiry_id": inq1["inquiry_id"],
                "client_response_id": uuid1,
                "offered_price_toman": 16400000,
                "is_in_stock": True,
                "notes": "رنگ مشکی و نقره‌ای موجود است",
                "submitted_at": datetime.utcnow().strftime("%Y-%m-%d %H:%M:%S")
            },
            {
                "inquiry_id": inq2["inquiry_id"],
                "client_response_id": uuid2,
                "offered_price_toman": 21800000,
                "is_in_stock": False,
                "notes": "اتمام موجودی، فردا شارژ می‌شود",
                "submitted_at": datetime.utcnow().strftime("%Y-%m-%d %H:%M:%S")
            }
        ]
    }

    # 1. First sync
    res1 = client.post("/api/seller/inquiries/batch-sync", headers=auth_headers, json=batch_payload)
    assert res1.status_code == 200
    data1 = res1.json()
    assert data1["synced_count"] == 2
    assert data1["duplicate_count"] == 0

    # 2. Resync (simulating re-try or network dropped ack)
    res2 = client.post("/api/seller/inquiries/batch-sync", headers=auth_headers, json=batch_payload)
    assert res2.status_code == 200
    data2 = res2.json()
    assert data2["synced_count"] == 0
    assert data2["duplicate_count"] == 2
