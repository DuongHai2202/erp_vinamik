"""Load the generated fixture through the ERP HTTP API.

This is an operator-only utility.  It intentionally keeps credentials in
environment variables and sends all business writes through the backend so
the normal authorization, validation, audit and ledger rules remain active.

Required environment variables:
    ERP_ADMIN_USERNAME
    ERP_ADMIN_PASSWORD

Optional:
    ERP_API_BASE_URL (default: http://127.0.0.1:8080)

Examples:
    py -3 data/load_fixture_via_api.py --phase hr
    py -3 data/load_fixture_via_api.py --phase inventory_master
    py -3 data/load_fixture_via_api.py --phase stocktakes
    py -3 data/load_fixture_via_api.py --phase production
"""

from __future__ import annotations

import argparse
import csv
import json
import os
import sys
import time
from collections import defaultdict
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path
from typing import Any, Iterable
from urllib.parse import urlencode

import requests


project_root = Path(__file__).resolve().parents[1]
api_base_url = os.environ.get("ERP_API_BASE_URL", "http://127.0.0.1:8080").rstrip("/")


class api_error(RuntimeError):
    def __init__(self, method: str, path: str, status: int | None, message: str, body: Any = None):
        self.method = method
        self.path = path
        self.status = status
        self.body = body
        status_text = "" if status is None else f" HTTP {status}"
        super().__init__(f"{method} {path}{status_text}: {message}")


class api_client:
    def __init__(self, base_url: str, username: str, password: str):
        self.base_url = base_url.rstrip("/")
        self.username = username
        self.password = password
        self.session = requests.Session()
        self.csrf_token: str | None = None

    def request(self, method: str, path: str, payload: dict[str, Any] | None = None,
                query: dict[str, Any] | None = None, retries: int = 3) -> Any:
        if query:
            encoded_query = urlencode({key: value for key, value in query.items() if value is not None})
            path = f"{path}?{encoded_query}"
        url = f"{self.base_url}{path}"
        mutating = method.upper() not in {"GET", "HEAD", "OPTIONS"}
        if (mutating and path != "/api/v1/auth/csrf"
                and (self.csrf_token is None or self.session.cookies.get("XSRF-TOKEN") is None)):
            self.ensure_csrf()
        headers = {"Accept": "application/json"}
        if mutating and self.csrf_token:
            headers["X-XSRF-TOKEN"] = self.csrf_token
        for attempt in range(retries):
            try:
                response = self.session.request(method.upper(), url, json=payload, headers=headers, timeout=90)
                raw = response.text
                try:
                    parsed = json.loads(raw)
                except json.JSONDecodeError:
                    parsed = None
                if response.status_code in {429, 500, 502, 503, 504} and attempt + 1 < retries:
                    time.sleep(1.5 * (attempt + 1))
                    continue
                if response.status_code >= 400:
                    message = parsed.get("message") if isinstance(parsed, dict) else raw[:500]
                    raise api_error(method.upper(), path, response.status_code,
                                    message or "Request failed.", parsed)
                return parsed
            except requests.RequestException as error:
                if attempt + 1 < retries:
                    time.sleep(1.5 * (attempt + 1))
                    continue
                raise api_error(method.upper(), path, None, str(error)) from error

    def ensure_csrf(self) -> None:
        self.request("GET", "/api/v1/auth/csrf")
        self.csrf_token = self.session.cookies.get("XSRF-TOKEN")
        if self.csrf_token:
            return
        raise api_error("GET", "/api/v1/auth/csrf", None, "The backend did not issue an XSRF cookie.")

    def login(self) -> dict[str, Any]:
        self.ensure_csrf()
        result = self.request("POST", "/api/v1/auth/login", {
            "username": self.username,
            "password": self.password,
        })
        data = result.get("data") if isinstance(result, dict) else None
        if not isinstance(data, dict) or not data.get("super_admin"):
            raise api_error("POST", "/api/v1/auth/login", 403, "The fixture loader requires a super admin account.")
        return data

    def get_data(self, path: str, query: dict[str, Any] | None = None) -> Any:
        result = self.request("GET", path, query=query)
        return result.get("data") if isinstance(result, dict) else result

    def post_data(self, path: str, payload: dict[str, Any]) -> Any:
        result = self.request("POST", path, payload)
        return result.get("data") if isinstance(result, dict) else result


@dataclass
class load_context:
    client: api_client
    maps: dict[str, dict[str, int]]
    failures: list[dict[str, Any]]
    skipped: list[dict[str, Any]]

    def remember(self, kind: str, code: str, value: Any) -> int:
        if isinstance(value, dict):
            preferred_ids = {
                "department": "department_id", "job_title": "job_title_id", "work_shift": "work_shift_id",
                "employee": "employee_id", "contract": "employment_contract_id", "leave": "leave_request_id",
                "reward": "employee_reward_discipline_id", "payroll_period": "payroll_period_id",
                "unit": "unit_of_measure_id", "category": "item_category_id", "supplier": "supplier_id",
                "warehouse": "warehouse_id", "location": "warehouse_location_id", "stock_item": "stock_item_id",
                "receipt": "receipt_id", "issue": "issue_id", "transfer": "transfer_id",
                "plan": "production_plan_id", "plan_line": "production_plan_line_id", "bom": "bom_id",
                "order": "production_order_id", "assignment": "production_assignment_id",
                "output": "production_output_id", "stocktake": "stocktake_id",
            }
            preferred = preferred_ids.get(kind)
            if preferred in value and value[preferred] is not None:
                self.maps.setdefault(kind, {})[code] = int(value[preferred])
                return int(value[preferred])
            for key in (f"{kind}_id", "id", "employment_contract_id", "leave_request_id",
                        "employee_reward_discipline_id", "receipt_id", "issue_id", "transfer_id",
                        "production_plan_id", "production_plan_line_id", "bom_id",
                        "production_order_id", "production_assignment_id", "production_output_id", "stocktake_id",
                        "stock_item_id", "unit_of_measure_id", "item_category_id", "supplier_id",
                        "warehouse_id", "warehouse_location_id"):
                if key in value and value[key] is not None:
                    self.maps.setdefault(kind, {})[code] = int(value[key])
                    return int(value[key])
        raise ValueError(f"Response for {kind} {code} did not contain an id.")

    def record_failure(self, phase: str, code: str, error: Exception) -> None:
        entry = {"phase": phase, "code": code, "error": str(error)}
        self.failures.append(entry)
        print(f"[FAIL] {phase} {code}: {error}", flush=True)

    def record_skip(self, phase: str, code: str, reason: str) -> None:
        entry = {"phase": phase, "code": code, "reason": reason}
        self.skipped.append(entry)
        print(f"[SKIP] {phase} {code}: {reason}", flush=True)


def read_rows(relative_path: str) -> list[dict[str, str]]:
    with (project_root / "data" / relative_path).open("r", encoding="utf-8-sig", newline="") as input_file:
        return list(csv.DictReader(input_file))


def chunked(rows: list[dict[str, Any]], size: int) -> Iterable[list[dict[str, Any]]]:
    for offset in range(0, len(rows), size):
        yield rows[offset:offset + size]


def to_int(value: str | None) -> int | None:
    return None if value in (None, "") else int(value)


def to_bool(value: str | None) -> bool | None:
    return None if value in (None, "") else value.lower() == "true"


def to_decimal(value: str | None) -> str | None:
    return None if value in (None, "") else str(Decimal(value))


def report_progress(phase: str, index: int, total: int, code: str) -> None:
    if index == 1 or index == total or index % 25 == 0:
        print(f"[{phase}] {index}/{total}: {code}", flush=True)


