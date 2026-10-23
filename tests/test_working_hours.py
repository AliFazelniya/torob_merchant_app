from datetime import time
import pytest
from services.working_hours import (
    parse_time_str,
    is_within_working_hours,
    is_shop_effectively_open,
)


def test_parse_time_str():
    assert parse_time_str("09:00") == time(9, 0)
    assert parse_time_str("21:30") == time(21, 30)
    assert parse_time_str("02:15:40") == time(2, 15)
    assert parse_time_str("") == time(9, 0)


def test_is_within_working_hours_daytime():
    # Daytime: 09:00 to 21:00
    open_t = "09:00"
    close_t = "21:00"

    assert is_within_working_hours(open_t, close_t, time(9, 0)) is True
    assert is_within_working_hours(open_t, close_t, time(14, 30)) is True
    assert is_within_working_hours(open_t, close_t, time(21, 0)) is True

    assert is_within_working_hours(open_t, close_t, time(8, 59)) is False
    assert is_within_working_hours(open_t, close_t, time(21, 1)) is False
    assert is_within_working_hours(open_t, close_t, time(4, 30)) is False


def test_is_within_working_hours_overnight():
    # Overnight shift: 18:00 to 02:00 (spans midnight)
    open_t = "18:00"
    close_t = "02:00"

    assert is_within_working_hours(open_t, close_t, time(18, 0)) is True
    assert is_within_working_hours(open_t, close_t, time(23, 59)) is True
    assert is_within_working_hours(open_t, close_t, time(0, 30)) is True
    assert is_within_working_hours(open_t, close_t, time(2, 0)) is True

    assert is_within_working_hours(open_t, close_t, time(2, 1)) is False
    assert is_within_working_hours(open_t, close_t, time(12, 0)) is False
    assert is_within_working_hours(open_t, close_t, time(17, 59)) is False


def test_is_within_working_hours_24h():
    # 24-hour shift: 00:00 to 23:59
    assert is_within_working_hours("00:00", "23:59", time(4, 30)) is True
    assert is_within_working_hours("00:00", "23:59", time(15, 0)) is True


def test_is_shop_effectively_open_unit():
    # 1. Closed by manual switch
    is_open, within, msg = is_shop_effectively_open(
        is_open_setting=False,
        holiday_mode=False,
        open_time="09:00",
        close_time="21:00",
        current_time=time(12, 0)
    )
    assert is_open is False
    assert within is True

    # 2. Holiday mode active
    is_open, within, msg = is_shop_effectively_open(
        is_open_setting=True,
        holiday_mode=True,
        open_time="09:00",
        close_time="21:00",
        current_time=time(12, 0)
    )
    assert is_open is False
    assert "تعطیلات" in msg

    # 3. Outside working hours
    is_open, within, msg = is_shop_effectively_open(
        is_open_setting=True,
        holiday_mode=False,
        open_time="09:00",
        close_time="21:00",
        current_time=time(4, 0)
    )
    assert is_open is False
    assert within is False
    assert "خارج از ساعات کاری" in msg

    # 4. Open and within hours
    is_open, within, msg = is_shop_effectively_open(
        is_open_setting=True,
        holiday_mode=False,
        open_time="09:00",
        close_time="21:00",
        current_time=time(12, 0)
    )
    assert is_open is True
    assert within is True


def test_shop_api_working_hours_enforcement(client, auth_headers):
    # Set shop hours to a range that is definitely closed (e.g. 01:00 to 01:05)
    update_resp = client.put("/api/seller/shop", headers=auth_headers, json={
        "open_time": "01:00",
        "close_time": "01:05",
        "is_open": False,
        "holiday_mode": False
    })
    assert update_resp.status_code == 200
    shop_data = update_resp.json()
    assert shop_data["open_time"] == "01:00"
    assert shop_data["close_time"] == "01:05"

    # Now fetch shop profile -> is_open MUST be False!
    get_resp = client.get("/api/seller/shop", headers=auth_headers)
    assert get_resp.status_code == 200
    profile = get_resp.json()
    assert profile["is_open"] is False
    assert profile["is_within_working_hours"] is False

    # Attempt to toggle shop open when outside working hours -> MUST return 400
    toggle_resp = client.post("/api/seller/shop/toggle-status", headers=auth_headers)
    assert toggle_resp.status_code == 400
    assert "ساعات غیرکاری" in toggle_resp.json()["detail"]

    # Attempt to PUT is_open = True while outside hours -> MUST return 400
    put_open_resp = client.put("/api/seller/shop", headers=auth_headers, json={
        "is_open": True
    })
    assert put_open_resp.status_code == 400
    assert "خارج از ساعات کاری" in put_open_resp.json()["detail"]

    # Now update hours to 24h (00:00 to 23:59)
    allow_resp = client.put("/api/seller/shop", headers=auth_headers, json={
        "open_time": "00:00",
        "close_time": "23:59"
    })
    assert allow_resp.status_code == 200
    assert allow_resp.json()["is_within_working_hours"] is True

    # Now that we are within working hours, toggle can open the shop!
    toggle_on = client.post("/api/seller/shop/toggle-status", headers=auth_headers)
    assert toggle_on.status_code == 200
    assert toggle_on.json()["is_open"] is True

    # Toggling again turns it off
    toggle_off = client.post("/api/seller/shop/toggle-status", headers=auth_headers)
    assert toggle_off.status_code == 200
    assert toggle_off.json()["is_open"] is False

    # Turn it back on for admin check
    client.post("/api/seller/shop/toggle-status", headers=auth_headers)

    # Admin shops endpoint also returns dynamic is_open
    admin_shops = client.get("/api/admin/shops")
    assert admin_shops.status_code == 200
    shops_list = admin_shops.json()
    our_shop = next((s for s in shops_list if s["shop_id"] == profile["shop_id"]), None)
    assert our_shop is not None
    assert our_shop["is_open"] is True
