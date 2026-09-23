#!/usr/bin/env python3
"""Validate generated ERP fixture counts and business-key references."""

from __future__ import annotations

import csv
import json
import re
import sys
from datetime import date, datetime
from collections import defaultdict
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


def require_status_coverage(
    rows: list[dict[str, str]],
    field_name: str,
    expected: set[str],
    label: str,
) -> None:
    actual = {row.get(field_name, "") for row in rows}
    missing = expected - actual
    if missing:
        errors.append(
            f"{label} missing statuses in {field_name}: {', '.join(sorted(missing))}"
        )


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
    inventory_stocktakes = read_csv("inventory/stocktakes.csv")
    inventory_balances = read_csv("inventory/stock_balances.csv")
    inventory_movements = read_csv("inventory/stock_movements.csv")
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
    quality_cost_rules = read_csv("quality_cost/cost_rules.csv")
    quality_cost_periods = read_csv("quality_cost/cost_periods.csv")
    quality_inspections = read_csv("quality_cost/inspections.csv")
    quality_nonconformances = read_csv("quality_cost/nonconformances.csv")
    quality_calculations = read_csv("quality_cost/calculations.csv")
    quality_price_proposals = read_csv("quality_cost/price_proposals.csv")

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
    stocktake_codes = codes(inventory_stocktakes, "stocktake_code", "stocktakes")
    plan_codes = codes(production_plans, "plan_code", "plans")
    bom_codes = codes(production_boms, "bom_code", "boms")
    order_codes = codes(production_orders, "order_code", "orders")
    quality_rule_codes = codes(quality_cost_rules, "rule_code", "quality_cost_rules")
    quality_period_codes = codes(quality_cost_periods, "period_code", "quality_cost_periods")
    quality_inspection_codes = codes(quality_inspections, "inspection_code", "quality_inspections")
    codes(quality_nonconformances, "nonconformance_code", "quality_nonconformances")
    codes(quality_calculations, "calculation_code", "quality_calculations")
    codes(quality_price_proposals, "proposal_code", "quality_price_proposals")

    if len(hr_employees) < 500:
        errors.append(f"employees has {len(hr_employees)} rows; expected at least 500")
    if len(inventory_items) < 500:
        errors.append(f"stock_items has {len(inventory_items)} rows; expected at least 500")
    if len(inventory_stocktakes) < 5:
        errors.append(f"stocktakes has {len(inventory_stocktakes)} rows; expected at least 5")
    if len(production_orders) < 500:
        errors.append(f"orders has {len(production_orders)} rows; expected at least 500")
    if len(quality_cost_rules) < 12:
        errors.append(f"quality_cost rules has {len(quality_cost_rules)} rows; expected at least 12")
    if len(quality_cost_periods) < 12:
        errors.append(f"quality_cost periods has {len(quality_cost_periods)} rows; expected at least 12")
    if len(quality_inspections) < 100:
        errors.append(f"quality_cost inspections has {len(quality_inspections)} rows; expected at least 100")
    if len(quality_calculations) < 100:
        errors.append(f"quality_cost calculations has {len(quality_calculations)} rows; expected at least 100")
    if len(quality_price_proposals) < 100:
        errors.append(f"quality_cost price proposals has {len(quality_price_proposals)} rows; expected at least 100")
    require_status_coverage(hr_employees, "employment_status",
                            {"active", "on_leave", "inactive", "terminated"}, "employees")
    require_status_coverage(hr_contracts, "status",
                            {"draft", "active", "expired", "terminated", "cancelled"}, "contracts")
    require_status_coverage(hr_leave_requests, "status",
                            {"draft", "pending", "approved", "rejected", "cancelled"}, "leave_requests")
    require_status_coverage(hr_rewards, "status",
                            {"draft", "pending", "approved", "rejected", "cancelled"}, "reward_discipline")
    require_status_coverage(read_csv("human_resources/payroll_periods.csv"), "status",
                            {"draft", "calculated", "approved", "rejected", "locked"}, "payroll_periods")
    require_status_coverage(inventory_items, "status", {"active", "inactive"}, "stock_items")
    require_status_coverage(inventory_suppliers, "status", {"active", "inactive"}, "suppliers")
    require_status_coverage(inventory_receipts, "status",
                            {"draft", "pending", "posted", "cancelled"}, "receipts")
    require_status_coverage(inventory_issues, "status",
                            {"draft", "pending", "posted", "cancelled"}, "issues")
    require_status_coverage(inventory_transfers, "status",
                            {"draft", "pending", "posted", "cancelled"}, "transfers")
    require_status_coverage(inventory_stocktakes, "status",
                            {"counting", "submitted", "approved", "posted", "cancelled"}, "stocktakes")
    require_status_coverage(production_plans, "status",
                            {"draft", "approved", "released", "completed"}, "production_plans")
    require_status_coverage(production_orders, "status",
                            {"planned", "released", "in_progress", "paused", "completed"}, "production_orders")
    require_status_coverage(production_assignments, "status",
                            {"planned", "active", "completed", "cancelled"}, "assignments")
    require_status_coverage(production_outputs, "status",
                            {"draft", "pending_receipt", "received", "failed", "cancelled"}, "outputs")
    require_status_coverage(quality_cost_periods, "target_status",
                            {"draft", "open", "calculating", "calculated", "approved", "locked", "cancelled"},
                            "quality_cost_periods")
    require_status_coverage(quality_inspections, "target_status",
                            {"draft", "submitted", "passed", "failed", "held", "released", "cancelled"},
                            "quality_inspections")
    require_status_coverage(quality_nonconformances, "target_status",
                            {"open", "in_progress", "resolved", "cancelled"}, "quality_nonconformances")
    require_status_coverage(quality_calculations, "target_status",
                            {"calculated", "approved", "locked", "cancelled"}, "quality_calculations")
    require_status_coverage(quality_price_proposals, "target_status",
                            {"draft", "pending", "approved", "rejected", "published", "cancelled"},
                            "quality_price_proposals")
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
    require_reference(inventory_stocktakes, "warehouse_code", warehouse_codes, "stocktakes")
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
    require_reference(quality_cost_periods, "rule_code", quality_rule_codes, "quality_cost_periods")
    require_reference(quality_inspections, "production_order_code", order_codes, "quality_inspections")
    require_reference(quality_inspections, "stock_item_code", item_codes, "quality_inspections")
    require_reference(quality_nonconformances, "inspection_code", quality_inspection_codes, "quality_nonconformances")
    require_reference(quality_calculations, "period_code", quality_period_codes, "quality_calculations")
    require_reference(quality_calculations, "production_order_code", order_codes, "quality_calculations")
    require_reference(quality_calculations, "stock_item_code", item_codes, "quality_calculations")
    require_reference(quality_price_proposals, "period_code", quality_period_codes, "quality_price_proposals")
    require_reference(quality_price_proposals, "stock_item_code", item_codes, "quality_price_proposals")
    for row in inventory_lots + inventory_receipt_lines + inventory_issue_lines + inventory_transfer_lines + inventory_balances:
        item_code = row.get("item_code", "")
        lot_code = row.get("lot_code", "")
        if lot_code and (item_code, lot_code) not in lot_codes:
            errors.append(f"unknown lot {item_code}/{lot_code}")
    for row in inventory_balances:
        if float(row.get("on_hand_quantity", "0")) < 0:
            errors.append(f"negative stock balance for {row.get("item_code")}/{row.get("location_code")}/{row.get("lot_code")}")

    location_to_warehouse = {row["location_code"]: row["warehouse_code"] for row in inventory_locations}
    receipt_by_code = {row["receipt_code"]: row for row in inventory_receipts}
    issue_by_code = {row["issue_code"]: row for row in inventory_issues}
    transfer_by_code = {row["transfer_code"]: row for row in inventory_transfers}
    for line_number, row in enumerate(inventory_receipt_lines, start=2):
        parent = receipt_by_code.get(row["receipt_code"])
        if parent and location_to_warehouse.get(row["location_code"]) != parent["warehouse_code"]:
            errors.append(f"receipt line {line_number} location does not belong to receipt warehouse")
    for line_number, row in enumerate(inventory_issue_lines, start=2):
        parent = issue_by_code.get(row["issue_code"])
        if parent and location_to_warehouse.get(row["location_code"]) != parent["warehouse_code"]:
            errors.append(f"issue line {line_number} location does not belong to issue warehouse")
    for line_number, row in enumerate(inventory_transfer_lines, start=2):
        parent = transfer_by_code.get(row["transfer_code"])
        if not parent:
            continue
        if location_to_warehouse.get(row["source_location_code"]) != parent["source_warehouse_code"]:
            errors.append(f"transfer line {line_number} source location does not belong to source warehouse")
        if location_to_warehouse.get(row["destination_location_code"]) != parent["destination_warehouse_code"]:
            errors.append(f"transfer line {line_number} destination location does not belong to destination warehouse")

    movement_totals = defaultdict(float)
    balance_totals = defaultdict(float)
    for row in inventory_movements:
        key = (row["item_code"], row["location_code"], row.get("lot_code", ""))
        movement_totals[key] += float(row["quantity_delta"])
    for row in inventory_balances:
        key = (row["item_code"], row["location_code"], row.get("lot_code", ""))
        balance_totals[key] += float(row["on_hand_quantity"])
    for key in set(movement_totals) | set(balance_totals):
        if abs(round(movement_totals[key] - max(balance_totals[key], 0), 6)) > 0.000001:
            errors.append(f"stock balance reconciliation failed for {key}: movement={movement_totals[key]}, balance={balance_totals[key]}")

    plan_lines_by_key = {(row["plan_code"], int(row["line_number"])): row for row in production_plan_lines}
    boms_by_code = {row["bom_code"]: row for row in production_boms}
    bom_lines_by_code = {row["bom_line_code"]: row for row in production_bom_lines}
    orders_by_code = {row["order_code"]: row for row in production_orders}
    for line_number, row in enumerate(production_orders, start=2):
        plan_line = plan_lines_by_key.get((row["plan_code"], int(row["plan_line_number"])))
        if not plan_line or plan_line["item_code"] != row["item_code"]:
            errors.append(f"production order {line_number} does not match its plan line")
        bom = boms_by_code.get(row["bom_code"])
        if not bom or bom["item_code"] != row["item_code"]:
            errors.append(f"production order {line_number} does not match its BOM item")
        if float(row["target_quantity"]) <= 0:
            errors.append(f"production order {line_number} has non-positive target quantity")
    for line_number, row in enumerate(production_requirements, start=2):
        bom_line = bom_lines_by_code.get(row["bom_line_code"])
        order = orders_by_code.get(row["order_code"])
        if not bom_line or bom_line["bom_code"] != row["bom_code"] or bom_line["material_item_code"] != row["material_item_code"]:
            errors.append(f"material requirement {line_number} does not match its BOM line")
        if bom_line and order:
            expected = round(float(bom_line["quantity_per_base"]) * float(order["target_quantity"]) / float(row["base_quantity_snapshot"]) * (1 + float(row["scrap_percent_snapshot"]) / 100), 6)
            if abs(expected - round(float(row["required_quantity"]), 6)) > 0.000001:
                errors.append(f"material requirement {line_number} formula mismatch")
    for line_number, row in enumerate(production_outputs, start=2):
        order = orders_by_code.get(row["order_code"])
        total_quantity = float(row["good_quantity"]) + float(row["defective_quantity"])
        if order and total_quantity > float(order["target_quantity"]) + 0.000001:
            errors.append(f"production output {line_number} exceeds order target")
        manufactured_on = datetime.strptime(row["manufactured_on"], "%Y-%m-%d").date()
        expires_on = datetime.strptime(row["expires_on"], "%Y-%m-%d").date()
        if expires_on <= manufactured_on:
            errors.append(f"production output {line_number} expires before manufactured date")

    for line_number, row in enumerate(quality_inspections, start=2):
        inspected = float(row["inspected_quantity"])
        good = float(row["good_quantity"])
        defective = float(row["defective_quantity"])
        if inspected <= 0 or good < 0 or defective < 0 or good + defective > inspected + 0.000001:
            errors.append(f"quality inspection {line_number} has invalid quantity split")
        if row.get("target_status") not in {"draft", "submitted", "passed", "failed", "held", "released", "cancelled"}:
            errors.append(f"quality inspection {line_number} has invalid target status")
    inspections_by_code = {row["inspection_code"]: row for row in quality_inspections}
    for line_number, row in enumerate(quality_nonconformances, start=2):
        inspection = inspections_by_code.get(row["inspection_code"])
        if inspection is None or float(row["quantity"]) <= 0:
            errors.append(f"quality nonconformance {line_number} has invalid inspection or quantity")
        if row.get("target_status") not in {"open", "in_progress", "resolved", "cancelled"}:
            errors.append(f"quality nonconformance {line_number} has invalid target status")
    for line_number, row in enumerate(quality_calculations, start=2):
        material = round(float(row["material_cost"]), 2)
        labor = round(float(row["direct_labor_cost"]), 2)
        overhead = round(float(row["overhead_cost"]), 2)
        adjustment = round(float(row["adjustment_amount"]), 2)
        expected_total = round(material + labor + overhead + adjustment, 2)
        actual_total = round(float(row["total_cost"]), 2)
        if abs(expected_total - actual_total) > 0.01:
            errors.append(f"quality calculation {line_number} total formula mismatch")
        good_quantity = float(row["good_quantity"])
        expected_unit = round(actual_total / good_quantity, 6)
        if abs(expected_unit - round(float(row["unit_cost"]), 6)) > 0.000001:
            errors.append(f"quality calculation {line_number} unit formula mismatch")
        if row.get("target_status") not in {"calculated", "approved", "locked", "cancelled"}:
            errors.append(f"quality calculation {line_number} has invalid target status")
    for line_number, row in enumerate(quality_price_proposals, start=2):
        expected_price = round(float(row["unit_cost"]) * (1 + float(row["margin_percent"]) / 100), 2)
        if abs(expected_price - round(float(row["proposed_price"]), 2)) > 0.01:
            errors.append(f"quality price proposal {line_number} margin formula mismatch")
        if row.get("target_status") not in {"draft", "pending", "approved", "rejected", "published", "cancelled"}:
            errors.append(f"quality price proposal {line_number} has invalid target status")

    warnings = []
    placeholder_pattern = re.compile(r"\b(?:demo|fake|mock|placeholder|sample|mô phỏng|tải lớn|phục vụ kiểm thử)\b", re.IGNORECASE)
    for file_name, rows in {
        "employees": hr_employees, "contracts": hr_contracts, "stock_items": inventory_items,
        "plans": production_plans, "boms": production_boms, "orders": production_orders,
    }.items():
        for line_number, row in enumerate(rows, start=2):
            for field_name, value in row.items():
                if value and placeholder_pattern.search(value):
                    warnings.append(f"{file_name} row {line_number} field {field_name} contains visible test wording")

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
            "inventory_stocktakes": len(inventory_stocktakes),
            "production_outputs": len(production_outputs),
            "inventory_movements": len(inventory_movements),
            "inventory_balances": len(inventory_balances),
            "quality_cost_rules": len(quality_cost_rules),
            "quality_cost_periods": len(quality_cost_periods),
            "quality_inspections": len(quality_inspections),
            "quality_nonconformances": len(quality_nonconformances),
            "quality_calculations": len(quality_calculations),
            "quality_price_proposals": len(quality_price_proposals),
        },
        "warnings": warnings,
    }
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0 if not errors else 1


if __name__ == "__main__":
    sys.exit(main())