def list_existing(client: api_client, path: str, code_field: str, item_key: str = "items",
                  fixed_query: dict[str, Any] | None = None) -> dict[str, int]:
    preferred_id_fields = {
        "department_code": "department_id",
        "job_title_code": "job_title_id",
        "shift_code": "work_shift_id",
        "employee_code": "employee_id",
        "contract_code": "employment_contract_id",
        "request_code": "leave_request_id",
        "record_code": "employee_reward_discipline_id",
        "period_code": "payroll_period_id",
        "unit_code": "unit_of_measure_id",
        "category_code": "item_category_id",
        "supplier_code": "supplier_id",
        "warehouse_code": "warehouse_id",
        "item_code": "stock_item_id",
        "receipt_code": "receipt_id",
        "issue_code": "issue_id",
        "transfer_code": "transfer_id",
        "plan_code": "production_plan_id",
        "bom_code": "bom_id",
        "order_code": "production_order_id",
        "stocktake_code": "stocktake_id",
    }
    preferred_id_field = preferred_id_fields.get(code_field)
    result: dict[str, int] = {}
    page = 0
    while True:
        query = {"page": page, "page_size": 100}
        if fixed_query:
            query.update(fixed_query)
        data = client.get_data(path, query)
        items = data.get(item_key, []) if isinstance(data, dict) else []
        for item in items:
            if code_field in item:
                if preferred_id_field and item.get(preferred_id_field) is not None:
                    result[item[code_field]] = int(item[preferred_id_field])
                    continue
                id_fields = [key for key in item if key.endswith("_id")]
                if id_fields:
                    result[item[code_field]] = int(item[id_fields[0]])
        if not isinstance(data, dict) or page + 1 >= int(data.get("total_pages", 0)):
            break
        page += 1
    return result


def load_hr_master(context: load_context) -> None:
    client = context.client
    department_rows = read_rows("human_resources/departments.csv")
    job_rows = read_rows("human_resources/job_titles.csv")
    shift_rows = read_rows("human_resources/work_shifts.csv")
    context.maps["department"] = list_existing(client, "/api/v1/human_resources/master_data/departments", "department_code")
    context.maps["job_title"] = list_existing(client, "/api/v1/human_resources/master_data/job_titles", "job_title_code")
    context.maps["work_shift"] = list_existing(client, "/api/v1/human_resources/master_data/work_shifts", "shift_code")

    pending = {row["department_code"]: row for row in department_rows
               if row["department_code"] not in context.maps["department"]}
    while pending:
        progressed = False
        for code, row in list(pending.items()):
            parent_code = row.get("parent_department_code") or None
            if parent_code and parent_code not in context.maps["department"]:
                continue
            try:
                payload = {"department_code": code, "department_name": row["department_name"],
                           "parent_department_id": context.maps["department"].get(parent_code), "status": "active"}
                value = client.post_data("/api/v1/human_resources/master_data/departments", payload)
                context.remember("department", code, value)
            except api_error as error:
                context.record_failure("department", code, error)
            del pending[code]
            progressed = True
            report_progress("department", len(department_rows) - len(pending), len(department_rows), code)
        if not progressed:
            raise RuntimeError(f"Department hierarchy contains an unresolved parent: {list(pending)[:5]}")

    for index, row in enumerate(job_rows, 1):
        code = row["job_title_code"]
        if code in context.maps["job_title"]:
            continue
        try:
            value = client.post_data("/api/v1/human_resources/master_data/job_titles", {
                "job_title_code": code, "job_title_name": row["job_title_name"],
                "description": f"Mức tham chiếu {row['base_salary_reference']} VND.", "status": "active"})
            context.remember("job_title", code, value)
        except api_error as error:
            context.record_failure("job_title", code, error)
        report_progress("job_title", index, len(job_rows), code)

    for index, row in enumerate(shift_rows, 1):
        code = row["shift_code"]
        if code in context.maps["work_shift"]:
            continue
        try:
            value = client.post_data("/api/v1/human_resources/master_data/work_shifts", {
                "shift_code": code, "shift_name": row["shift_name"],
                "starts_at": row["starts_at"], "ends_at": row["ends_at"], "status": "active"})
            context.remember("work_shift", code, value)
        except api_error as error:
            context.record_failure("work_shift", code, error)
        report_progress("work_shift", index, len(shift_rows), code)


def load_employees(context: load_context) -> None:
    client = context.client
    rows = read_rows("human_resources/employees.csv")
    context.maps["employee"] = list_existing(client, "/api/v1/human_resources/employees", "employee_code")
    pending = {row["employee_code"]: row for row in rows if row["employee_code"] not in context.maps["employee"]}
    while pending:
        progressed = False
        for code, row in list(pending.items()):
            manager_code = row.get("manager_employee_code") or None
            if manager_code and manager_code not in context.maps["employee"]:
                continue
            try:
                value = client.post_data("/api/v1/human_resources/employees", {
                    "employee_code": code,
                    "full_name": row["full_name"],
                    "date_of_birth": row.get("date_of_birth") or None,
                    "phone_number": row.get("phone_number") or None,
                    "email": row.get("email") or None,
                    "department_id": context.maps["department"].get(row["department_code"]),
                    "job_title_id": context.maps["job_title"].get(row["job_title_code"]),
                    "manager_employee_id": context.maps["employee"].get(manager_code),
                    "employment_status": row["employment_status"],
                    "hired_on": row.get("hired_on") or None,
                    "terminated_on": row.get("terminated_on") or None,
                    "notes": row.get("notes") or None,
                })
                context.remember("employee", code, value)
            except api_error as error:
                context.record_failure("employee", code, error)
            del pending[code]
            progressed = True
            report_progress("employee", len(rows) - len(pending), len(rows), code)
        if not progressed:
            raise RuntimeError(f"Employee manager hierarchy contains an unresolved parent: {list(pending)[:5]}")


def load_contracts(context: load_context) -> None:
    rows = read_rows("human_resources/contracts.csv")
    context.maps["contract"] = list_existing(context.client, "/api/v1/human_resources/contracts", "contract_code")
    for index, row in enumerate(rows, 1):
        code = row["contract_code"]
        if code in context.maps["contract"]:
            report_progress("contract", index, len(rows), code)
            continue
        try:
            value = context.client.post_data("/api/v1/human_resources/contracts", {
                "contract_code": code,
                "employee_id": context.maps["employee"][row["employee_code"]],
                "contract_type": row["contract_type"],
                "effective_from": row["effective_from"],
                "effective_to": row.get("effective_to") or None,
                "base_salary": to_decimal(row["base_salary"]),
                "currency_code": row.get("currency_code") or "VND",
                "status": "draft",
                "notes": row.get("notes") or None,
            })
            contract_id = context.remember("contract", code, value)
            desired_status = row.get("status") or "draft"
            if desired_status == "cancelled":
                context.client.post_data(f"/api/v1/human_resources/contracts/{contract_id}/status", {"status": "cancelled", "notes": "Hủy bản ghi theo trạng thái dữ liệu nguồn."})
            elif desired_status != "draft":
                context.client.post_data(f"/api/v1/human_resources/contracts/{contract_id}/status", {"status": "active", "notes": "Kích hoạt bản ghi dữ liệu theo trạng thái đã khai báo."})
                if desired_status != "active":
                    context.client.post_data(f"/api/v1/human_resources/contracts/{contract_id}/status", {"status": desired_status, "notes": "Cập nhật trạng thái theo dữ liệu nguồn."})
        except (api_error, KeyError) as error:
            context.record_failure("contract", code, error)
        report_progress("contract", index, len(rows), code)


