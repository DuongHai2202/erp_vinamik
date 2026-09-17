#!/usr/bin/env python3
"""Validate generated ERP fixture counts and business-key references."""

from __future__ import annotations

import csv
import json
import re
import sys
from datetime import date, datetime
from pathlib import Path


data_root = Path(__file__).resolve().parent
errors: list[str] = []


def read_csv(relative_path: str) -> list[dict[str, str]]:
    target_path = data_root / relative_path
    if not target_path.exists():
        errors.append(f"missing file: {relative_path}")
        return []
    with target_path.open("r", encoding="utf-8-sig", newline="") as input_file:
        return list(csv.DictReader(input_file))


def codes(rows: list[dict[str, str]], field_name: str, label: str) -> set[str]:
    values = [row.get(field_name, "") for row in rows]
    if any(not value for value in values):
        errors.append(f"empty {field_name} in {label}")
    duplicates = len(values) - len(set(values))
    if duplicates:
        errors.append(f"{duplicates} duplicate {field_name} values in {label}")
    return set(values)


def require_reference(rows: list[dict[str, str]], field_name: str, allowed: set[str], label: str, allow_empty: bool = False) -> None:
    for row_number, row in enumerate(rows, start=2):
        value = row.get(field_name, "")
        if allow_empty and not value:
            continue
        if value not in allowed:
            errors.append(f"{label} row {row_number} has unknown {field_name}={value}")


def validate_employee_contact_and_dates(rows: list[dict[str, str]]) -> None:
    phone_pattern = re.compile(r"0\d{9,10}")
    for row_number, row in enumerate(rows, start=2):
        phone_number = row.get("phone_number", "")
        phone_number_display = row.get("phone_number_display", "")
        if not phone_pattern.fullmatch(phone_number):
            errors.append(f"employees row {row_number} has invalid Vietnamese phone_number={phone_number}")
        expected_display = f"{phone_number[:4]} {phone_number[4:7]} {phone_number[7:]}" if phone_number else ""
        if phone_number_display != expected_display:
            errors.append(f"employees row {row_number} has inconsistent phone_number_display")

        date_values: dict[str, date | None] = {}
        for field_name in ("date_of_birth", "hired_on", "terminated_on"):
            raw_value = row.get(field_name, "")
            if not raw_value:
                date_values[field_name] = None
                continue
            try:
                date_values[field_name] = datetime.strptime(raw_value, "%Y-%m-%d").date()
            except ValueError:
                errors.append(f"employees row {row_number} has invalid ISO {field_name}={raw_value}")
                date_values[field_name] = None

            display_value = row.get(f"{field_name}_display", "")
            try:
                parsed_display = datetime.strptime(display_value, "%d/%m/%Y").date()
            except ValueError:
                errors.append(f"employees row {row_number} has invalid Vietnamese {field_name}_display={display_value}")
                continue
            if parsed_display != date_values[field_name]:
                errors.append(f"employees row {row_number} has inconsistent {field_name}_display")

        birth_date = date_values["date_of_birth"]
        hired_on = date_values["hired_on"]
        terminated_on = date_values["terminated_on"]
        if birth_date and hired_on:
            try:
                adult_cutoff = hired_on.replace(year=hired_on.year - 18)
            except ValueError:
                adult_cutoff = hired_on.replace(year=hired_on.year - 18, day=28)
            if birth_date > adult_cutoff:
                errors.append(f"employees row {row_number} has employee younger than 18 on hired_on")
        if hired_on and terminated_on and terminated_on < hired_on:
            errors.append(f"employees row {row_number} has terminated_on before hired_on")


