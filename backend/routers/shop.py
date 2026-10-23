from fastapi import APIRouter, HTTPException, Depends
from database import get_db
from models import ShopDto, ShopUpdateDto, SellerDto
from routers.auth import get_current_seller
from services.working_hours import is_within_working_hours, is_shop_effectively_open

router = APIRouter(prefix="/seller/shop", tags=["Seller Shop"])

@router.get("", response_model=ShopDto)
def get_shop_profile(seller: SellerDto = Depends(get_current_seller)):
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("""
    SELECT shop_id, shop_name, address, phone, is_open, open_time, close_time, accept_inquiries, holiday_mode
    FROM shop_settings
    WHERE shop_id = ?
    """, (seller.shop_id,))
    row = cursor.fetchone()

    if not row:
        # Create default shop settings if not yet existing
        cursor.execute("""
        INSERT INTO shop_settings (shop_id, shop_name, address, phone, is_open)
        VALUES (?, 'فروشگاه من', 'تهران، خیابان ولیعصر', ?, 1)
        """, (seller.shop_id, seller.phone_number))
        conn.commit()
        cursor.execute("SELECT * FROM shop_settings WHERE shop_id = ?", (seller.shop_id,))
        row = cursor.fetchone()

    conn.close()

    is_open_setting = bool(row["is_open"])
    holiday_mode = bool(row["holiday_mode"])
    open_time = row["open_time"] or "09:00"
    close_time = row["close_time"] or "21:00"

    effective_open, within_hours, status_msg = is_shop_effectively_open(
        is_open_setting=is_open_setting,
        holiday_mode=holiday_mode,
        open_time=open_time,
        close_time=close_time
    )

    return ShopDto(
        shop_id=row["shop_id"],
        shop_name=row["shop_name"] or "فروشگاه",
        address=row["address"] or "تهران",
        phone=row["phone"] or seller.phone_number,
        is_open=effective_open,  # Never pass True outside working hours!
        open_time=open_time,
        close_time=close_time,
        accept_inquiries=bool(row["accept_inquiries"]),
        holiday_mode=holiday_mode,
        manual_is_open=is_open_setting,
        is_within_working_hours=within_hours,
        status_message=status_msg
    )

@router.put("", response_model=ShopDto)
def update_shop_profile(payload: ShopUpdateDto, seller: SellerDto = Depends(get_current_seller)):
    conn = get_db()
    cursor = conn.cursor()

    # Get current
    cursor.execute("SELECT * FROM shop_settings WHERE shop_id = ?", (seller.shop_id,))
    row = cursor.fetchone()
    if not row:
        cursor.execute("INSERT INTO shop_settings (shop_id, shop_name) VALUES (?, 'فروشگاه')", (seller.shop_id,))
        conn.commit()
        cursor.execute("SELECT * FROM shop_settings WHERE shop_id = ?", (seller.shop_id,))
        row = cursor.fetchone()

    target_open_time = payload.open_time if payload.open_time is not None else (row["open_time"] or "09:00")
    target_close_time = payload.close_time if payload.close_time is not None else (row["close_time"] or "21:00")
    target_holiday = payload.holiday_mode if payload.holiday_mode is not None else bool(row["holiday_mode"])

    # If attempting to set is_open = True, validate working hours & holiday mode
    if payload.is_open is True:
        within_target_hours = is_within_working_hours(target_open_time, target_close_time)
        if not within_target_hours:
            conn.close()
            raise HTTPException(
                status_code=400,
                detail=f"امکان فعال‌سازی وضعیت باز در خارج از ساعات کاری وجود ندارد. ساعت کاری تعیین‌شده: {target_open_time} تا {target_close_time}"
            )
        if target_holiday:
            conn.close()
            raise HTTPException(
                status_code=400,
                detail="امکان فعال‌سازی وضعیت باز همزمان با حالت تعطیلات موقت وجود ندارد."
            )

    updates = []
    params = []
    if payload.shop_name is not None:
        updates.append("shop_name = ?")
        params.append(payload.shop_name)
    if payload.address is not None:
        updates.append("address = ?")
        params.append(payload.address)
    if payload.phone is not None:
        updates.append("phone = ?")
        params.append(payload.phone)
    if payload.is_open is not None:
        updates.append("is_open = ?")
        params.append(1 if payload.is_open else 0)
    if payload.open_time is not None:
        updates.append("open_time = ?")
        params.append(payload.open_time)
    if payload.close_time is not None:
        updates.append("close_time = ?")
        params.append(payload.close_time)
    if payload.accept_inquiries is not None:
        updates.append("accept_inquiries = ?")
        params.append(1 if payload.accept_inquiries else 0)
    if payload.holiday_mode is not None:
        updates.append("holiday_mode = ?")
        params.append(1 if payload.holiday_mode else 0)

    if updates:
        updates.append("updated_at = CURRENT_TIMESTAMP")
        params.append(seller.shop_id)
        sql = f"UPDATE shop_settings SET {', '.join(updates)} WHERE shop_id = ?"
        cursor.execute(sql, params)
        conn.commit()

    conn.close()
    return get_shop_profile(seller)

@router.post("/toggle-status", response_model=ShopDto)
def toggle_shop_status(seller: SellerDto = Depends(get_current_seller)):
    """Toggle store open/closed with a single tap."""
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("SELECT is_open, holiday_mode, open_time, close_time FROM shop_settings WHERE shop_id = ?", (seller.shop_id,))
    row = cursor.fetchone()

    if not row:
        conn.close()
        raise HTTPException(status_code=404, detail="تنظیمات فروشگاه یافت نشد")

    is_open_setting = bool(row["is_open"])
    holiday_mode = bool(row["holiday_mode"])
    open_time = row["open_time"] or "09:00"
    close_time = row["close_time"] or "21:00"

    effective_open, within_hours, _ = is_shop_effectively_open(
        is_open_setting=is_open_setting,
        holiday_mode=holiday_mode,
        open_time=open_time,
        close_time=close_time
    )

    # If currently effectively open, seller wants to close it
    if effective_open:
        cursor.execute("""
        UPDATE shop_settings
        SET is_open = 0, updated_at = CURRENT_TIMESTAMP
        WHERE shop_id = ?
        """, (seller.shop_id,))
        conn.commit()
        conn.close()
        return get_shop_profile(seller)

    # Otherwise seller wants to open it -> validate working hours and holiday mode
    if holiday_mode:
        conn.close()
        raise HTTPException(
            status_code=400,
            detail="فروشگاه در حالت تعطیلات موقت قرار دارد. برای بازگشایی، ابتدا حالت تعطیلات را غیرفعال کنید."
        )

    if not within_hours:
        conn.close()
        raise HTTPException(
            status_code=400,
            detail=f"امکان بازگشایی فروشگاه در ساعات غیرکاری وجود ندارد. ساعت کاری فروشگاه: {open_time} تا {close_time}"
        )

    cursor.execute("""
    UPDATE shop_settings
    SET is_open = 1, updated_at = CURRENT_TIMESTAMP
    WHERE shop_id = ?
    """, (seller.shop_id,))
    conn.commit()
    conn.close()

    return get_shop_profile(seller)