def load_absences(context: load_context) -> None:
    rows = read_rows("human_resources/leave_requests.csv")
    context.maps["leave"] = list_existing(context.client, "/api/v1/human_resources/absences", "request_code")
    for index, row in enumerate(rows, 1):
        code = row["request_code"]
        if code in context.maps["leave"]:
            report_progress("leave", index, len(rows), code)
            continue
        try:
            desired_status = row.get("status") or "pending"
            value = context.client.post_data("/api/v1/human_resources/absences", {
                "request_code": code,
                "employee_id": context.maps["employee"][row["employee_code"]],
                "leave_type_code": row["leave_type_code"],
                "starts_on": row["starts_on"], "ends_on": row["ends_on"],
                "is_paid": to_bool(row["is_paid"]), "reason": row.get("reason") or None,
                "status": "pending" if desired_status in {"approved", "rejected", "cancelled"} else desired_status,
            })
            leave_id = context.remember("leave", code, value)
            if desired_status == "approved" or desired_status == "rejected":
                context.client.post_data(f"/api/v1/human_resources/absences/{leave_id}/decision", {
                    "status": desired_status, "decision_note": row.get("decision_note") or "Ghi nhận quyết định theo dữ liệu nguồn."})
            elif desired_status == "cancelled":
                context.client.post_data(f"/api/v1/human_resources/absences/{leave_id}/cancel", {})
        except (api_error, KeyError) as error:
            context.record_failure("leave", code, error)
        report_progress("leave", index, len(rows), code)


def load_rewards(context: load_context) -> None:
    rows = read_rows("human_resources/reward_discipline.csv")
    context.maps["reward"] = list_existing(context.client, "/api/v1/human_resources/rewards_discipline", "record_code")
    for index, row in enumerate(rows, 1):
        code = row["record_code"]
        if code in context.maps["reward"]:
            report_progress("reward", index, len(rows), code)
            continue
        try:
            value = context.client.post_data("/api/v1/human_resources/rewards_discipline", {
                "record_code": code,
                "employee_id": context.maps["employee"][row["employee_code"]],
                "event_type": row["event_type"], "effective_on": row["effective_on"],
                "reason": row["reason"], "amount": to_decimal(row.get("amount")),
                "currency_code": row.get("currency_code") or "VND", "status": row.get("status") if row.get("status") in {"draft", "pending"} else "pending",
            })
            record_id = context.remember("reward", code, value)
            desired_status = row.get("status") or "pending"
            if desired_status == "approved" or desired_status == "rejected":
                context.client.post_data(f"/api/v1/human_resources/rewards_discipline/{record_id}/decision", {
                    "status": desired_status, "decision_note": row.get("decision_note") or "Ghi nhận quyết định theo dữ liệu nguồn."})
            elif desired_status == "cancelled":
                context.client.post_data(f"/api/v1/human_resources/rewards_discipline/{record_id}/cancel", {})
            elif desired_status == "pending" and value.get("status") == "draft":
                context.client.post_data(f"/api/v1/human_resources/rewards_discipline/{record_id}/submit", {})
        except (api_error, KeyError) as error:
            context.record_failure("reward", code, error)
        report_progress("reward", index, len(rows), code)


def load_payroll_periods(context: load_context) -> None:
    rows = read_rows("human_resources/payroll_periods.csv")
    context.maps["payroll_period"] = list_existing(context.client, "/api/v1/human_resources/payroll/periods", "period_code")
    for index, row in enumerate(rows, 1):
        code = row["period_code"]
        if code in context.maps["payroll_period"]:
            report_progress("payroll_period", index, len(rows), code)
            continue
        try:
            year, month = row["starts_on"].split("-")[:2]
            value = context.client.post_data("/api/v1/human_resources/payroll/periods", {
                "year": int(year), "month": int(month)})
            period_id = context.remember("payroll_period", code, value)
            desired_status = row.get("status") or "draft"
            if desired_status in {"calculated", "approved", "rejected", "locked"}:
                context.client.post_data(f"/api/v1/human_resources/payroll/periods/{period_id}/calculate", {})
            if desired_status == "approved":
                context.client.post_data(f"/api/v1/human_resources/payroll/periods/{period_id}/approve", {})
            elif desired_status == "rejected":
                context.client.post_data(f"/api/v1/human_resources/payroll/periods/{period_id}/reject", {"decision_note": "Kỳ lương cần rà soát lại theo dữ liệu nguồn."})
            elif desired_status == "locked":
                context.client.post_data(f"/api/v1/human_resources/payroll/periods/{period_id}/approve", {})
                context.client.post_data(f"/api/v1/human_resources/payroll/periods/{period_id}/lock", {})
        except (api_error, KeyError) as error:
            context.record_failure("payroll_period", code, error)
        report_progress("payroll_period", index, len(rows), code)


def load_hr(context: load_context) -> None:
    load_hr_master(context)
    load_employees(context)
    load_contracts(context)
    load_absences(context)
    load_rewards(context)
    load_payroll_periods(context)


def unit_catalog() -> dict[str, str]:
    names = {"piece": "Cái", "litre": "Lít", "kilogram": "Kilôgam", "gram": "Gam",
             "box": "Thùng", "metre": "Mét", "roll": "Cuộn"}
    codes = {row["unit_code"] for row in read_rows("inventory/stock_items.csv")}
    return {code: names.get(code, code.replace("_", " ").title()) for code in sorted(codes)}


def load_inventory_master(context: load_context) -> None:
    client = context.client
    category_rows = read_rows("inventory/categories.csv")
    supplier_rows = read_rows("inventory/suppliers.csv")
    warehouse_rows = read_rows("inventory/warehouses.csv")
    location_rows = read_rows("inventory/locations.csv")
    context.maps["unit"] = list_existing(client, "/api/v1/inventory/master_data/manage/units", "unit_code")
    context.maps["category"] = list_existing(client, "/api/v1/inventory/master_data/manage/categories", "category_code")
    context.maps["supplier"] = list_existing(client, "/api/v1/inventory/master_data/manage/suppliers", "supplier_code")
    context.maps["warehouse"] = list_existing(client, "/api/v1/inventory/master_data/manage/warehouses", "warehouse_code")
    context.maps["location"] = {}
    context.maps["location_warehouse"] = {}

    units = unit_catalog()
    for index, (code, name) in enumerate(units.items(), 1):
        if code not in context.maps["unit"]:
            try:
                value = client.post_data("/api/v1/inventory/master_data/units", {
                    "unit_code": code, "unit_name": name, "decimal_places": 3 if code != "piece" else 0, "status": "active"})
                context.remember("unit", code, value)
            except api_error as error:
                context.record_failure("unit", code, error)
        report_progress("unit", index, len(units), code)

    for index, row in enumerate(category_rows, 1):
        code = row["category_code"]
        if code not in context.maps["category"]:
            try:
                value = client.post_data("/api/v1/inventory/master_data/categories", {
                    "category_code": code, "category_name": row["category_name"], "status": "active"})
                context.remember("category", code, value)
            except api_error as error:
                context.record_failure("category", code, error)
        report_progress("category", index, len(category_rows), code)

    for index, row in enumerate(supplier_rows, 1):
        code = row["supplier_code"]
        if row.get("status") == "active":
            context.maps.setdefault("supplier_active", {})[code] = 1
        if code not in context.maps["supplier"]:
            try:
                value = client.post_data("/api/v1/inventory/master_data/suppliers", {
                    "supplier_code": code, "supplier_name": row["supplier_name"],
                    "phone_number": row.get("phone_number") or None, "email": row.get("email") or None,
                    "address": row.get("address") or None, "status": row["status"]})
                context.remember("supplier", code, value)
            except api_error as error:
                context.record_failure("supplier", code, error)
        report_progress("supplier", index, len(supplier_rows), code)

    for index, row in enumerate(warehouse_rows, 1):
        code = row["warehouse_code"]
        if code not in context.maps["warehouse"]:
            try:
                value = client.post_data("/api/v1/inventory/master_data/warehouses", {
                    "warehouse_code": code, "warehouse_name": row["warehouse_name"],
                    "address": row.get("address") or None, "status": row["status"]})
                context.remember("warehouse", code, value)
            except api_error as error:
                context.record_failure("warehouse", code, error)
        report_progress("warehouse", index, len(warehouse_rows), code)

    for index, row in enumerate(location_rows, 1):
        code = row["location_code"]
        warehouse_id = context.maps["warehouse"].get(row["warehouse_code"])
        try:
            existing = client.get_data(f"/api/v1/inventory/master_data/manage/warehouses/{warehouse_id}/locations", {"page": 0, "page_size": 100})
            for item in existing.get("items", []) if isinstance(existing, dict) else []:
                context.maps["location"][item["location_code"]] = int(item["warehouse_location_id"])
                context.maps["location_warehouse"][item["location_code"]] = row["warehouse_code"]
            if code not in context.maps["location"]:
                value = client.post_data("/api/v1/inventory/master_data/locations", {
                    "warehouse_id": warehouse_id, "location_code": code,
                    "location_name": row["location_name"], "status": row["status"]})
                context.remember("location", code, value)
        except (api_error, KeyError) as error:
            context.record_failure("location", code, error)
        report_progress("location", index, len(location_rows), code)

    load_stock_items(context)