def main() -> int:
    manifest_path = data_root / "manifest.json"
    if not manifest_path.exists():
        errors.append("manifest.json is missing; run generate_large_dataset.py first")
    manifest = json.loads(manifest_path.read_text(encoding="utf-8")) if manifest_path.exists() else {}
    hr_employees = read_csv("human_resources/employees.csv")
    hr_departments = read_csv("human_resources/departments.csv")
    hr_job_titles = read_csv("human_resources/job_titles.csv")
    hr_contracts = read_csv("human_resources/contracts.csv")
    hr_leave_requests = read_csv("human_resources/leave_requests.csv")
    hr_rewards = read_csv("human_resources/reward_discipline.csv")
    inventory_categories = read_csv("inventory/categories.csv")
    inventory_suppliers = read_csv("inventory/suppliers.csv")
    inventory_warehouses = read_csv("inventory/warehouses.csv")
    inventory_locations = read_csv("inventory/locations.csv")
    inventory_items = read_csv("inventory/stock_items.csv")
    inventory_lots = read_csv("inventory/lots.csv")
    inventory_receipts = read_csv("inventory/receipts.csv")
    inventory_receipt_lines = read_csv("inventory/receipt_lines.csv")
    inventory_issues = read_csv("inventory/issues.csv")
    inventory_issue_lines = read_csv("inventory/issue_lines.csv")
    inventory_transfers = read_csv("inventory/transfers.csv")
    inventory_transfer_lines = read_csv("inventory/transfer_lines.csv")
    inventory_balances = read_csv("inventory/stock_balances.csv")
    production_plans = read_csv("production/plans.csv")
    production_plan_lines = read_csv("production/plan_lines.csv")
    production_boms = read_csv("production/boms.csv")
    production_bom_lines = read_csv("production/bom_lines.csv")
    production_orders = read_csv("production/orders.csv")
    production_requirements = read_csv("production/material_requirements.csv")
    production_events = read_csv("production/order_events.csv")
    production_assignments = read_csv("production/assignments.csv")
    production_consumptions = read_csv("production/material_consumptions.csv")
    production_outputs = read_csv("production/outputs.csv")

    employee_codes = codes(hr_employees, "employee_code", "employees")
    department_codes = codes(hr_departments, "department_code", "departments")
    job_title_codes = codes(hr_job_titles, "job_title_code", "job_titles")
    contract_codes = codes(hr_contracts, "contract_code", "contracts")
    codes(hr_leave_requests, "request_code", "leave_requests")
    codes(hr_rewards, "record_code", "reward_discipline")
    item_codes = codes(inventory_items, "item_code", "stock_items")
    category_codes = codes(inventory_categories, "category_code", "categories")
    supplier_codes = codes(inventory_suppliers, "supplier_code", "suppliers")
    warehouse_codes = codes(inventory_warehouses, "warehouse_code", "warehouses")
    location_codes = codes(inventory_locations, "location_code", "locations")
    lot_codes = {(row["item_code"], row["lot_code"]) for row in inventory_lots}
    receipt_codes = codes(inventory_receipts, "receipt_code", "receipts")
    issue_codes = codes(inventory_issues, "issue_code", "issues")
    transfer_codes = codes(inventory_transfers, "transfer_code", "transfers")
    plan_codes = codes(production_plans, "plan_code", "plans")
    bom_codes = codes(production_boms, "bom_code", "boms")
    order_codes = codes(production_orders, "order_code", "orders")

    if len(hr_employees) < 500:
        errors.append(f"employees has {len(hr_employees)} rows; expected at least 500")
    if len(inventory_items) < 500:
        errors.append(f"stock_items has {len(inventory_items)} rows; expected at least 500")
    if len(production_orders) < 500:
        errors.append(f"orders has {len(production_orders)} rows; expected at least 500")
    validate_employee_contact_and_dates(hr_employees)
    require_reference(hr_employees, "department_code", department_codes, "employees")
    require_reference(hr_employees, "job_title_code", job_title_codes, "employees")
    require_reference(hr_employees, "manager_employee_code", employee_codes, "employees", allow_empty=True)
    require_reference(hr_contracts, "employee_code", employee_codes, "contracts")
    require_reference(hr_leave_requests, "employee_code", employee_codes, "leave_requests")
    require_reference(hr_rewards, "employee_code", employee_codes, "reward_discipline")
    require_reference(inventory_items, "category_code", category_codes, "stock_items")
    require_reference(inventory_suppliers, "supplier_code", supplier_codes, "suppliers")
    require_reference(inventory_locations, "warehouse_code", warehouse_codes, "locations")
    require_reference(inventory_lots, "item_code", item_codes, "lots")
    require_reference(inventory_receipts, "warehouse_code", warehouse_codes, "receipts")
    require_reference(inventory_receipts, "supplier_code", supplier_codes, "receipts", allow_empty=True)
    require_reference(inventory_receipts, "source_document_code", order_codes, "receipts", allow_empty=True)
    require_reference(inventory_receipt_lines, "receipt_code", receipt_codes, "receipt_lines")
    require_reference(inventory_receipt_lines, "item_code", item_codes, "receipt_lines")
    require_reference(inventory_receipt_lines, "location_code", location_codes, "receipt_lines")
    require_reference(inventory_issues, "warehouse_code", warehouse_codes, "issues")
    require_reference(inventory_issue_lines, "issue_code", issue_codes, "issue_lines")
    require_reference(inventory_issue_lines, "item_code", item_codes, "issue_lines")
    require_reference(inventory_issue_lines, "location_code", location_codes, "issue_lines")
    require_reference(inventory_transfers, "source_warehouse_code", warehouse_codes, "transfers")
    require_reference(inventory_transfers, "destination_warehouse_code", warehouse_codes, "transfers")
    require_reference(inventory_transfer_lines, "transfer_code", transfer_codes, "transfer_lines")
    require_reference(inventory_transfer_lines, "item_code", item_codes, "transfer_lines")
    require_reference(production_plan_lines, "plan_code", plan_codes, "plan_lines")
    require_reference(production_plan_lines, "item_code", item_codes, "plan_lines")
    require_reference(production_boms, "item_code", item_codes, "boms")
    require_reference(production_bom_lines, "bom_code", bom_codes, "bom_lines")
    require_reference(production_bom_lines, "material_item_code", item_codes, "bom_lines")
    require_reference(production_orders, "plan_code", plan_codes, "orders")
    require_reference(production_orders, "item_code", item_codes, "orders")
    require_reference(production_orders, "bom_code", bom_codes, "orders")
    require_reference(production_requirements, "order_code", order_codes, "material_requirements")
    require_reference(production_requirements, "bom_code", bom_codes, "material_requirements")
    require_reference(production_requirements, "material_item_code", item_codes, "material_requirements")
    require_reference(production_events, "order_code", order_codes, "order_events")
    require_reference(production_assignments, "order_code", order_codes, "assignments")
    require_reference(production_assignments, "employee_code", employee_codes, "assignments")
    require_reference(production_consumptions, "order_code", order_codes, "material_consumptions")
    require_reference(production_consumptions, "inventory_issue_code", issue_codes, "material_consumptions")
    require_reference(production_outputs, "order_code", order_codes, "outputs")
    require_reference(production_outputs, "item_code", item_codes, "outputs")
    require_reference(production_outputs, "inventory_receipt_code", receipt_codes, "outputs", allow_empty=True)
    for row in inventory_lots + inventory_receipt_lines + inventory_issue_lines + inventory_transfer_lines + inventory_balances:
        item_code = row.get("item_code", "")
        lot_code = row.get("lot_code", "")
        if lot_code and (item_code, lot_code) not in lot_codes:
            errors.append(f"unknown lot {item_code}/{lot_code}")
    for row in inventory_balances:
        if float(row.get("on_hand_quantity", "0")) < 0:
            errors.append(f"negative stock balance for {row.get('item_code')}/{row.get('location_code')}/{row.get('lot_code')}")

    result = {
        "valid": not errors,
        "errors": errors,
        "manifest": manifest,
        "core_counts": {
            "employees": len(hr_employees),
            "stock_items": len(inventory_items),
            "production_orders": len(production_orders),
            "inventory_receipts": len(inventory_receipts),
            "inventory_issues": len(inventory_issues),
            "production_outputs": len(production_outputs),
        },
    }
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0 if not errors else 1


if __name__ == "__main__":
    sys.exit(main())
