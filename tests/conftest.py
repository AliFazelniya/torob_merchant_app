import sys
import os
import pytest
from fastapi.testclient import TestClient

# Add backend directory to sys.path
backend_path = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "backend"))
if backend_path not in sys.path:
    sys.path.insert(0, backend_path)

from main import app
from database import init_db, get_db

@pytest.fixture(scope="session", autouse=True)
def setup_test_database():
    os.environ["DATABASE_PATH"] = "test_seller.db"
    init_db()
    yield
    # Cleanup test db
    if os.path.exists("test_seller.db"):
        try:
            os.remove("test_seller.db")
        except Exception:
            pass

@pytest.fixture
def client():
    with TestClient(app) as c:
        yield c

@pytest.fixture
def auth_headers(client):
    # Authenticate default test seller
    resp = client.post("/api/seller/auth/verify-otp", json={
        "phone_number": "09123456789",
        "otp_code": "12345"
    })
    token = resp.json()["access_token"]
    return {"Authorization": f"Bearer {token}"}