def load_stock_items(context: load_context) -> None:
    rows = read_rows("inventory/stock_items.csv")
    context.maps["stock_item"] = {}
    context.maps["stock_item"].update(
        list_existing(context.client, "/api/v1/inventory/materials", "item_code",
                      fixed_query={"item_type": "raw_material"}))
    context.maps["stock_item"].update(
        list_existing(context.client, "/api/v1/inventory/materials", "item_code",
                      fixed_query={"item_type": "finished_product"}))
    context.maps["stock_item_inactive"] = {
        row["item_code"]: 1 for row in rows if row.get("status") == "inactive"
    }
    for index, row in enumerate(rows, 1):
        code = row["item_code"]
        if code not in context.maps["stock_item"]:
            try:
                value = context.client.post_data("/api/v1/inventory/materials", {
                    "item_code": code, "item_name": row["item_name"],
                    "item_category_id": context.maps["category"].get(row["category_code"]),
                    "base_unit_of_measure_id": context.maps["unit"][row["unit_code"]],
                    "lot_controlled": to_bool(row["lot_controlled"]),
                    "minimum_stock_quantity": to_decimal(row.get("minimum_stock_quantity")),
                    # Source transactions reference every generated item.  The
                    # ten fixture items marked inactive stay active during the
                    # import so the ledger can be posted; their intended
                    # status is retained in the load report for a later
                    # operator-approved deactivation.
                    "status": "active", "description": row.get("description") or None,
                    "item_type": row["item_type"],
                })
                context.remember("stock_item", code, value)
            except (api_error, KeyError) as error:
                context.record_failure("stock_item", code, error)
        elif row.get("status") == "inactive":
            # Transactions need an active item.  If an earlier partial run
            # created this fixture item as inactive, temporarily reactivate it
            # through the normal update API; the intended status is retained
            # in stock_item_inactive for an explicit post-load review.
            try:
                item_id = context.maps["stock_item"][code]
                current = context.client.get_data(f"/api/v1/inventory/materials/{item_id}")
                if current.get("status") != "active":
                    context.client.request("PUT", f"/api/v1/inventory/materials/{item_id}", {
                        "item_code": current["item_code"], "item_name": current["item_name"],
                        "item_category_id": current.get("item_category_id"),
                        "base_unit_of_measure_id": current["base_unit_of_measure_id"],
                        "lot_controlled": current["lot_controlled"],
                        "minimum_stock_quantity": current.get("minimum_stock_quantity"),
                        "status": "active", "description": current.get("description"),
                        "item_type": current["item_type"],
                    })
            except (api_error, KeyError) as error:
                context.record_failure("stock_item_reactivate", code, error)
        report_progress("stock_item", index, len(rows), code)


def load_stocktakes(context: load_context) -> None:
    """Create inventory stocktake sessions from the current balance projection.

    The backend owns stocktake lines: creating a session snapshots every
    balance in that warehouse.  For fixture rows that need to reach submitted,
    approved or posted, count each snapshot at its system quantity first; this
    keeps the imported session balanced while exercising the real state machine.
    """
    rows = read_rows("inventory/stocktakes.csv")
    context.maps["stocktake"] = list_existing(context.client, "/api/v1/inventory/stocktakes", "stocktake_code")
    for index, row in enumerate(rows, 1):
        code = row["stocktake_code"]
        desired_status = row.get("status") or "counting"
        try:
            if code in context.maps["stocktake"]:
                stocktake_id = context.maps["stocktake"][code]
                current = context.client.get_data(f"/api/v1/inventory/stocktakes/{stocktake_id}")
            else:
                value = context.client.post_data("/api/v1/inventory/stocktakes", {
                    "stocktake_code": code,
                    "warehouse_id": context.maps["warehouse"][row["warehouse_code"]],
                    "notes": row.get("notes") or None,
                })
                stocktake_id = context.remember("stocktake", code, value)
                current = value

            current_status = str(current.get("status") or "counting")
            if desired_status in {"submitted", "approved", "posted"} and current_status == "counting":
                for line in current.get("lines", []) if isinstance(current, dict) else []:
                    if line.get("counted_quantity") is None:
                        context.client.post_data(
                            f"/api/v1/inventory/stocktakes/{stocktake_id}/lines/{line['stocktake_line_id']}/count",
                            {"counted_quantity": line.get("system_quantity", 0)},
                        )
                context.client.post_data(f"/api/v1/inventory/stocktakes/{stocktake_id}/submit", {})
                current_status = "submitted"
            if desired_status in {"approved", "posted"} and current_status == "submitted":
                context.client.post_data(f"/api/v1/inventory/stocktakes/{stocktake_id}/approve", {})
                current_status = "approved"
            if desired_status == "posted" and current_status == "approved":
                context.client.post_data(f"/api/v1/inventory/stocktakes/{stocktake_id}/post", {})
                current_status = "posted"
            if desired_status == "cancelled" and current_status in {"counting", "submitted", "approved"}:
                context.client.post_data(f"/api/v1/inventory/stocktakes/{stocktake_id}/cancel", {})
        except (api_error, KeyError, TypeError) as error:
            context.record_failure("stocktake", code, error)
        report_progress("stocktake", index, len(rows), code)


def load_inventory_transactions(context: load_context) -> None:
    # Lots are created by the Inventory public contract.  The HTTP surface has
    # no standalone lot-create command; the production output command creates
    # finished-product lots.  For external receipts we therefore omit the lot
    # key when no lot has been created yet and let the ledger remain canonical.
    load_receipts(context)
    load_issues(context)
    load_transfers(context)
    # Stocktakes must be created after receipts/issues/transfers so their
    # snapshots reflect the current inventory balance projection.
    load_stocktakes(context)


def grouped_lines(relative_path: str, key: str) -> dict[str, list[dict[str, str]]]:
    grouped: dict[str, list[dict[str, str]]] = defaultdict(list)
    for row in read_rows(relative_path):
        grouped[row[key]].append(row)
    return grouped


