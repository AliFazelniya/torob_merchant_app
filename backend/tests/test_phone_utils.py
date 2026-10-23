import pytest
from services.voice.phone_utils import (
    normalize_phone_number,
    is_iranian_mobile,
    is_iranian_landline,
    is_valid_phone,
    mask_phone_number
)


def test_persian_digit_normalization():
    persian_mobile = "۰۹۱۳۴۲۵۷۸۱۵"
    persian_landline = "۰۲۱۷۶۵۵۷۶۲۴"
    assert normalize_phone_number(persian_mobile) == "09134257815"
    assert normalize_phone_number(persian_landline) == "02176557624"


def test_prefix_normalization():
    assert normalize_phone_number("+989123456789") == "09123456789"
    assert normalize_phone_number("00982188888888") == "02188888888"
    assert normalize_phone_number("989123456789") == "09123456789"
    assert normalize_phone_number("9123456789") == "09123456789"


def test_special_characters_cleaning():
    assert normalize_phone_number(" 021 - 8888 8888 ") == "02188888888"
    assert normalize_phone_number("(031) 3222-4455") == "03132224455"


def test_is_iranian_mobile():
    assert is_iranian_mobile("09123456789") is True
    assert is_iranian_mobile("۰۹۱۳۴۲۵۷۸۱۵") is True
    assert is_iranian_mobile("09351234567") is True
    assert is_iranian_mobile("02188888888") is False  # Landline
    assert is_iranian_mobile("0912345") is False  # Too short
    assert is_iranian_mobile("091234567890") is False  # Too long


def test_is_iranian_landline():
    assert is_iranian_landline("02188888888") is True  # Tehran
    assert is_iranian_landline("۰۳۱۳۲۲۲۴۴۵۵") is True  # Isfahan
    assert is_iranian_landline("05138888888") is True  # Mashhad
    assert is_iranian_landline("07136666666") is True  # Shiraz
    assert is_iranian_landline("09123456789") is False  # Mobile
    assert is_iranian_landline("1234567") is False


def test_is_valid_phone():
    assert is_valid_phone("09134257815") is True
    assert is_valid_phone("02188888888") is True
    assert is_valid_phone("invalid_phone") is False
    assert is_valid_phone("") is False


def test_mask_phone_number():
    assert mask_phone_number("09134257815") == "0913***7815"
    assert mask_phone_number("02188888888") == "0218***8888"
