#!/usr/bin/env python3
"""Generate deterministic, realistic ERP load-test fixtures.

The fixture is synthetic. Public Vinamilk product names are catalog references
only; employee identities and operational records do not represent real people
or internal Vinamilk data.
"""

from __future__ import annotations

import csv
import json
import random
import unicodedata
from calendar import monthrange
from collections import defaultdict
from datetime import date, datetime, time, timedelta, timezone
from pathlib import Path


seed_value = 20260916
random_source = random.Random(seed_value)
data_root = Path(__file__).resolve().parent
business_today = date(2026, 9, 16)
utc_plus_seven = timezone(timedelta(hours=7))


def write_csv(relative_path: str, field_names: list[str], rows: list[dict]) -> None:
    target_path = data_root / relative_path
    target_path.parent.mkdir(parents=True, exist_ok=True)
    with target_path.open("w", encoding="utf-8-sig", newline="") as output_file:
        writer = csv.DictWriter(output_file, fieldnames=field_names, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)


def ascii_slug(value: str) -> str:
    normalized_value = unicodedata.normalize("NFD", value)
    without_marks = "".join(character for character in normalized_value if unicodedata.category(character) != "Mn")
    without_marks = without_marks.replace("Đ", "D").replace("đ", "d")
    return "".join(character.lower() if character.isalnum() else "." for character in without_marks).strip(".")


def iso_timestamp(day_value: date, hour_value: int = 8, minute_value: int = 0) -> str:
    return datetime.combine(day_value, time(hour_value, minute_value), tzinfo=utc_plus_seven).isoformat()


def format_date_vi(value: date | None) -> str:
    """Return the human-facing Vietnamese date format while keeping ISO fields machine-readable."""
    return value.strftime("%d/%m/%Y") if value else ""


def employee_phone_number(employee_index: int) -> str:
    """Generate a deterministic Vietnamese 10-digit mobile number for synthetic fixtures."""
    mobile_prefixes = [
        "032", "033", "034", "035", "036", "037", "038", "039",
        "056", "058", "070", "076", "077", "078", "079", "081",
        "082", "083", "084", "085", "086", "088", "089", "090",
        "091", "093", "094", "096", "097", "098", "099",
    ]
    prefix = mobile_prefixes[employee_index % len(mobile_prefixes)]
    subscriber = (employee_index * 7919 + 1234567) % 10_000_000
    return f"{prefix}{subscriber:07d}"


def employee_birth_date(hired_on: date, employee_index: int) -> date:
    """Create an adult birth date that is always at least 18 on the hiring date."""
    earliest = date(1965, 1, 1)
    latest = hired_on.replace(year=hired_on.year - 18)
    if latest < earliest:
        latest = earliest
    span_days = (latest - earliest).days
    return earliest + timedelta(days=(employee_index * 7919) % (span_days + 1))


def month_start(year_value: int, month_value: int) -> date:
    return date(year_value, month_value, 1)


def month_end(year_value: int, month_value: int) -> date:
    return date(year_value, month_value, monthrange(year_value, month_value)[1])