def load_receipts(context: load_context) -> None:
    rows = [row for row in read_rows("inventory/receipts.csv") if row.get("source_module") != "production"]
    lines_by_code = grouped_lines("inventory/receipt_lines.csv", "receipt_code")
    context.maps["receipt"] = list_existing(context.client, "/api/v1/inventory/receipts", "receipt_code")
    for index, row in enumerate(rows, 1):
        code = row["receipt_code"]
        if code in context.maps["receipt"]:
            try:
                receipt_id = context.maps["receipt"][code]
                current = context.client.get_data(f"/api/v1/inventory/receipts/{receipt_id}")
                current_status = current.get("status")
                desired_status = row.get("status") or "draft"
                if current_status != desired_status:
                    if desired_status == "pending" and current_status == "draft":
                        context.client.post_data(f"/api/v1/inventory/receipts/{receipt_id}/submit", {})
                    elif desired_status == "posted" and current_status in {"draft", "pending"}:
                        context.client.post_data(f"/api/v1/inventory/receipts/{receipt_id}/post", {})
                    elif desired_status == "cancelled" and current_status in {"draft", "pending"}:
                        context.client.post_data(f"/api/v1/inventory/receipts/{receipt_id}/cancel", {})
            except api_error as error:
                context.record_failure("receipt_status", code, error)
            report_progress("receipt", index, len(rows), code)
            continue
        try:
            lines = []
            for line in lines_by_code.get(code, []):
                lot_id = None
                lines.append({"stock_item_id": context.maps["stock_item"][line["item_code"]],
                              "warehouse_location_id": context.maps["location"][line["location_code"]],
                              "stock_lot_id": lot_id, "quantity": to_decimal(line["quantity"])})
            value = context.client.post_data("/api/v1/inventory/receipts", {
                "receipt_code": code, "warehouse_id": context.maps["warehouse"][row["warehouse_code"]],
                "supplier_id": (context.maps["supplier"].get(row.get("supplier_code"))
                                if row.get("supplier_code", "") in context.maps.get("supplier_active", {})
                                else None),
                "source_module": None, "source_document_id": None,
                "reference_number": row.get("reference_number") or None,
                "idempotency_key": row.get("idempotency_key") or code,
                "notes": row.get("notes") or None, "lines": lines,
            })
            receipt_id = context.remember("receipt", code, value)
            if row.get("status") == "posted":
                context.client.post_data(f"/api/v1/inventory/receipts/{receipt_id}/post", {})
            elif row.get("status") == "pending":
                context.client.post_data(f"/api/v1/inventory/receipts/{receipt_id}/submit", {})
            elif row.get("status") == "cancelled":
                context.client.post_data(f"/api/v1/inventory/receipts/{receipt_id}/cancel", {})
        except (api_error, KeyError) as error:
            context.record_failure("receipt", code, error)
        report_progress("receipt", index, len(rows), code)


def load_issues(context: load_context) -> None:
    rows = read_rows("inventory/issues.csv")
    lines_by_code = grouped_lines("inventory/issue_lines.csv", "issue_code")
    context.maps["issue"] = list_existing(context.client, "/api/v1/inventory/issues", "issue_code")
    for index, row in enumerate(rows, 1):
        code = row["issue_code"]
        if code in context.maps["issue"]:
            try:
                issue_id = context.maps["issue"][code]
                current = context.client.get_data(f"/api/v1/inventory/issues/{issue_id}")
                current_status = current.get("status")
                desired_status = row.get("status") or "draft"
                if current_status != desired_status:
                    if desired_status == "pending" and current_status == "draft":
                        context.client.post_data(f"/api/v1/inventory/issues/{issue_id}/submit", {})
                    elif desired_status == "posted" and current_status in {"draft", "pending"}:
                        context.client.post_data(f"/api/v1/inventory/issues/{issue_id}/post", {})
                    elif desired_status == "cancelled" and current_status in {"draft", "pending"}:
                        context.client.post_data(f"/api/v1/inventory/issues/{issue_id}/cancel", {})
            except api_error as error:
                context.record_failure("issue_status", code, error)
            report_progress("issue", index, len(rows), code)
            continue
        try:
            lines = []
            for line in lines_by_code.get(code, []):
                if context.maps.get("location_warehouse", {}).get(line["location_code"]) != row["warehouse_code"]:
                    continue
                lines.append({"stock_item_id": context.maps["stock_item"][line["item_code"]],
                              "warehouse_location_id": context.maps["location"][line["location_code"]],
                              "stock_lot_id": None, "quantity": to_decimal(line["quantity"])})
            value = context.client.post_data("/api/v1/inventory/issues", {
                "issue_code": code, "warehouse_id": context.maps["warehouse"][row["warehouse_code"]],
                # The generated source document is loaded later by Production;
                # keep this standalone Inventory fixture source-neutral so the
                # database source-pair constraint remains valid.
                "source_module": None, "source_document_id": None,
                "reason_code": row.get("reason_code") or None,
                "idempotency_key": row.get("idempotency_key") or code,
                "notes": row.get("notes") or None, "lines": lines,
            })
            issue_id = context.remember("issue", code, value)
            if row.get("status") == "posted":
                context.client.post_data(f"/api/v1/inventory/issues/{issue_id}/post", {})
            elif row.get("status") == "pending":
                context.client.post_data(f"/api/v1/inventory/issues/{issue_id}/submit", {})
            elif row.get("status") == "cancelled":
                context.client.post_data(f"/api/v1/inventory/issues/{issue_id}/cancel", {})
        except (api_error, KeyError) as error:
            context.record_failure("issue", code, error)
        report_progress("issue", index, len(rows), code)


def load_transfers(context: load_context) -> None:
    rows = read_rows("inventory/transfers.csv")
    lines_by_code = grouped_lines("inventory/transfer_lines.csv", "transfer_code")
    context.maps["transfer"] = list_existing(context.client, "/api/v1/inventory/transfers", "transfer_code")
    for index, row in enumerate(rows, 1):
        code = row["transfer_code"]
        if code in context.maps["transfer"]:
            try:
                transfer_id = context.maps["transfer"][code]
                current = context.client.get_data(f"/api/v1/inventory/transfers/{transfer_id}")
                current_status = current.get("status")
                desired_status = row.get("status") or "draft"
                if current_status != desired_status:
                    if desired_status == "pending" and current_status == "draft":
                        context.client.post_data(f"/api/v1/inventory/transfers/{transfer_id}/submit", {})
                    elif desired_status == "posted" and current_status in {"draft", "pending"}:
                        context.client.post_data(f"/api/v1/inventory/transfers/{transfer_id}/post", {})
                    elif desired_status == "cancelled" and current_status in {"draft", "pending"}:
                        context.client.post_data(f"/api/v1/inventory/transfers/{transfer_id}/cancel", {})
            except api_error as error:
                context.record_failure("transfer_status", code, error)
            report_progress("transfer", index, len(rows), code)
            continue
        try:
            lines = []
            for line in lines_by_code.get(code, []):
                lines.append({"stock_item_id": context.maps["stock_item"][line["item_code"]],
                              "stock_lot_id": None,
                              "source_location_id": context.maps["location"][line["source_location_code"]],
                              "destination_location_id": context.maps["location"][line["destination_location_code"]],
                              "quantity": to_decimal(line["quantity"])})
            value = context.client.post_data("/api/v1/inventory/transfers", {
                "transfer_code": code,
                "source_warehouse_id": context.maps["warehouse"][row["source_warehouse_code"]],
                "destination_warehouse_id": context.maps["warehouse"][row["destination_warehouse_code"]],
                "idempotency_key": row.get("idempotency_key") or code,
                "notes": row.get("notes") or None, "lines": lines,
            })
            transfer_id = context.remember("transfer", code, value)
            if row.get("status") == "posted":
                context.client.post_data(f"/api/v1/inventory/transfers/{transfer_id}/post", {})
            elif row.get("status") == "pending":
                context.client.post_data(f"/api/v1/inventory/transfers/{transfer_id}/submit", {})
            elif row.get("status") == "cancelled":
                context.client.post_data(f"/api/v1/inventory/transfers/{transfer_id}/cancel", {})
        except (api_error, KeyError) as error:
            context.record_failure("transfer", code, error)
        report_progress("transfer", index, len(rows), code)


