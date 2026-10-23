def test_request_otp(client):
    response = client.post("/api/seller/auth/request-otp", json={"phone_number": "09129998877"})
    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
    assert "demo_otp" in data

def test_verify_otp_success(client):
    response = client.post("/api/seller/auth/verify-otp", json={
        "phone_number": "09129998877",
        "otp_code": "12345"
    })
    assert response.status_code == 200
    data = response.json()
    assert "access_token" in data
    assert data["seller"]["phone_number"] == "09129998877"

def test_verify_otp_invalid(client):
    response = client.post("/api/seller/auth/verify-otp", json={
        "phone_number": "09129998877",
        "otp_code": "00000"
    })
    assert response.status_code == 400

def test_get_me(client, auth_headers):
    response = client.get("/api/seller/auth/me", headers=auth_headers)
    assert response.status_code == 200
    data = response.json()
    assert data["seller_id"] == "seller_test_001"