product_references = [
    ("Sữa tươi tiệt trùng Green Farm Rất ít đường", "Sữa tươi tiệt trùng", "Green Farm", "https://www.vinamilk.com.vn/products/sua-dau-nanh-vinamilk-super-soy-gap-doi-canxi"),
    ("Sữa tươi tiệt trùng Green Farm Organic", "Sữa tươi tiệt trùng", "Green Farm", "https://www.vinamilk.com.vn/products/sua-dau-nanh-vinamilk-super-soy-gap-doi-canxi"),
    ("Sữa tươi tiệt trùng Green Farm Cao đạm ít béo", "Sữa tươi tiệt trùng", "Green Farm", "https://www.vinamilk.com.vn/products/sua-dau-nanh-vinamilk-super-soy-gap-doi-canxi"),
    ("Sữa tươi tiệt trùng 100% Ít đường", "Sữa tươi tiệt trùng", "Vinamilk 100%", "https://partners.vinamilk.com.vn/collections/all-products"),
    ("Sữa tươi tiệt trùng 100% Không đường", "Sữa tươi tiệt trùng", "Vinamilk 100%", "https://new.vinamilk.com.vn/collections/sua-tuoi"),
    ("Sữa hạt 9 loại hạt Ít đường", "Sữa hạt", "Vinamilk", "https://www.vinamilk.com.vn/products/sua-dau-nanh-vinamilk-super-soy-gap-doi-canxi"),
    ("Sữa đậu nành Hạnh nhân Ít đường", "Sữa đậu nành", "Vinamilk", "https://partners.vinamilk.com.vn/collections/all-products"),
    ("Sữa đậu nành Sữa tươi Ít đường", "Sữa đậu nành", "Vinamilk", "https://www.vinamilk.com.vn/products/sua-bot-pedia-kenji-1"),
    ("Sữa bột CanxiPro", "Sữa bột người lớn", "CanxiPro", "https://www.vinamilk.com.vn/products/sua-dau-nanh-vinamilk-super-soy-gap-doi-canxi"),
    ("Sữa chua uống SuSu Hương Dâu", "Sữa chua uống", "SuSu", "https://www.vinamilk.com.vn/vi/tin-tuc-su-kien/2556/khuyen-dung-susu-va-hero-mua-cang-nhieu-qua-cang-to"),
    ("Sữa chua uống SuSu Hương Cam", "Sữa chua uống", "SuSu", "https://www.vinamilk.com.vn/cong-bo-san-pham/wp-content/uploads/cbsp/pa-000001-tcbsp-09-ncpt-24-final.pdf"),
    ("Sữa chua uống SuSu Hương Táo Nho", "Sữa chua uống", "SuSu", "https://www.vinamilk.com.vn/vi/tin-tuc-su-kien/2556/khuyen-dung-susu-va-hero-mua-cang-nhieu-qua-cang-to"),
    ("Sữa chua uống SuSu Hương Việt Quất Chuối", "Sữa chua uống", "SuSu", "https://www.vinamilk.com.vn/vi/tin-tuc-su-kien/2556/khuyen-dung-susu-va-hero-mua-cang-nhieu-qua-cang-to"),
    ("Sữa trái cây Hero Hương Dâu", "Sữa trái cây", "Hero", "https://new.vinamilk.com.vn/collections/sua-trai-cay"),
    ("Sữa trái cây Hero Hương Kẹo Nho", "Sữa trái cây", "Hero", "https://new.vinamilk.com.vn/collections/sua-trai-cay"),
    ("Sữa trái cây Hero Hương Cam", "Sữa trái cây", "Hero", "https://new.vinamilk.com.vn/collections/sua-trai-cay"),
    ("Sữa trái cây Hero Hương Dưa Hấu", "Sữa trái cây", "Hero", "https://new.vinamilk.com.vn/collections/sua-trai-cay"),
    ("Sữa trái cây SuSu Thạch Hương Kẹo Bông", "Sữa trái cây có thạch", "SuSu", "https://new.vinamilk.com.vn/collections/sua-trai-cay"),
    ("Sữa trái cây SuSu Thạch Hương Marshmallow Kiwi", "Sữa trái cây có thạch", "SuSu", "https://new.vinamilk.com.vn/collections/sua-trai-cay"),
    ("Nước ép Cam Đào Collagen", "Nước ép", "Vinamilk", "https://partners.vinamilk.com.vn/collections/all-products"),
    ("Nước ép Cam", "Nước ép", "Vinamilk", "https://www.vinamilk.com.vn/products/sua-bot-pedia-kenji-1"),
    ("Nước ép Táo", "Nước ép", "Vinamilk", "https://new.vinamilk.com.vn/collections/nuoc-giai-khat"),
    ("Nước dừa tươi", "Nước dừa", "Vinamilk", "https://new.vinamilk.com.vn/collections/nuoc-giai-khat"),
    ("Nước dừa tắc", "Nước dừa", "Vinamilk", "https://www.vinamilk.com.vn/products/sua-bot-pedia-kenji-1"),
    ("Nước tinh khiết ICY Premium", "Nước tinh khiết", "ICY", "https://new.vinamilk.com.vn/collections/nuoc-giai-khat"),
    ("Sữa đặc Ông Thọ Nhãn đỏ", "Sữa đặc", "Ông Thọ", "https://www.vinamilk.com.vn/products/sua-bot-pedia-kenji-1"),
    ("Sữa bột Dielac Grow Plus 2", "Sữa bột trẻ em", "Dielac Grow Plus", "https://livestream.vinamilk.com.vn/"),
    ("Sữa uống dinh dưỡng Dielac Grow Plus", "Sữa uống dinh dưỡng", "Dielac Grow Plus", "https://livestream.vinamilk.com.vn/"),
    ("Sữa bột Optimum Gold 4", "Sữa bột trẻ em", "Optimum Gold", "https://livestream.vinamilk.com.vn/"),
    ("Sữa uống dinh dưỡng Optimum Gold", "Sữa uống dinh dưỡng", "Optimum Gold", "https://livestream.vinamilk.com.vn/"),
    ("Sữa bột Vinamilk ColosGold 3", "Sữa bột trẻ em", "ColosGold", "https://livestream.vinamilk.com.vn/"),
    ("Sữa uống dinh dưỡng Vinamilk ColosGold", "Sữa uống dinh dưỡng", "ColosGold", "https://livestream.vinamilk.com.vn/"),
    ("Sữa bột YokoGold 1", "Sữa bột trẻ em", "YokoGold", "https://www.vinamilk.com.vn/products/sua-bot-pedia-kenji-1"),
    ("Bột ăn dặm RiDielac Gold", "Bột dinh dưỡng", "RiDielac Gold", "https://livestream.vinamilk.com.vn/"),
    ("Kem hộp Vinamilk Dừa", "Kem", "Vinamilk", "https://www.vinamilk.com.vn/products/sua-bot-pedia-kenji-1"),
    ("Kem hộp Vinamilk Gelato Dừa", "Kem", "Vinamilk Gelato", "https://www.vinamilk.com.vn/products/sua-bot-pedia-kenji-1"),
    ("Sữa chua ăn Vinamilk Có đường", "Sữa chua ăn", "Vinamilk", "https://partners.vinamilk.com.vn/collections/all-products"),
    ("Sữa chua ăn Vinamilk Không đường", "Sữa chua ăn", "Vinamilk", "https://partners.vinamilk.com.vn/collections/all-products"),
    ("Sữa chua uống Probi", "Sữa chua uống", "Probi", "https://partners.vinamilk.com.vn/collections/all-products"),
    ("Phô mai Vinamilk", "Phô mai", "Vinamilk", "https://www.vinamilk.com.vn/static/uploads/bc_thuong_nien/1398182024-7e25ebe2588a7b30e90438bd67863da4777dfe4bbb6d919cb6a6b9c98867b980.pdf"),
]


department_specs = [
    ("board", "Ban điều hành", ""),
    ("human_resources", "Phòng Nhân sự", "board"),
    ("finance", "Phòng Tài chính - Kế toán", "board"),
    ("procurement", "Phòng Thu mua", "board"),
    ("quality_assurance", "Phòng Đảm bảo chất lượng", "board"),
    ("research_development", "Trung tâm Nghiên cứu và Phát triển", "board"),
    ("supply_chain", "Khối Chuỗi cung ứng", "board"),
    ("warehouse_raw", "Kho nguyên liệu", "supply_chain"),
    ("warehouse_finished", "Kho thành phẩm", "supply_chain"),
    ("production_fresh_milk", "Xưởng Sữa nước", "board"),
    ("production_yogurt", "Xưởng Sữa chua", "board"),
    ("production_powder", "Xưởng Sữa bột", "board"),
    ("production_beverage", "Xưởng Nước giải khát", "board"),
    ("maintenance", "Phòng Kỹ thuật và Bảo trì", "board"),
    ("laboratory", "Phòng Thí nghiệm", "quality_assurance"),
    ("sales", "Khối Kinh doanh", "board"),
    ("marketing", "Phòng Marketing", "sales"),
    ("information_technology", "Phòng Công nghệ thông tin", "board"),
    ("safety_environment", "Phòng An toàn và Môi trường", "board"),
    ("planning", "Phòng Kế hoạch sản xuất", "supply_chain"),
]


job_title_specs = [
    ("chief_executive_officer", "Tổng giám đốc", 85000000),
    ("division_director", "Giám đốc khối", 60000000),
    ("department_manager", "Trưởng phòng", 42000000),
    ("factory_manager", "Quản đốc nhà máy", 45000000),
    ("production_supervisor", "Giám sát sản xuất", 26000000),
    ("production_planner", "Chuyên viên kế hoạch sản xuất", 22000000),
    ("production_operator", "Nhân viên vận hành sản xuất", 13500000),
    ("packaging_operator", "Nhân viên đóng gói", 12000000),
    ("warehouse_supervisor", "Giám sát kho", 22000000),
    ("warehouse_keeper", "Thủ kho", 15500000),
    ("forklift_operator", "Nhân viên xe nâng", 13000000),
    ("quality_manager", "Trưởng bộ phận chất lượng", 38000000),
    ("quality_engineer", "Kỹ sư chất lượng", 23000000),
    ("laboratory_technician", "Kỹ thuật viên phòng thí nghiệm", 18000000),
    ("maintenance_engineer", "Kỹ sư bảo trì", 23000000),
    ("maintenance_technician", "Kỹ thuật viên bảo trì", 17000000),
    ("food_engineer", "Kỹ sư công nghệ thực phẩm", 24000000),
    ("research_specialist", "Chuyên viên nghiên cứu sản phẩm", 25000000),
    ("procurement_specialist", "Chuyên viên thu mua", 21000000),
    ("supply_chain_specialist", "Chuyên viên chuỗi cung ứng", 22000000),
    ("human_resources_specialist", "Chuyên viên nhân sự", 19000000),
    ("payroll_specialist", "Chuyên viên tiền lương", 21000000),
    ("accountant", "Kế toán viên", 19000000),
    ("financial_analyst", "Chuyên viên phân tích tài chính", 24000000),
    ("sales_specialist", "Chuyên viên kinh doanh", 20000000),
    ("marketing_specialist", "Chuyên viên marketing", 22000000),
    ("system_administrator", "Quản trị hệ thống", 26000000),
    ("software_engineer", "Kỹ sư phần mềm", 28000000),
    ("safety_specialist", "Chuyên viên an toàn lao động", 20000000),
    ("administrative_staff", "Nhân viên hành chính", 15000000),
]


