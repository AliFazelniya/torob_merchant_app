import sqlite3
import os
import json
from datetime import datetime
from config import settings

def get_db():
    """Get SQLite connection with WAL mode and row factory."""
    conn = sqlite3.connect(settings.DATABASE_PATH, timeout=10.0)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA foreign_keys=ON")
    return conn

def init_db():
    """Initialize SQLite database tables and migrations."""
    conn = get_db()
    cursor = conn.cursor()

    # 1. Sellers Table
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS sellers (
        seller_id TEXT PRIMARY KEY,
        phone_number TEXT UNIQUE NOT NULL,
        shop_id TEXT NOT NULL,
        full_name TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        is_active INTEGER DEFAULT 1
    )
    """)

    # 2. Seller Devices & Push Tokens
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS seller_devices (
        device_id TEXT PRIMARY KEY,
        seller_id TEXT NOT NULL,
        fcm_token TEXT UNIQUE NOT NULL,
        device_model TEXT,
        os_version TEXT,
        app_version TEXT,
        notifications_enabled INTEGER DEFAULT 1,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (seller_id) REFERENCES sellers(seller_id)
    )
    """)

    # 3. Inquiries Table
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS inquiries (
        inquiry_id TEXT PRIMARY KEY,
        tracking_code TEXT UNIQUE NOT NULL,
        shop_id TEXT NOT NULL,
        product_id TEXT,
        product_title TEXT NOT NULL,
        product_image_url TEXT,
        category_name TEXT,
        recorded_price_toman INTEGER DEFAULT 0,
        customer_name TEXT NOT NULL,
        customer_phone TEXT,
        inquiry_type TEXT NOT NULL, -- 'price_and_stock', 'specs', 'installment'
        user_note TEXT,
        status TEXT NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'ANSWERED', 'EXPIRED', 'TRANSFERRED'
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        expires_at TIMESTAMP NOT NULL
    )
    """)

    # 4. Inquiry Responses (with client_response_id for offline idempotency)
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS inquiry_responses (
        response_id TEXT PRIMARY KEY,
        client_response_id TEXT UNIQUE NOT NULL,
        inquiry_id TEXT NOT NULL,
        seller_id TEXT NOT NULL,
        shop_id TEXT NOT NULL,
        offered_price_toman INTEGER NOT NULL,
        is_in_stock INTEGER NOT NULL, -- 1 = in stock, 0 = out of stock
        stock_count INTEGER DEFAULT 1,
        notes TEXT,
        submitted_at TIMESTAMP NOT NULL, -- timestamp on device
        received_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- timestamp on server
        FOREIGN KEY (inquiry_id) REFERENCES inquiries(inquiry_id),
        FOREIGN KEY (seller_id) REFERENCES sellers(seller_id)
    )
    """)

    # Safe migration for existing DB
    try:
        cursor.execute("ALTER TABLE inquiry_responses ADD COLUMN stock_count INTEGER DEFAULT 1")
    except Exception:
        pass

    # 5. Push Notification Audit Logs
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS push_audit_logs (
        log_id TEXT PRIMARY KEY,
        inquiry_id TEXT,
        shop_id TEXT,
        fcm_token TEXT,
        notification_type TEXT NOT NULL, -- 'INITIAL_INQUIRY', 'REMINDER_5MIN', 'EXPIRATION_ALERT', 'TEST_PING'
        title TEXT NOT NULL,
        body TEXT NOT NULL,
        payload_json TEXT,
        status TEXT NOT NULL, -- 'DELIVERED', 'FAILED', 'SIMULATED'
        error_detail TEXT,
        dispatched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
    """)

    # 6. Inquiry Timing & SLA Timelines
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS inquiry_timelines (
        inquiry_id TEXT PRIMARY KEY,
        registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        initial_push_at TIMESTAMP,
        reminder_push_at TIMESTAMP,
        seller_viewed_at TIMESTAMP,
        seller_responded_at TIMESTAMP,
        response_time_seconds INTEGER,
        expired_at TIMESTAMP,
        alternative_shop_id TEXT,
        FOREIGN KEY (inquiry_id) REFERENCES inquiries(inquiry_id)
    )
    """)

    # 7. Shop Operating Settings
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS shop_settings (
        shop_id TEXT PRIMARY KEY,
        shop_name TEXT,
        address TEXT,
        phone TEXT,
        is_open INTEGER DEFAULT 1,
        open_time TEXT DEFAULT '09:00',
        close_time TEXT DEFAULT '21:00',
        accept_inquiries INTEGER DEFAULT 1,
        holiday_mode INTEGER DEFAULT 0,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
    """)

    # 8. Dynamic Scheduler Settings
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS scheduler_configs (
        config_key TEXT PRIMARY KEY,
        config_value TEXT NOT NULL,
        description TEXT,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
    """)

    # Seed default scheduler configs
    cursor.execute("""
    INSERT OR IGNORE INTO scheduler_configs (config_key, config_value, description)
    VALUES
        ('reminder_timeout_seconds', '300', 'Time in seconds after creation to send reminder push'),
        ('expiration_timeout_seconds', '600', 'Time in seconds after creation to expire inquiry')
    """)

    # Seed sample seller & shop if empty
    cursor.execute("SELECT COUNT(*) FROM sellers")
    if cursor.fetchone()[0] == 0:
        default_seller_id = "seller_test_001"
        default_phone = "09123456789"
        default_shop_id = "shop_51d0c1a6edf81ba759726bab"  # موبایل تخت جمشید
        default_name = "علی رضایی"

        cursor.execute("""
        INSERT INTO sellers (seller_id, phone_number, shop_id, full_name)
        VALUES (?, ?, ?, ?)
        """, (default_seller_id, default_phone, default_shop_id, default_name))

        cursor.execute("""
        INSERT INTO shop_settings (shop_id, shop_name, address, phone, is_open, open_time, close_time)
        VALUES (?, ?, ?, ?, 1, '09:30', '21:30')
        """, (default_shop_id, "موبایل تخت جمشید", "خیابان جمهوری، نرسیده به پل حافظ، نبش کوچه کلانتری، پاساژ خانه موبایل، پلاک ۴۱۲", "۰۲۱۷۶۵۵۷۶۲۴"))

    conn.commit()
    conn.close()

if __name__ == "__main__":
    init_db()
    print("Database initialized successfully.")