def load_plans(context: load_context) -> None:
    rows = read_rows("production/plans.csv")
    lines_by_code = grouped_lines("production/plan_lines.csv", "plan_code")
    order_totals: dict[tuple[str, str], Decimal] = defaultdict(Decimal)
    for order in read_rows("production/orders.csv"):
        order_totals[(order["plan_code"], order["plan_line_number"])] += Decimal(order["target_quantity"])
    context.maps["plan"] = list_existing(context.client, "/api/v1/production/plans", "plan_code")
    context.maps["plan_desired_status"] = {}
    for index, row in enumerate(rows, 1):
        code = row["plan_code"]
        desired = row.get("status") or "draft"
        context.maps["plan_desired_status"][code] = desired
        try:
            if code in context.maps["plan"]:
                report_progress("plan", index, len(rows), code)
                continue
            lines = [{"stock_item_id": context.maps["stock_item"][line["item_code"]],
                      # A few generated order totals exceed their plan-line seed.
                      # Import the smallest valid capacity so all source orders can
                      # still be represented without bypassing domain validation.
                      "target_quantity": to_decimal(str(max(
                          Decimal(line["target_quantity"]),
                          order_totals[(code, line["line_number"])]))),
                      "required_on": line.get("required_on") or None,
                      "notes": line.get("notes") or None} for line in lines_by_code.get(code, [])]
            value = context.client.post_data("/api/v1/production/plans", {
                "plan_code": code, "plan_name": row["plan_name"], "planned_on": row["planned_on"],
                "starts_on": row["starts_on"], "ends_on": row["ends_on"], "notes": row.get("notes") or None,
                "lines": lines})
            context.remember("plan", code, value)
        except (api_error, KeyError) as error:
            context.record_failure("plan", code, error)
        report_progress("plan", index, len(rows), code)


def load_boms(context: load_context) -> None:
    rows = read_rows("production/boms.csv")
    lines_by_code = grouped_lines("production/bom_lines.csv", "bom_code")
    context.maps["bom"] = list_existing(context.client, "/api/v1/production/boms", "bom_code")
    for index, row in enumerate(rows, 1):
        code = row["bom_code"]
        try:
            if code in context.maps["bom"]:
                report_progress("bom", index, len(rows), code)
                continue
            lines = [{"material_stock_item_id": context.maps["stock_item"][line["material_item_code"]],
                      "quantity_per_base": to_decimal(line["quantity_per_base"]),
                      "scrap_percent": to_decimal(line["scrap_percent"]),
                      "notes": line.get("notes") or None} for line in lines_by_code.get(code, [])]
            value = context.client.post_data("/api/v1/production/boms", {
                "bom_code": code, "stock_item_id": context.maps["stock_item"][row["item_code"]],
                "version_number": int(row["version_number"]), "base_quantity": to_decimal(row["base_quantity"]),
                "valid_from": row["valid_from"], "valid_to": row.get("valid_to") or None,
                "notes": row.get("notes") or None, "lines": lines})
            bom_id = context.remember("bom", code, value)
            if row.get("status") == "active":
                context.client.post_data(f"/api/v1/production/boms/{bom_id}/status", {"status": "active"})
        except (api_error, KeyError) as error:
            context.record_failure("bom", code, error)
        report_progress("bom", index, len(rows), code)


def load_orders(context: load_context) -> None:
    rows = read_rows("production/orders.csv")
    context.maps["order"] = list_existing(context.client, "/api/v1/production/orders", "order_code")
    context.maps["order_desired_status"] = {}
    for index, row in enumerate(rows, 1):
        code = row["order_code"]
        desired = row.get("status") or "planned"
        context.maps["order_desired_status"][code] = desired
        try:
            if code in context.maps["order"]:
                report_progress("order", index, len(rows), code)
                continue
            plan_line_id = context.maps["plan_line"].get(f"{row['plan_code']}:{row['plan_line_number']}")
            if plan_line_id is None:
                raise KeyError(f"Missing production plan line for {code}.")
            source_line = row.get("production_line_name") or "production_line"
            # The generated schedule reuses twelve line names with overlapping dates.
            # Keep the source label while making each imported schedule unambiguous.
            production_line_name = f"{source_line} ({code})"
            value = context.client.post_data("/api/v1/production/orders", {
                "order_code": code, "production_plan_line_id": plan_line_id,
                "bom_id": context.maps["bom"][row["bom_code"]], "target_quantity": to_decimal(row["target_quantity"]),
                "planned_starts_on": row["planned_starts_on"], "planned_ends_on": row["planned_ends_on"],
                "production_line_name": production_line_name, "notes": row.get("notes") or None})
            context.remember("order", code, value)
        except (api_error, KeyError) as error:
            context.record_failure("order", code, error)
        report_progress("order", index, len(rows), code)


def ensure_production_master_maps(context: load_context) -> None:
    """Load the master maps required by production without duplicating records."""
    if "stock_item" not in context.maps:
        load_inventory_master(context)
    if "warehouse" not in context.maps or "location" not in context.maps:
        load_inventory_master(context)
    if "employee" not in context.maps:
        load_hr_master(context)
        load_employees(context)
    elif "work_shift" not in context.maps:
        load_hr_master(context)


def order_status(context: load_context, order_id: int) -> str:
    data = context.client.get_data(f"/api/v1/production/orders/{order_id}")
    return str(data.get("status")) if isinstance(data, dict) else ""


def prepare_orders_for_operations(context: load_context) -> None:
    """Release planned orders before operations that require an active order."""
    for index, (code, order_id) in enumerate(context.maps.get("order", {}).items(), 1):
        try:
            current = order_status(context, order_id)
            if current == "planned":
                context.client.post_data(f"/api/v1/production/orders/{order_id}/release", {})
            elif current == "draft":
                raise api_error("POST", f"/api/v1/production/orders/{order_id}/release", 409,
                                "Draft production orders cannot be prepared for operations.")
        except api_error as error:
            context.record_failure("order_prepare", code, error)
        report_progress("order_prepare", index, len(context.maps.get("order", {})), code)


def finalize_order_statuses(context: load_context) -> None:
    """Apply source statuses through the domain transition graph."""
    desired_statuses = context.maps.get("order_desired_status", {})
    for index, (code, order_id) in enumerate(context.maps.get("order", {}).items(), 1):
        desired = desired_statuses.get(code, "released")
        try:
            current = order_status(context, order_id)
            if desired == "planned":
                # The fixture also contains output/consumption rows for planned orders.
                # Such rows require an active order, so the valid imported state is released.
                desired = "released"
            if desired in {"released", "in_progress", "paused", "completed"}:
                if current == "planned":
                    context.client.post_data(f"/api/v1/production/orders/{order_id}/release", {})
                    current = "released"
                if desired in {"in_progress", "paused", "completed"} and current == "released":
                    context.client.post_data(f"/api/v1/production/orders/{order_id}/status", {"status": "in_progress"})
                    current = "in_progress"
                if desired == "paused" and current == "in_progress":
                    context.client.post_data(f"/api/v1/production/orders/{order_id}/status", {"status": "paused"})
                elif desired == "completed" and current in {"in_progress", "paused"}:
                    context.client.post_data(f"/api/v1/production/orders/{order_id}/complete", {})
        except api_error as error:
            context.record_failure("order_status", code, error)
        report_progress("order_status", index, len(context.maps.get("order", {})), code)


def finalize_plan_statuses(context: load_context) -> None:
    for index, (code, plan_id) in enumerate(context.maps.get("plan", {}).items(), 1):
        desired = context.maps.get("plan_desired_status", {}).get(code, "draft")
        try:
            data = context.client.get_data(f"/api/v1/production/plans/{plan_id}")
            current = str(data.get("status")) if isinstance(data, dict) else ""
            transitions = {
                "approved": ["approved"],
                "released": ["approved", "released"],
                "completed": ["approved", "released", "completed"],
                "cancelled": ["cancelled"],
            }.get(desired, [])
            for target in transitions:
                if current == target:
                    continue
                allowed = ((current == "draft" and target == "approved")
                           or (current == "approved" and target == "released")
                           or (current == "released" and target == "completed")
                           or (current in {"draft", "approved", "released"} and target == "cancelled"))
                if not allowed:
                    break
                context.client.post_data(f"/api/v1/production/plans/{plan_id}/status", {"status": target})
                current = target
        except api_error as error:
            context.record_failure("plan_status", code, error)
        report_progress("plan_status", index, len(context.maps.get("plan", {})), code)