shift_specs = [
    ("shift_morning", "Ca sáng", "06:00:00", "14:00:00"),
    ("shift_afternoon", "Ca chiều", "14:00:00", "22:00:00"),
    ("shift_night", "Ca đêm", "22:00:00", "06:00:00"),
]


def generate_human_resources() -> dict[str, list[dict]]:
    surnames = ["Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan", "Vũ", "Võ", "Đặng", "Bùi", "Đỗ", "Hồ", "Ngô", "Dương", "Lý", "Trịnh", "Đinh", "Mai", "Tạ"]
    middle_names = ["Văn", "Thị", "Minh", "Ngọc", "Thanh", "Đức", "Quang", "Hoài", "Gia", "Hữu", "Xuân", "Bảo", "Kim", "Khánh", "Phương", "Anh", "Tuấn", "Mạnh", "Nhật", "Thùy", "Hồng", "Đình", "Công", "Trọng", "Diệu"]
    given_names = ["An", "Anh", "Bình", "Châu", "Chi", "Cường", "Dũng", "Duy", "Đạt", "Đức", "Giang", "Hà", "Hải", "Hạnh", "Hiền", "Hiếu", "Hoa", "Hoàng", "Hùng", "Huy", "Huyền", "Khánh", "Khoa", "Lâm", "Lan", "Linh", "Long", "Mai", "Minh", "My", "Nam", "Nga", "Ngân", "Ngọc", "Nhân", "Nhi", "Phong", "Phúc", "Phương", "Quân", "Quang", "Quyên", "Sơn", "Tâm", "Thảo", "Thành", "Thịnh", "Thu", "Thư", "Thủy", "Tiến", "Trang", "Trí", "Trung", "Tú", "Tuấn", "Uyên", "Việt", "Vinh", "Yến"]
    name_pool = [f"{surname} {middle_name} {given_name}" for surname in surnames for middle_name in middle_names for given_name in given_names]
    full_names = random_source.sample(name_pool, 600)
    department_codes = [spec[0] for spec in department_specs]
    department_manager_codes = {code: f"vmk{index + 1:04d}" for index, (code, _, _) in enumerate(department_specs)}
    job_title_salary = {spec[0]: spec[2] for spec in job_title_specs}
    employee_rows = []
    contract_rows = []
    for index in range(600):
        employee_number = index + 1
        employee_code = f"vmk{employee_number:04d}"
        if index < len(department_specs):
            department_code = department_codes[index]
            job_title_code = "chief_executive_officer" if index == 0 else ("factory_manager" if department_code.startswith("production_") else "department_manager")
        else:
            department_code = department_codes[(index * 7 + index // 13) % len(department_codes)]
            if department_code.startswith("production_"):
                job_title_code = random_source.choice(["production_supervisor", "production_operator", "packaging_operator", "food_engineer"])
            elif department_code.startswith("warehouse_"):
                job_title_code = random_source.choice(["warehouse_supervisor", "warehouse_keeper", "forklift_operator"])
            elif department_code in {"quality_assurance", "laboratory"}:
                job_title_code = random_source.choice(["quality_engineer", "laboratory_technician", "food_engineer"])
            elif department_code == "maintenance":
                job_title_code = random_source.choice(["maintenance_engineer", "maintenance_technician"])
            elif department_code == "information_technology":
                job_title_code = random_source.choice(["system_administrator", "software_engineer"])
            else:
                job_title_code = random_source.choice([spec[0] for spec in job_title_specs[4:]])
        if index < 550:
            employment_status = "active"
        elif index < 570:
            employment_status = "on_leave"
        elif index < 590:
            employment_status = "inactive"
        else:
            employment_status = "terminated"
        hired_on = date(2012 + (index % 13), 1 + (index * 5 % 12), 1 + (index * 11 % 25))
        date_of_birth = employee_birth_date(hired_on, index)
        phone_number = employee_phone_number(index)
        terminated_on = date(2025, 1 + (index % 12), 15) if employment_status == "terminated" else None
        manager_code = "" if index < len(department_specs) else department_manager_codes[department_code]
        email_prefix = ascii_slug(full_names[index]).replace(".", "")
        employee_rows.append({
            "employee_code": employee_code,
            "full_name": full_names[index],
            "email": f"{email_prefix}.{employee_number:04d}@vinamilk.test",
            "phone_number": phone_number,
            "phone_number_display": f"{phone_number[:4]} {phone_number[4:7]} {phone_number[7:]}",
            "date_of_birth": date_of_birth.isoformat(),
            "date_of_birth_display": format_date_vi(date_of_birth),
            "department_code": department_code,
            "job_title_code": job_title_code,
            "manager_employee_code": manager_code,
            "employment_status": employment_status,
            "hired_on": hired_on.isoformat(),
            "hired_on_display": format_date_vi(hired_on),
            "terminated_on": terminated_on.isoformat() if terminated_on else "",
            "terminated_on_display": format_date_vi(terminated_on),
            "notes": "Dữ liệu nhân sự tổng hợp phục vụ kiểm thử tải.",
        })
        base_salary = job_title_salary[job_title_code] + (index % 8) * 750000
        contract_status = "active" if employment_status in {"active", "on_leave"} else ("terminated" if employment_status == "terminated" else "expired")
        contract_rows.append({
            "contract_code": f"contract_2024_{employee_number:04d}",
            "employee_code": employee_code,
            "contract_type": "indefinite" if index % 4 else "fixed_term_36_months",
            "effective_from": max(hired_on, date(2024, 1, 1)).isoformat(),
            "effective_to": "" if contract_status == "active" else date(2025, 12, 31).isoformat(),
            "base_salary": base_salary,
            "currency_code": "VND",
            "status": contract_status,
            "notes": "Hợp đồng tổng hợp phục vụ kiểm thử nghiệp vụ.",
        })
    leave_types = [("annual_leave", True), ("sick_leave", True), ("personal_unpaid", False), ("maternity_leave", True), ("family_leave", True)]
    leave_statuses = ["approved", "approved", "approved", "pending", "rejected", "cancelled"]
    leave_rows = []
    for index in range(900):
        employee_index = (index * 17 + 23) % 570
        starts_on = date(2025 + index % 2, 1 + (index * 7 % 12), 1 + (index * 13 % 24))
        leave_type_code, is_paid = leave_types[index % len(leave_types)]
        status = leave_statuses[index % len(leave_statuses)]
        leave_rows.append({
            "request_code": f"leave_2025_{index + 1:05d}",
            "employee_code": f"vmk{employee_index + 1:04d}",
            "leave_type_code": leave_type_code,
            "starts_on": starts_on.isoformat(),
            "ends_on": (starts_on + timedelta(days=index % 3)).isoformat(),
            "is_paid": str(is_paid).lower(),
            "reason": "Nghỉ theo kế hoạch đã đăng ký.",
            "status": status,
            "decided_at": iso_timestamp(starts_on - timedelta(days=3)) if status in {"approved", "rejected"} else "",
            "decision_note": "Approved according to company policy." if status == "approved" else ("Request does not meet current policy." if status == "rejected" else ""),
        })
    reward_reasons = ["Hoàn thành vượt kế hoạch sản xuất", "Sáng kiến cải tiến quy trình", "Tuân thủ an toàn xuất sắc", "Đạt kết quả chất lượng tốt"]
    discipline_reasons = ["Không tuân thủ quy trình bàn giao ca", "Đi muộn nhiều lần", "Sai sót chứng từ kho", "Không hoàn thành đào tạo bắt buộc"]
    reward_rows = []
    for index in range(420):
        event_type = "reward" if index % 4 else "discipline"
        status = ["approved", "approved", "pending", "rejected"][index % 4]
        effective_on = date(2025 + index % 2, 1 + (index * 5 % 12), 1 + (index * 9 % 25))
        reward_rows.append({
            "record_code": f"hr_event_{index + 1:05d}",
            "employee_code": f"vmk{(index * 19 % 570) + 1:04d}",
            "event_type": event_type,
            "effective_on": effective_on.isoformat(),
            "reason": random_source.choice(reward_reasons if event_type == "reward" else discipline_reasons),
            "amount": 500000 + (index % 8) * 250000,
            "currency_code": "VND",
            "status": status,
            "decided_at": iso_timestamp(effective_on + timedelta(days=2)) if status in {"approved", "rejected"} else "",
            "decision_note": "Approved after review." if status == "approved" else ("Rejected after review." if status == "rejected" else ""),
        })
    payroll_rows = []
    for month_value in range(1, 13):
        starts_on = month_start(2026, month_value)
        ends_on = month_end(2026, month_value)
        standard_days = sum(1 for day_number in range(1, ends_on.day + 1) if date(2026, month_value, day_number).weekday() < 6)
        payroll_rows.append({
            "period_code": f"payroll_2026_{month_value:02d}",
            "starts_on": starts_on.isoformat(),
            "ends_on": ends_on.isoformat(),
            "standard_working_days": standard_days,
            "calculation_version": "monthly_mon_sat_v1",
            "status": "draft",
        })
    return {
        "departments": [{"department_code": code, "department_name": name, "parent_department_code": parent} for code, name, parent in department_specs],
        "job_titles": [{"job_title_code": code, "job_title_name": name, "base_salary_reference": salary} for code, name, salary in job_title_specs],
        "work_shifts": [{"shift_code": code, "shift_name": name, "starts_at": starts_at, "ends_at": ends_at} for code, name, starts_at, ends_at in shift_specs],
        "employees": employee_rows,
        "contracts": contract_rows,
        "leave_requests": leave_rows,
        "reward_discipline": reward_rows,
        "payroll_periods": payroll_rows,
    }


def generate_inventory() -> dict[str, list[dict]]:
    category_rows = [
        {"category_code": "dairy_input", "category_name": "Nguyên liệu sữa"},
        {"category_code": "sweetener", "category_name": "Đường và chất tạo ngọt"},
        {"category_code": "culture", "category_name": "Men vi sinh"},
        {"category_code": "vitamin_mineral", "category_name": "Vitamin và khoáng chất"},
        {"category_code": "stabilizer", "category_name": "Chất ổn định thực phẩm"},
        {"category_code": "fruit_flavor", "category_name": "Trái cây và hương liệu"},
        {"category_code": "grain_nut", "category_name": "Ngũ cốc và hạt"},
        {"category_code": "packaging", "category_name": "Bao bì sản xuất"},
        {"category_code": "finished_product", "category_name": "Thành phẩm"},
    ]
    provinces = ["Thành phố Hồ Chí Minh", "Bình Dương", "Đồng Nai", "Long An", "Hà Nội", "Bắc Ninh", "Đà Nẵng", "Cần Thơ"]
    supplier_specialties = ["Nguyên liệu sữa", "Bao bì thực phẩm", "Vitamin và khoáng chất", "Trái cây cô đặc", "Men vi sinh", "Đường thực phẩm"]
    supplier_rows = [{
        "supplier_code": f"supplier_{index + 1:03d}",
        "supplier_name": f"Nhà cung cấp {supplier_specialties[index % len(supplier_specialties)]} {index + 1:02d}",
        "phone_number": "",
        "email": f"supplier.{index + 1:03d}@vinamilk.test",
        "address": provinces[index % len(provinces)],
        "status": "active" if index < 27 else "inactive",
    } for index in range(30)]
    warehouse_rows = [{"warehouse_code": code, "warehouse_name": name, "address": address, "status": "active"} for code, name, address in warehouse_specs]
    locations_by_warehouse = {}
    location_rows = []
    for warehouse_code, _, _ in warehouse_specs:
        locations_by_warehouse[warehouse_code] = []
        for zone_index in range(1, 11):
            location_code = f"{warehouse_code}_z{zone_index:02d}"
            locations_by_warehouse[warehouse_code].append(location_code)
            location_rows.append({
                "warehouse_code": warehouse_code,
                "location_code": location_code,
                "location_name": f"Khu {zone_index:02d}",
                "status": "active",
            })
    stock_item_rows = []
    raw_variants = ["Tiêu chuẩn A", "Tiêu chuẩn B", "Lô nội địa", "Lô nhập khẩu", "Dùng cho sản xuất thử"]
    for index in range(320):
        category_code, base_name, unit_code = raw_material_bases[index % len(raw_material_bases)]
        stock_item_rows.append({
            "item_code": f"rm_{index + 1:04d}",
            "item_name": f"{base_name} - {raw_variants[index // len(raw_material_bases)]}",
            "item_type": "raw_material",
            "category_code": category_code,
            "unit_code": unit_code,
            "lot_controlled": str(category_code != "packaging").lower(),
            "minimum_stock_quantity": 500 if unit_code != "piece" else 5000,
            "status": "active" if index < 310 else "inactive",
            "description": "Danh mục nguyên liệu tổng hợp theo nghiệp vụ doanh nghiệp sữa.",
        })
    package_variants = ["110 ml", "180 ml", "220 ml", "400 ml", "Thùng tiêu chuẩn"]
    for product_index, (product_name, product_category, brand, source_url) in enumerate(product_references):
        for package_index, package_value in enumerate(package_variants):
            item_index = product_index * len(package_variants) + package_index + 1
            stock_item_rows.append({
                "item_code": f"fg_{item_index:04d}",
                "item_name": f"{product_name} - {package_value}",
                "item_type": "finished_product",
                "category_code": "finished_product",
                "unit_code": "piece",
                "lot_controlled": "true",
                "minimum_stock_quantity": 1000,
                "status": "active",
                "description": f"Tham chiếu công khai: {brand}; nhóm {product_category}; {source_url}",
            })
    lot_rows = []
    for item_index, item in enumerate(stock_item_rows):
        if item["lot_controlled"] == "true":
            for lot_index in range(2):
                manufactured_on = date(2025 + (item_index + lot_index) % 2, 1 + (item_index * 3 + lot_index) % 12, 1 + (item_index * 7 + lot_index) % 24)
                shelf_life_days = 365 if item["item_type"] == "finished_product" else 540
                lot_rows.append({
                    "item_code": item["item_code"],
                    "lot_code": f"lot_{item['item_code']}_{lot_index + 1:02d}",
                    "manufactured_on": manufactured_on.isoformat(),
                    "expires_on": (manufactured_on + timedelta(days=shelf_life_days)).isoformat(),
                    "status": "active",
                })
    lots_by_item = defaultdict(list)
    for lot in lot_rows:
        lots_by_item[lot["item_code"]].append(lot["lot_code"])
    stock_seed_entries = []
    receipt_rows = []
    receipt_line_rows = []
    stock_movements = []
    balance_map = {}
    for index in range(650):
        receipt_code = f"receipt_2025_{index + 1:05d}"
        warehouse_code = warehouse_specs[index % len(warehouse_specs)][0]
        receipt_day = date(2025 + index % 2, 1 + (index * 5 % 12), 1 + (index * 11 % 24))
        receipt_rows.append({
            "receipt_code": receipt_code,
            "warehouse_code": warehouse_code,
            "supplier_code": f"supplier_{index % 30 + 1:03d}",
            "source_module": "",
            "source_document_code": "",
            "reference_number": f"purchase_order_{2025 + index % 2}_{index + 1:05d}",
            "status": "posted",
            "idempotency_key": f"receipt_{2025 + index % 2}_{index + 1:05d}",
            "posted_at": iso_timestamp(receipt_day, 9),
            "notes": "Phiếu nhập dữ liệu tải lớn.",
        })
        for line_index in range(3):
            item = stock_item_rows[(index * 3 + line_index) % len(stock_item_rows)]
            lot_options = lots_by_item[item["item_code"]]
            lot_code = lot_options[(index + line_index) % len(lot_options)] if lot_options else ""
            location_code = locations_by_warehouse[warehouse_code][(index + line_index) % 10]
            quantity = 8000 + ((index * 37 + line_index * 101) % 12000)
            receipt_line_rows.append({
                "receipt_code": receipt_code,
                "line_number": line_index + 1,
                "item_code": item["item_code"],
                "location_code": location_code,
                "lot_code": lot_code,
                "quantity": quantity,
            })
            stock_seed_entries.append({"item_code": item["item_code"], "warehouse_code": warehouse_code, "location_code": location_code, "lot_code": lot_code, "quantity": quantity})
            stock_movements.append({
                "source_document_type": "receipt",
                "source_document_code": receipt_code,
                "source_document_line_number": line_index + 1,
                "movement_leg": "single",
                "movement_type": "receipt",
                "item_code": item["item_code"],
                "location_code": location_code,
                "lot_code": lot_code,
                "quantity_delta": quantity,
                "idempotency_key": f"receipt_post_{index + 1:05d}_{line_index + 1}",
            })
            add_balance(balance_map, item["item_code"], location_code, lot_code, quantity)
    raw_item_codes = {item["item_code"] for item in stock_item_rows[:320]}
    raw_stock_seed_entries = [entry for entry in stock_seed_entries if entry["item_code"] in raw_item_codes]
    issue_rows = []
    issue_line_rows = []
    for index in range(420):
        issue_code = f"issue_2026_{index + 1:05d}"
        first_candidate = raw_stock_seed_entries[(index * 2) % len(raw_stock_seed_entries)]
        issue_rows.append({
            "issue_code": issue_code,
            "warehouse_code": first_candidate["warehouse_code"],
            "source_module": "production",
            "source_document_code": f"production_order_2026_{index + 1:05d}",
            "reason_code": "production_consumption",
            "status": "posted",
            "idempotency_key": f"issue_2026_{index + 1:05d}",
            "posted_at": iso_timestamp(date(2026, 1 + (index * 7 % 9), 1 + (index * 13 % 24)), 7),
            "notes": "Xuất nguyên liệu cho sản xuất.",
        })
        for line_index in range(2):
            candidate = raw_stock_seed_entries[(index * 2 + line_index) % len(raw_stock_seed_entries)]
            quantity = 10 + ((index + line_index * 3) % 40)
            issue_line_rows.append({
                "issue_code": issue_code,
                "line_number": line_index + 1,
                "item_code": candidate["item_code"],
                "location_code": candidate["location_code"],
                "lot_code": candidate["lot_code"],
                "quantity": quantity,
            })
            stock_movements.append({
                "source_document_type": "issue",
                "source_document_code": issue_code,
                "source_document_line_number": line_index + 1,
                "movement_leg": "single",
                "movement_type": "issue",
                "item_code": candidate["item_code"],
                "location_code": candidate["location_code"],
                "lot_code": candidate["lot_code"],
                "quantity_delta": -quantity,
                "idempotency_key": f"issue_post_{index + 1:05d}_{line_index + 1}",
            })
            add_balance(balance_map, candidate["item_code"], candidate["location_code"], candidate["lot_code"], -quantity)
    transfer_rows = []
    transfer_line_rows = []
    for index in range(250):
        candidate = raw_stock_seed_entries[(index * 5) % len(raw_stock_seed_entries)]
        source_warehouse_code = candidate["warehouse_code"]
        destination_warehouse_code = warehouse_specs[(index + 1) % len(warehouse_specs)][0]
        if destination_warehouse_code == source_warehouse_code:
            destination_warehouse_code = warehouse_specs[(index + 2) % len(warehouse_specs)][0]
        transfer_code = f"transfer_2026_{index + 1:05d}"
        quantity = 5 + index % 20
        destination_location_code = locations_by_warehouse[destination_warehouse_code][index % 10]
        transfer_rows.append({
            "transfer_code": transfer_code,
            "source_warehouse_code": source_warehouse_code,
            "destination_warehouse_code": destination_warehouse_code,
            "status": "posted",
            "idempotency_key": f"transfer_2026_{index + 1:05d}",
            "posted_at": iso_timestamp(date(2026, 1 + (index * 3 % 9), 1 + (index * 7 % 24)), 13),
            "notes": "Điều chuyển cân đối tồn kho.",
        })
        transfer_line_rows.append({
            "transfer_code": transfer_code,
            "line_number": 1,
            "item_code": candidate["item_code"],
            "lot_code": candidate["lot_code"],
            "source_location_code": candidate["location_code"],
            "destination_location_code": destination_location_code,
            "quantity": quantity,
        })
        for movement_leg, location_code, signed_quantity in [("source", candidate["location_code"], -quantity), ("destination", destination_location_code, quantity)]:
            stock_movements.append({
                "source_document_type": "transfer",
                "source_document_code": transfer_code,
                "source_document_line_number": 1,
                "movement_leg": movement_leg,
                "movement_type": "transfer",
                "item_code": candidate["item_code"],
                "location_code": location_code,
                "lot_code": candidate["lot_code"],
                "quantity_delta": signed_quantity,
                "idempotency_key": f"transfer_post_{index + 1:05d}_{movement_leg}",
            })
            add_balance(balance_map, candidate["item_code"], location_code, candidate["lot_code"], signed_quantity)
    return {
        "categories": category_rows,
        "suppliers": supplier_rows,
        "warehouses": warehouse_rows,
        "locations": location_rows,
        "stock_items": stock_item_rows,
        "lots": lot_rows,
        "receipts": receipt_rows,
        "receipt_lines": receipt_line_rows,
        "issues": issue_rows,
        "issue_lines": issue_line_rows,
        "transfers": transfer_rows,
        "transfer_lines": transfer_line_rows,
        "stock_movements": stock_movements,
        "stock_balances": [],
        "_balance_map": balance_map,
        "_locations_by_warehouse": locations_by_warehouse,
    }


def generate_production(human_resources_data: dict[str, list[dict]], inventory_data: dict[str, list[dict]]) -> dict[str, list[dict]]:
    finished_items = [row for row in inventory_data["stock_items"] if row["item_type"] == "finished_product"]
    raw_items = [row for row in inventory_data["stock_items"] if row["item_type"] == "raw_material" and row["status"] == "active"]
    unit_by_item = {row["item_code"]: row["unit_code"] for row in inventory_data["stock_items"]}
    production_employee_codes = [row["employee_code"] for row in human_resources_data["employees"] if row["department_code"].startswith("production_") and row["employment_status"] == "active"]
    plan_rows = []
    plan_line_rows = []
    plan_start_by_code = {}
    for index in range(250):
        plan_code = f"plan_2026_{index + 1:04d}"
        starts_on = date(2026, 1 + index % 9, 1 + (index * 3 % 20))
        plan_start_by_code[plan_code] = starts_on
        plan_rows.append({
            "plan_code": plan_code,
            "plan_name": f"Kế hoạch sản xuất tuần {index + 1:03d}",
            "planned_on": (starts_on - timedelta(days=14)).isoformat(),
            "starts_on": starts_on.isoformat(),
            "ends_on": (starts_on + timedelta(days=6)).isoformat(),
            "status": ["approved", "released", "completed", "draft"][index % 4],
            "notes": "Kế hoạch dữ liệu tải lớn.",
        })
        for line_index in range(2):
            product = finished_items[(index * 2 + line_index) % len(finished_items)]
            plan_line_rows.append({
                "plan_code": plan_code,
                "line_number": line_index + 1,
                "item_code": product["item_code"],
                "unit_code": "piece",
                "target_quantity": 10000 + ((index * 211 + line_index * 1000) % 30000),
                "required_on": (starts_on + timedelta(days=5 + line_index)).isoformat(),
                "notes": "Sản lượng kế hoạch theo tuần.",
            })
    bom_rows = []
    bom_line_rows = []
    bom_lines_by_item = {}
    for index, product in enumerate(finished_items):
        bom_code = f"bom_{product['item_code']}"
        bom_rows.append({
            "bom_code": bom_code,
            "item_code": product["item_code"],
            "version_number": 1,
            "base_quantity": 1000,
            "unit_code": "piece",
            "valid_from": "2025-01-01",
            "valid_to": "",
            "status": "active",
            "notes": "Định mức mô phỏng cho một nghìn đơn vị thành phẩm.",
        })
        bom_lines_by_item[product["item_code"]] = []
        for line_index in range(5):
            material = raw_items[(index * 5 + line_index * 17) % len(raw_items)]
            bom_line_code = f"{bom_code}_line_{line_index + 1:02d}"
            bom_lines_by_item[product["item_code"]].append(bom_line_code)
            bom_line_rows.append({
                "bom_line_code": bom_line_code,
                "bom_code": bom_code,
                "version_number": 1,
                "line_number": line_index + 1,
                "material_item_code": material["item_code"],
                "unit_code": material["unit_code"],
                "quantity_per_base": 30 + ((index * 13 + line_index * 29) % 700),
                "scrap_percent": [0.5, 1.0, 1.5, 2.0, 0.0][line_index],
                "notes": "Định mức nguyên liệu theo mẻ chuẩn.",
            })
    order_rows = []
    requirement_rows = []
    event_rows = []
    assignment_rows = []
    consumption_rows = []
    output_rows = []
    plan_lines_by_code = {f"{row['plan_code']}:{row['line_number']}": row for row in plan_line_rows}
    issue_line_rows_by_order = defaultdict(list)
    for row in inventory_data["issue_lines"]:
        issue_line_rows_by_order[row["issue_code"]].append(row)
    for index in range(600):
        plan_line = plan_line_rows[index % len(plan_line_rows)]
        order_code = f"production_order_2026_{index + 1:05d}"
        planned_start = plan_start_by_code[plan_line["plan_code"]] + timedelta(days=index % 4)
        order_status = ["planned", "released", "in_progress", "completed", "paused"][index % 5]
        bom_code = f"bom_{plan_line['item_code']}"
        order_rows.append({
            "order_code": order_code,
            "plan_code": plan_line["plan_code"],
            "plan_line_number": plan_line["line_number"],
            "item_code": plan_line["item_code"],
            "bom_code": bom_code,
            "bom_version_number": 1,
            "target_quantity": 2000 + (index * 137 % 8000),
            "unit_code": "piece",
            "planned_starts_on": planned_start.isoformat(),
            "planned_ends_on": (planned_start + timedelta(days=2)).isoformat(),
            "production_line_name": f"Dây chuyền {index % 12 + 1:02d}",
            "status": order_status,
            "notes": "Lệnh sản xuất dữ liệu tải lớn.",
        })
        for line_index, bom_line_code in enumerate(bom_lines_by_item[plan_line["item_code"]]):
            bom_line = next(row for row in bom_line_rows if row["bom_line_code"] == bom_line_code)
            required_quantity = round(float(bom_line["quantity_per_base"]) * float(order_rows[-1]["target_quantity"]) / 1000 * (1 + float(bom_line["scrap_percent"]) / 100), 6)
            requirement_rows.append({
                "order_code": order_code,
                "bom_code": bom_code,
                "bom_line_code": bom_line_code,
                "material_item_code": bom_line["material_item_code"],
                "unit_code": bom_line["unit_code"],
                "base_quantity_snapshot": 1000,
                "scrap_percent_snapshot": bom_line["scrap_percent"],
                "required_quantity": required_quantity,
            })
        event_rows.extend([
            {"order_code": order_code, "event_type": "status_changed", "previous_status": "draft", "new_status": order_status, "note": "Tạo và cập nhật lệnh theo kế hoạch.", "occurred_at": iso_timestamp(planned_start - timedelta(days=1), 8), "idempotency_key": f"order_event_status_{index + 1:05d}"},
            {"order_code": order_code, "event_type": "progress_note", "previous_status": order_status, "new_status": order_status, "note": "Đã cập nhật tiến độ ca sản xuất.", "occurred_at": iso_timestamp(planned_start + timedelta(days=1), 16), "idempotency_key": f"order_event_progress_{index + 1:05d}"},
        ])
        assignment_count = 1 if index % 2 else 2
        for assignment_index in range(assignment_count):
            employee_code = production_employee_codes[(index * 3 + assignment_index) % len(production_employee_codes)]
            shift_code, _, start_text, _ = shift_specs[(index + assignment_index) % len(shift_specs)]
            start_hour = int(start_text[:2])
            start_datetime = datetime.combine(planned_start, time(start_hour), tzinfo=utc_plus_seven)
            assignment_rows.append({
                "order_code": order_code,
                "employee_code": employee_code,
                "shift_code": shift_code,
                "assignment_name": ["Vận hành thiết bị", "Kiểm soát đóng gói", "Bàn giao ca"][(index + assignment_index) % 3],
                "starts_at": start_datetime.isoformat(),
                "ends_at": (start_datetime + timedelta(hours=8)).isoformat(),
                "status": "completed" if order_status == "completed" else ("active" if order_status == "in_progress" else "planned"),
                "notes": "Phân công theo ca sản xuất.",
            })
        issue_code = f"issue_2026_{index % 420 + 1:05d}"
        for consumption_index, issue_line in enumerate(issue_line_rows_by_order[issue_code][:2]):
            consumption_rows.append({
                "order_code": order_code,
                "material_item_code": issue_line["item_code"],
                "inventory_issue_code": issue_code,
                "inventory_issue_line_number": issue_line["line_number"],
                "unit_code": unit_by_item[issue_line["item_code"]],
                "consumed_quantity": round(float(issue_line["quantity"]) * (0.92 + (index % 5) * 0.01), 6),
                "consumed_at": iso_timestamp(planned_start + timedelta(days=1), 10 + consumption_index),
                "idempotency_key": f"material_consumption_{index + 1:05d}_{consumption_index + 1}",
                "notes": "Tiêu hao thực tế đối chiếu với phiếu xuất.",
            })
        manufactured_on = planned_start + timedelta(days=1)
        output_status = "received" if index < 300 else ("pending_receipt" if index < 480 else "draft")
        lot_code = f"production_lot_2026_{index + 1:05d}"
        warehouse_code = "wh_finished_south" if index % 2 == 0 else "wh_finished_north"
        location_code = inventory_data["_locations_by_warehouse"][warehouse_code][index % 10]
        receipt_code = f"receipt_production_2026_{index + 1:05d}" if output_status == "received" else ""
        output_rows.append({
            "order_code": order_code,
            "item_code": plan_line["item_code"],
            "lot_code": lot_code,
            "manufactured_on": manufactured_on.isoformat(),
            "expires_on": (manufactured_on + timedelta(days=240)).isoformat(),
            "good_quantity": 1800 + (index * 101 % 7000),
            "defective_quantity": index % 17,
            "status": output_status,
            "idempotency_key": f"production_output_{index + 1:05d}",
            "inventory_receipt_code": receipt_code,
            "warehouse_code": warehouse_code,
            "location_code": location_code,
            "notes": "Sản lượng thành phẩm theo lô.",
        })
        if output_status == "received":
            inventory_data["lots"].append({"item_code": plan_line["item_code"], "lot_code": lot_code, "manufactured_on": manufactured_on.isoformat(), "expires_on": (manufactured_on + timedelta(days=240)).isoformat(), "status": "active"})
            inventory_data["receipts"].append({
                "receipt_code": receipt_code,
                "warehouse_code": warehouse_code,
                "supplier_code": "",
                "source_module": "production",
                "source_document_code": order_code,
                "reference_number": order_code,
                "status": "posted",
                "idempotency_key": f"receipt_production_2026_{index + 1:05d}",
                "posted_at": iso_timestamp(manufactured_on, 18),
                "notes": "Bàn giao thành phẩm đạt từ Sản xuất sang Kho.",
            })
            inventory_data["receipt_lines"].append({"receipt_code": receipt_code, "line_number": 1, "item_code": plan_line["item_code"], "location_code": location_code, "lot_code": lot_code, "quantity": output_rows[-1]["good_quantity"]})
            inventory_data["stock_movements"].append({
                "source_document_type": "receipt",
                "source_document_code": receipt_code,
                "source_document_line_number": 1,
                "movement_leg": "single",
                "movement_type": "receipt",
                "item_code": plan_line["item_code"],
                "location_code": location_code,
                "lot_code": lot_code,
                "quantity_delta": output_rows[-1]["good_quantity"],
                "idempotency_key": f"receipt_production_post_{index + 1:05d}",
            })
            add_balance(inventory_data["_balance_map"], plan_line["item_code"], location_code, lot_code, output_rows[-1]["good_quantity"])
    return {
        "plans": plan_rows,
        "plan_lines": plan_line_rows,
        "boms": bom_rows,
        "bom_lines": bom_line_rows,
        "orders": order_rows,
        "material_requirements": requirement_rows,
        "order_events": event_rows,
        "assignments": assignment_rows,
        "material_consumptions": consumption_rows,
        "outputs": output_rows,
    }


def finalize_inventory(inventory_data: dict[str, list[dict]]) -> None:
    inventory_data["stock_balances"] = [
        {"item_code": item_code, "location_code": location_code, "lot_code": lot_code, "on_hand_quantity": quantity}
        for (item_code, location_code, lot_code), quantity in sorted(inventory_data["_balance_map"].items())
        if quantity > 0
    ]


def write_collection(directory_name: str, collection_name: str, rows: list[dict]) -> None:
    if not rows:
        return
    write_csv(f"{directory_name}/{collection_name}.csv", list(rows[0]), rows)


def main() -> None:
    human_resources_data = generate_human_resources()
    inventory_data = generate_inventory()
    production_data = generate_production(human_resources_data, inventory_data)
    finalize_inventory(inventory_data)

    write_csv("reference/vinamilk_product_catalog.csv", ["product_name", "product_category", "brand", "source_url"], [
        {"product_name": row[0], "product_category": row[1], "brand": row[2], "source_url": row[3]} for row in product_references
    ])
    for collection_name, rows in human_resources_data.items():
        write_collection("human_resources", collection_name, rows)
    for collection_name, rows in inventory_data.items():
        if not collection_name.startswith("_"):
            write_collection("inventory", collection_name, rows)
    for collection_name, rows in production_data.items():
        write_collection("production", collection_name, rows)

    manifest = {
        "data_version": "large_seed_2026_09",
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "random_seed": seed_value,
        "synthetic_personal_data": True,
        "public_product_reference_count": len(product_references),
        "modules": {
            "human_resources": {name: len(rows) for name, rows in human_resources_data.items()},
            "inventory": {name: len(rows) for name, rows in inventory_data.items() if not name.startswith("_")},
            "production": {name: len(rows) for name, rows in production_data.items()},
        },
    }
    (data_root / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(manifest, ensure_ascii=False, indent=2))


raw_material_bases = [
    ("dairy_input", "Sữa bò tươi nguyên liệu", "litre"), ("dairy_input", "Sữa bột gầy", "kg"),
    ("dairy_input", "Sữa bột nguyên kem", "kg"), ("dairy_input", "Whey bột", "kg"),
    ("dairy_input", "Đạm whey cô đặc", "kg"), ("dairy_input", "Casein sữa", "kg"),
    ("dairy_input", "Chất béo sữa khan", "kg"), ("dairy_input", "Kem sữa", "litre"),
    ("dairy_input", "Lactose thực phẩm", "kg"), ("dairy_input", "Sữa non dạng bột", "kg"),
    ("sweetener", "Đường tinh luyện", "kg"), ("sweetener", "Đường glucose", "kg"),
    ("sweetener", "Siro glucose", "litre"), ("sweetener", "Maltodextrin", "kg"),
    ("sweetener", "Chất tạo ngọt stevia", "kg"), ("culture", "Men Streptococcus thermophilus", "kg"),
    ("culture", "Men Lactobacillus bulgaricus", "kg"), ("culture", "Men Bifidobacterium", "kg"),
    ("culture", "Men Lactobacillus casei", "kg"), ("vitamin_mineral", "Premix vitamin A D E", "kg"),
    ("vitamin_mineral", "Premix vitamin nhóm B", "kg"), ("vitamin_mineral", "Canxi carbonate", "kg"),
    ("vitamin_mineral", "Canxi phosphate", "kg"), ("vitamin_mineral", "Kẽm sulfate", "kg"),
    ("vitamin_mineral", "Sắt fumarate", "kg"), ("vitamin_mineral", "Dầu cá DHA", "kg"),
    ("vitamin_mineral", "Bột collagen thủy phân", "kg"), ("stabilizer", "Pectin thực phẩm", "kg"),
    ("stabilizer", "Carrageenan thực phẩm", "kg"), ("stabilizer", "Guar gum thực phẩm", "kg"),
    ("stabilizer", "Xanthan gum thực phẩm", "kg"), ("stabilizer", "Chất xơ hòa tan inulin", "kg"),
    ("fruit_flavor", "Dịch cô đặc cam", "litre"), ("fruit_flavor", "Dịch cô đặc táo", "litre"),
    ("fruit_flavor", "Dịch cô đặc đào", "litre"), ("fruit_flavor", "Dịch cô đặc dâu", "litre"),
    ("fruit_flavor", "Dịch cô đặc nho", "litre"), ("fruit_flavor", "Dịch cô đặc dừa", "litre"),
    ("fruit_flavor", "Hương vani thực phẩm", "kg"), ("fruit_flavor", "Bột cacao", "kg"),
    ("grain_nut", "Hạt đậu nành", "kg"), ("grain_nut", "Hạt hạnh nhân", "kg"),
    ("grain_nut", "Hạt óc chó", "kg"), ("grain_nut", "Hạt mắc ca", "kg"),
    ("grain_nut", "Yến mạch", "kg"), ("grain_nut", "Gạo lứt", "kg"),
    ("grain_nut", "Ngũ cốc phối trộn", "kg"), ("grain_nut", "Thạch dừa", "kg"),
    ("packaging", "Hộp giấy tiệt trùng 110 ml", "piece"), ("packaging", "Hộp giấy tiệt trùng 180 ml", "piece"),
    ("packaging", "Hộp giấy tiệt trùng 1 lít", "piece"), ("packaging", "Chai nhựa thực phẩm 150 ml", "piece"),
    ("packaging", "Chai nhựa thực phẩm 450 ml", "piece"), ("packaging", "Cốc sữa chua 100 g", "piece"),
    ("packaging", "Nắp nhôm cốc sữa chua", "piece"), ("packaging", "Nắp chai HDPE", "piece"),
    ("packaging", "Ống hút giấy", "piece"), ("packaging", "Màng co lốc sản phẩm", "kg"),
    ("packaging", "Thùng carton 12 sản phẩm", "piece"), ("packaging", "Thùng carton 24 sản phẩm", "piece"),
    ("packaging", "Thùng carton 48 sản phẩm", "piece"), ("packaging", "Nhãn cuộn sản phẩm", "piece"),
    ("packaging", "Pallet nhựa thực phẩm", "piece"), ("packaging", "Túi sữa tiệt trùng", "piece"),
]

warehouse_specs = [
    ("wh_raw_south", "Kho nguyên liệu Nhà máy Sữa Việt Nam", "Lô A-4,5,6,7-CN đường NA7, KCN Mỹ Phước 2, Bình Dương"),
    ("wh_raw_north", "Xí nghiệp Kho vận Hà Nội", "Km 10 Quốc lộ 5, xã Dương Xá, huyện Gia Lâm, Hà Nội"),
    ("wh_finished_south", "Kho thành phẩm Nhà máy Sữa Thống Nhất", "12 Đặng Văn Bi, phường Trường Thọ, TP Thủ Đức, TP Hồ Chí Minh"),
    ("wh_finished_north", "Kho thành phẩm Tiên Sơn", "Khu công nghiệp Tiên Sơn, tỉnh Bắc Ninh"),
    ("wh_cold", "Trung tâm sữa tươi nguyên liệu Củ Chi", "Khu công nghiệp Đông Nam, huyện Củ Chi, TP Hồ Chí Minh"),
    ("wh_packaging", "Kho bao bì Nhà máy Sữa Bột Việt Nam", "Số 9 Đại lộ Tự Do, KCN Việt Nam - Singapore 1, Bình Dương"),
]


def stock_key(item_code: str, location_code: str, lot_code: str) -> tuple[str, str, str]:
    return item_code, location_code, lot_code


def add_balance(balance_map: dict[tuple[str, str, str], float], item_code: str, location_code: str, lot_code: str, quantity: float) -> None:
    key = stock_key(item_code, location_code, lot_code)
    balance_map[key] = round(balance_map.get(key, 0.0) + quantity, 6)


if __name__ == "__main__":
    main()
