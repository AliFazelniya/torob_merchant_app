import pytest

def test_direct_shop_info_update_is_blocked(client, auth_headers):
    """
    Ensure sellers cannot directly modify general shop information
    (shop_name, phone, address, open_time, close_time) via PUT /api/seller/shop.
    """
    # 1. Attempt to change shop_name directly
    resp_name = client.put("/api/seller/shop", headers=auth_headers, json={
        "shop_name": "نام جعلی متفرقه"
    })
    assert resp_name.status_code == 400
    assert "ثبت درخواست رسمی به پشتیبانی" in resp_name.json()["detail"]

    # 2. Attempt to change phone directly
    resp_phone = client.put("/api/seller/shop", headers=auth_headers, json={
        "phone": "09999999999"
    })
    assert resp_phone.status_code == 400
    assert "ثبت درخواست رسمی به پشتیبانی" in resp_phone.json()["detail"]

    # 3. Attempt to change address directly
    resp_addr = client.put("/api/seller/shop", headers=auth_headers, json={
        "address": "آدرس غیرواقعی جدید"
    })
    assert resp_addr.status_code == 400
    assert "ثبت درخواست رسمی به پشتیبانی" in resp_addr.json()["detail"]


def test_shop_change_request_lifecycle(client, auth_headers):
    """
    Test the full lifecycle of a formal shop change request:
    Seller Submits -> Admin Reviews -> Admin Rejects ->
    Seller Resubmits -> Admin Approves -> Shop Profile Updated.
    """
    # 1. Submitting empty request should fail
    resp_empty = client.post("/api/seller/shop/change-requests", headers=auth_headers, json={})
    assert resp_empty.status_code == 400

    # 2. Submit valid change request
    req_payload = {
        "requested_shop_name": "موبایل تخت جمشید (شعبه مرکزی)",
        "requested_phone": "02188887777",
        "requested_address": "تهران، خیابان ولیعصر، بالاتر از طالقانی، پلاک ۱۰۰",
        "requested_open_time": "10:00",
        "requested_close_time": "22:00",
        "reason": "تغییر آدرس و خط ثابت شعبه مرکزی"
    }
    submit_resp = client.post("/api/seller/shop/change-requests", headers=auth_headers, json=req_payload)
    assert submit_resp.status_code == 200
    req_data = submit_resp.json()
    assert req_data["status"] == "PENDING"
    assert req_data["requested_shop_name"] == req_payload["requested_shop_name"]
    req_id = req_data["request_id"]

    # 3. Attempting to submit another request while one is PENDING should be rejected
    dup_resp = client.post("/api/seller/shop/change-requests", headers=auth_headers, json={
        "requested_shop_name": "تلاش مجدد"
    })
    assert dup_resp.status_code == 400
    assert "در انتظار بررسی" in dup_resp.json()["detail"]

    # 4. Seller can get latest change request
    latest_resp = client.get("/api/seller/shop/change-requests/latest", headers=auth_headers)
    assert latest_resp.status_code == 200
    assert latest_resp.json()["request_id"] == req_id
    assert latest_resp.json()["status"] == "PENDING"

    # 5. Admin lists requests
    admin_list = client.get("/api/admin/shop-change-requests?status=PENDING")
    assert admin_list.status_code == 200
    assert any(r["request_id"] == req_id for r in admin_list.json())

    # 6. Admin rejects the first request
    reject_resp = client.post(f"/api/admin/shop-change-requests/{req_id}/reject", json={
        "action": "reject",
        "review_notes": "مدارک اجاره‌نامه ارائه نشده است."
    })
    assert reject_resp.status_code == 200
    assert reject_resp.json()["status"] == "REJECTED"
    assert reject_resp.json()["review_notes"] == "مدارک اجاره‌نامه ارائه نشده است."

    # 7. Now seller can submit a corrected request
    req2_payload = {
        "requested_shop_name": "موبایل تخت جمشید VIP",
        "requested_phone": "02199991111",
        "requested_address": "تهران، خیابان جمهوری، پاساژ چارسو، طبقه ۲",
        "requested_open_time": "09:30",
        "requested_close_time": "21:30",
        "reason": "شعبه جدید چارسو همراه با مدارک ضمیمه"
    }
    submit2_resp = client.post("/api/seller/shop/change-requests", headers=auth_headers, json=req2_payload)
    assert submit2_resp.status_code == 200
    req2_id = submit2_resp.json()["request_id"]

    # 8. Admin approves the corrected request
    approve_resp = client.post(f"/api/admin/shop-change-requests/{req2_id}/approve", json={
        "action": "approve",
        "review_notes": "مدارک بررسی و تایید گردید."
    })
    assert approve_resp.status_code == 200
    assert approve_resp.json()["status"] == "APPROVED"

    # 9. Verify shop profile reflects the approved changes
    profile_resp = client.get("/api/seller/shop", headers=auth_headers)
    assert profile_resp.status_code == 200
    updated_profile = profile_resp.json()
    assert updated_profile["shop_name"] == req2_payload["requested_shop_name"]
    assert updated_profile["phone"] == req2_payload["requested_phone"]
    assert updated_profile["address"] == req2_payload["requested_address"]
    assert updated_profile["open_time"] == req2_payload["requested_open_time"]
    assert updated_profile["close_time"] == req2_payload["requested_close_time"]
