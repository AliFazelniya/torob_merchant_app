import uuid
from datetime import datetime, timedelta
from fastapi import APIRouter, HTTPException, Depends, Header
from database import get_db
from models import RequestOtpRequest, RequestOtpResponse, VerifyOtpRequest, TokenResponse, SellerDto
from services.sms_service import send_otp, verify_otp
from config import settings

router = APIRouter(prefix="/seller/auth", tags=["Seller Auth"])

def get_current_seller(authorization: str = Header(None)) -> SellerDto:
    """Simple Bearer token resolution for seller."""
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="اعتبارسنجی نامعتبر است. لطفا مجددا وارد شوید.")
    
    token = authorization.replace("Bearer ", "").strip()
    # Simple token scheme: token is either seller_id or a simulated JWT token
    seller_id = token.replace("token_", "") if token.startswith("token_") else token
    
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("SELECT seller_id, phone_number, shop_id, full_name, is_active FROM sellers WHERE seller_id = ?", (seller_id,))
    row = cursor.fetchone()
    conn.close()

    if not row:
        raise HTTPException(status_code=401, detail="حساب کاربری یافت نشد")

    return SellerDto(
        seller_id=row["seller_id"],
        phone_number=row["phone_number"],
        shop_id=row["shop_id"],
        full_name=row["full_name"],
        is_active=bool(row["is_active"])
    )

@router.post("/request-otp", response_model=RequestOtpResponse)
def request_otp_endpoint(payload: RequestOtpRequest):
    phone = payload.phone_number.strip().replace(" ", "")
    if len(phone) < 10:
        raise HTTPException(status_code=400, detail="شماره تلفن وارد شده نامعتبر است")

    success, msg, code = send_otp(phone)
    # Always return demo OTP if configured or mock
    demo_otp = code if (settings.SMS_PROVIDER == "mock" or not settings.SMS_API_KEY) else None

    return RequestOtpResponse(
        success=True,
        message=msg,
        demo_otp=demo_otp
    )

@router.post("/verify-otp", response_model=TokenResponse)
def verify_otp_endpoint(payload: VerifyOtpRequest):
    phone = payload.phone_number.strip().replace(" ", "")
    code = payload.otp_code.strip()

    if not verify_otp(phone, code):
        raise HTTPException(status_code=400, detail="کد تایید وارد شده نادرست یا منقضی شده است")

    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("SELECT seller_id, phone_number, shop_id, full_name, is_active FROM sellers WHERE phone_number = ?", (phone,))
    row = cursor.fetchone()

    if not row:
        # Auto-register new seller linked to default shop
        seller_id = f"seller_{uuid.uuid4().hex[:8]}"
        shop_id = "shop_51d0c1a6edf81ba759726bab"
        full_name = "فروشنده جدید"
        cursor.execute("""
        INSERT INTO sellers (seller_id, phone_number, shop_id, full_name)
        VALUES (?, ?, ?, ?)
        """, (seller_id, phone, shop_id, full_name))
        conn.commit()
        seller_dto = SellerDto(seller_id=seller_id, phone_number=phone, shop_id=shop_id, full_name=full_name, is_active=True)
    else:
        seller_dto = SellerDto(
            seller_id=row["seller_id"],
            phone_number=row["phone_number"],
            shop_id=row["shop_id"],
            full_name=row["full_name"],
            is_active=bool(row["is_active"])
        )

    conn.close()

    token = f"token_{seller_dto.seller_id}"
    return TokenResponse(access_token=token, seller=seller_dto)

@router.get("/me", response_model=SellerDto)
def get_me(seller: SellerDto = Depends(get_current_seller)):
    return seller