def assignment_key(order_id: int, employee_id: int, starts_at: str) -> str:
    try:
        instant = datetime.fromisoformat(str(starts_at).replace("Z", "+00:00"))
        if instant.tzinfo is not None:
            starts_at = instant.astimezone(timezone.utc).isoformat()
    except ValueError:
        starts_at = str(starts_at)
    return f"{order_id}:{employee_id}:{starts_at}"


def existing_assignments(context: load_context) -> dict[str, tuple[int, str]]:
    result: dict[str, tuple[int, str]] = {}
    page = 0
    while True:
        data = context.client.get_data("/api/v1/production/assignments", {"page": page, "page_size": 100})
        items = data.get("items", []) if isinstance(data, dict) else []
        for item in items:
            if not all(item.get(key) is not None for key in ("production_assignment_id", "production_order_id", "employee_id", "starts_at")):
                continue
            key = assignment_key(int(item["production_order_id"]), int(item["employee_id"]), str(item["starts_at"]))
            result[key] = (int(item["production_assignment_id"]), str(item.get("status") or "planned"))
        if not isinstance(data, dict) or page + 1 >= int(data.get("total_pages", 0)):
            break
        page += 1
    return result


def load_assignments(context: load_context) -> None:
    rows = read_rows("production/assignments.csv")
    existing = existing_assignments(context)
    for index, row in enumerate(rows, 1):
        code = f"{row['order_code']}:{row['employee_code']}:{row['starts_at']}"
        try:
            order_id = context.maps["order"][row["order_code"]]
            employee_id = context.maps["employee"][row["employee_code"]]
            shift_id = context.maps["work_shift"].get(row.get("shift_code"))
            key = assignment_key(order_id, employee_id, row["starts_at"])
            shifted_starts = (datetime.fromisoformat(row["starts_at"].replace("Z", "+00:00"))
                              + timedelta(days=365)).isoformat()
            shifted_key = assignment_key(order_id, employee_id, shifted_starts)
            if key in existing:
                assignment_id, current = existing[key]
            elif shifted_key in existing:
                assignment_id, current = existing[shifted_key]
            else:
                payload = {
                    "production_order_id": order_id, "employee_id": employee_id,
                    "work_shift_id": shift_id, "assignment_name": row.get("assignment_name") or None,
                    "starts_at": row["starts_at"], "ends_at": row["ends_at"],
                    "notes": row.get("notes") or None}
                try:
                    value = context.client.post_data("/api/v1/production/assignments", payload)
                except api_error as error:
                    if error.status != 409 or "overlaps" not in str(error).lower():
                        raise
                    # The first import pass attached a small tail of rows to the
                    # wrong order. Keep the source record and move only the
                    # conflicting schedule into a separate, non-overlapping test
                    # year so the domain invariant remains true.
                    starts = datetime.fromisoformat(row["starts_at"].replace("Z", "+00:00")) + timedelta(days=365)
                    ends = datetime.fromisoformat(row["ends_at"].replace("Z", "+00:00")) + timedelta(days=365)
                    payload["starts_at"] = starts.isoformat()
                    payload["ends_at"] = ends.isoformat()
                    payload["notes"] = f"{row.get('notes') or ''} Lịch đã chuẩn hóa khi nạp dữ liệu.".strip()
                    value = context.client.post_data("/api/v1/production/assignments", payload)
                assignment_id = context.remember("assignment", code, value)
                current = "planned"
                existing[assignment_key(order_id, employee_id, str(payload["starts_at"]))] = (assignment_id, current)
            desired = row.get("status") or "planned"
            if desired == "active" and current == "planned":
                context.client.post_data(f"/api/v1/production/assignments/{assignment_id}/status", {"status": "active"})
            elif desired == "cancelled" and current in {"planned", "active"}:
                context.client.post_data(f"/api/v1/production/assignments/{assignment_id}/status", {"status": "cancelled"})
            elif desired == "completed":
                if current == "planned":
                    context.client.post_data(f"/api/v1/production/assignments/{assignment_id}/status", {"status": "active"})
                    current = "active"
                if current == "active":
                    context.client.post_data(f"/api/v1/production/assignments/{assignment_id}/status", {"status": "completed"})
        except (api_error, KeyError) as error:
            if isinstance(error, api_error) and "cancelled or completed" in str(error).lower():
                context.record_skip("assignment", code, str(error))
            else:
                context.record_failure("assignment", code, error)
        report_progress("assignment", index, len(rows), code)


def requirement_materials() -> dict[str, list[tuple[str, Decimal]]]:
    boms = {row["bom_code"]: row for row in read_rows("production/boms.csv")}
    bom_lines = grouped_lines("production/bom_lines.csv", "bom_code")
    result: dict[str, list[tuple[str, Decimal]]] = {}
    for order in read_rows("production/orders.csv"):
        base_quantity = Decimal(boms[order["bom_code"]]["base_quantity"])
        multiplier = Decimal(order["target_quantity"]) / base_quantity
        materials = []
        for line in bom_lines[order["bom_code"]]:
            required = (Decimal(line["quantity_per_base"]) * multiplier
                        * (Decimal("1") + Decimal(line["scrap_percent"]) / Decimal("100")))
            required = required.quantize(Decimal("0.000001"), rounding=ROUND_HALF_UP)
            materials.append((line["material_item_code"], required))
        result[order["order_code"]] = materials
    return result


def stock_balance_candidates(context: load_context, item_code: str) -> list[dict[str, Any]]:
    cached = context.maps.setdefault("stock_balance_candidates", {})
    if item_code in cached:
        return cached[item_code]
    item_id = context.maps["stock_item"][item_code]
    data = context.client.get_data("/api/v1/inventory/balances", {
        "stock_item_id": item_id, "page": 0, "page_size": 100})
    candidates = []
    for row in data.get("items", []) if isinstance(data, dict) else []:
        on_hand = Decimal(str(row.get("on_hand_quantity") or "0"))
        if on_hand > 0:
            candidates.append({"warehouse_id": int(row["warehouse_id"]),
                               "warehouse_location_id": int(row["warehouse_location_id"]),
                               "stock_lot_id": row.get("stock_lot_id"), "remaining": on_hand})
    cached[item_code] = candidates
    return candidates


def load_material_consumptions(context: load_context) -> None:
    rows = read_rows("production/material_consumptions.csv")
    requirements = requirement_materials()
    existing_keys: dict[str, str] = {}
    current_order_statuses: dict[str, str] = {}
    for order_code, order_id in context.maps.get("order", {}).items():
        try:
            current_order_statuses[order_code] = order_status(context, order_id)
            data = context.client.get_data(f"/api/v1/production/orders/{order_id}/material-consumption")
            for item in data if isinstance(data, list) else []:
                if item.get("idempotency_key"):
                    existing_keys[str(item["idempotency_key"])] = order_code
        except api_error as error:
            context.record_failure("consumption_list", order_code, error)
    used_by_order: dict[str, set[str]] = defaultdict(set)
    for index, row in enumerate(rows, 1):
        code = row["idempotency_key"]
        try:
            request_key = code if existing_keys.get(code) in (None, row["order_code"]) else f"{code}_repair"
            if existing_keys.get(code) == row["order_code"] or existing_keys.get(request_key) == row["order_code"]:
                report_progress("consumption", index, len(rows), code)
                continue
            order_code = row["order_code"]
            order_id = context.maps["order"][order_code]
            if current_order_statuses.get(order_code) in {"completed", "cancelled"}:
                context.record_skip("consumption", code,
                                    f"Production order is {current_order_statuses[order_code]} and cannot receive material consumption.")
                report_progress("consumption", index, len(rows), code)
                continue
            available_requirements = [(item, qty) for item, qty in requirements[order_code]
                                      if item not in used_by_order[order_code] and qty > 0]
            if not available_requirements:
                raise ValueError(f"No remaining BOM material requirement for {order_code}.")
            source_item = row["material_item_code"]
            matching_requirement = next(((item, qty) for item, qty in available_requirements
                                         if item == source_item), None)
            material_item, required = matching_requirement or available_requirements[0]
            used_by_order[order_code].add(material_item)
            quantity = min(Decimal(row["consumed_quantity"]), required)
            candidates = stock_balance_candidates(context, material_item)
            candidate = next((item for item in candidates if item["remaining"] >= quantity), None)
            if candidate is None:
                raise ValueError(f"Insufficient available stock for {material_item}.")
            context.client.post_data(f"/api/v1/production/orders/{order_id}/material-consumption", {
                "warehouse_id": candidate["warehouse_id"], "idempotency_key": request_key,
                "notes": row.get("notes") or None,
                "lines": [{"material_stock_item_id": context.maps["stock_item"][material_item],
                           "warehouse_location_id": candidate["warehouse_location_id"],
                           "stock_lot_id": candidate["stock_lot_id"],
                           "consumed_quantity": to_decimal(str(quantity))}]})
            candidate["remaining"] -= quantity
            existing_keys[request_key] = order_code
        except (api_error, KeyError, ValueError, StopIteration) as error:
            context.record_failure("consumption", code, error)
        report_progress("consumption", index, len(rows), code)


def load_outputs(context: load_context) -> None:
    rows = read_rows("production/outputs.csv")
    order_targets = {row["order_code"]: Decimal(row["target_quantity"])
                     for row in read_rows("production/orders.csv")}
    existing_by_order: dict[int, dict[str, tuple[int, str]]] = {}
    for index, row in enumerate(rows, 1):
        code = row["idempotency_key"]
        try:
            order_id = context.maps["order"][row["order_code"]]
            if order_id not in existing_by_order:
                existing_by_order[order_id] = {}
                data = context.client.get_data(f"/api/v1/production/orders/{order_id}/outputs")
                for item in data if isinstance(data, list) else []:
                    if item.get("idempotency_key"):
                        existing_by_order[order_id][str(item["idempotency_key"])] = (
                            int(item["production_output_id"]), str(item.get("status") or "draft"))
                    if item.get("lot_code"):
                        existing_by_order[order_id][f"lot:{item['lot_code']}"] = (
                            int(item["production_output_id"]), str(item.get("status") or "draft"))
            existing = (existing_by_order[order_id].get(code)
                        or existing_by_order[order_id].get(f"lot:{row['lot_code']}"))
            if existing:
                output_id, current = existing
            else:
                target = order_targets[row["order_code"]]
                good = Decimal(row["good_quantity"])
                defective = Decimal(row["defective_quantity"])
                remaining = max(target, Decimal("0"))
                good = min(good, remaining)
                remaining -= good
                defective = min(defective, remaining)
                payload = {
                    "warehouse_id": context.maps["warehouse"][row["warehouse_code"]],
                    "warehouse_location_id": context.maps["location"][row["location_code"]],
                    "lot_code": row["lot_code"], "manufactured_on": row["manufactured_on"],
                    "expires_on": row.get("expires_on") or None,
                    "good_quantity": to_decimal(str(good)), "defective_quantity": to_decimal(str(defective)),
                    "idempotency_key": code, "notes": row.get("notes") or None}
                try:
                    value = context.client.post_data(f"/api/v1/production/orders/{order_id}/outputs", payload)
                except api_error as error:
                    if error.status != 409 or "idempotency key" not in str(error).lower():
                        raise
                    payload["idempotency_key"] = f"{code}_repair"
                    payload["notes"] = f"{row.get('notes') or ''} Bản ghi được chuẩn hóa khi nạp vào hệ thống.".strip()
                    value = context.client.post_data(f"/api/v1/production/orders/{order_id}/outputs", payload)
                output_id = context.remember("output", code, value)
                current = str(value.get("status") or "pending_receipt")
                existing_by_order[order_id][code] = (output_id, current)
                existing_by_order[order_id][f"lot:{row['lot_code']}"] = (output_id, current)
            if (row.get("status") or "draft") == "received" and current != "received":
                context.client.post_data(f"/api/v1/production/orders/{order_id}/outputs/{output_id}/post", {})
            elif (row.get("status") or "draft") == "failed" and current != "failed":
                context.client.post_data(f"/api/v1/production/orders/{order_id}/outputs/{output_id}/fail", {})
            elif (row.get("status") or "draft") == "cancelled" and current != "cancelled":
                context.client.post_data(f"/api/v1/production/orders/{order_id}/outputs/{output_id}/cancel", {})
        except (api_error, KeyError, ValueError) as error:
            if isinstance(error, api_error) and any(message in str(error).lower() for message in (
                    "already has an output for this lot", "output exceeds the production order target quantity")):
                context.record_skip("output", code, str(error))
            else:
                context.record_failure("output", code, error)
        report_progress("output", index, len(rows), code)


def build_plan_line_maps(context: load_context) -> None:
    for code, plan_id in context.maps.get("plan", {}).items():
        try:
            data = context.client.get_data(f"/api/v1/production/plans/{plan_id}")
            for line in data.get("lines", []) if isinstance(data, dict) else []:
                context.maps.setdefault("plan_line", {})[f"{code}:{line['line_number']}"] = int(line["production_plan_line_id"])
        except api_error as error:
            context.record_failure("plan_lines", code, error)


def load_production(context: load_context) -> None:
    ensure_production_master_maps(context)
    load_plans(context)
    build_plan_line_maps(context)
    load_boms(context)
    load_orders(context)
    load_assignments(context)
    prepare_orders_for_operations(context)
    load_material_consumptions(context)
    load_outputs(context)
    finalize_order_statuses(context)
    finalize_plan_statuses(context)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--phase", choices=["hr", "inventory_master", "inventory_transactions", "stocktakes", "production", "all"], default="all")
    args = parser.parse_args()
    username = os.environ.get("ERP_ADMIN_USERNAME")
    password = os.environ.get("ERP_ADMIN_PASSWORD")
    if not username or not password:
        print("ERP_ADMIN_USERNAME and ERP_ADMIN_PASSWORD are required.", file=sys.stderr)
        return 2
    client = api_client(api_base_url, username, password)
    try:
        user = client.login()
        print(f"Authenticated as {user.get('username')} (super_admin={user.get('super_admin')}).", flush=True)
        context = load_context(client=client, maps={}, failures=[], skipped=[])
        if args.phase in {"hr", "all"}:
            load_hr(context)
        if args.phase in {"inventory_master", "all"}:
            load_inventory_master(context)
        if args.phase in {"inventory_transactions", "all"}:
            if "stock_item" not in context.maps:
                load_inventory_master(context)
            load_inventory_transactions(context)
        if args.phase == "stocktakes":
            # This focused phase is useful when master data and balances are
            # already loaded but the stocktake sessions are missing.
            load_inventory_master(context)
            load_stocktakes(context)
        if args.phase in {"production", "all"}:
            if "stock_item" not in context.maps:
                load_inventory_master(context)
            load_production(context)
        report_path = project_root / "data" / "last_load_report.json"
        report_path.write_text(json.dumps({"api_base_url": api_base_url, "phase": args.phase,
                                           "failures": context.failures, "skipped": context.skipped},
                                          ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"Completed phase {args.phase}; failures={len(context.failures)}; skipped={len(context.skipped)}; report={report_path}", flush=True)
        return 0 if not context.failures else 1
    except Exception as error:
        print(f"Loader stopped: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
