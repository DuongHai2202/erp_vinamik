"""Load a balanced sample of ~400-600 records across all ERP Vinamik tables.

Required environment variables:
    ERP_ADMIN_USERNAME
    ERP_ADMIN_PASSWORD

Optional:
    ERP_API_BASE_URL (default: http://127.0.0.1:8080)
"""

import csv
import os
import sys
from pathlib import Path

# Add project root to sys.path
project_root = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(project_root))

import data.load_fixture_via_api as loader

# 1. Pre-calculate needed codes for Production & Inventory
with open(project_root / "data/production/plans.csv", encoding="utf-8-sig") as f:
    sample_plans = list(csv.DictReader(f))[:10]
sample_plan_codes = {p["plan_code"] for p in sample_plans}

with open(project_root / "data/production/plan_lines.csv", encoding="utf-8-sig") as f:
    sample_plan_lines = [l for l in csv.DictReader(f) if l["plan_code"] in sample_plan_codes]
sample_plan_items = {l["item_code"] for l in sample_plan_lines}

with open(project_root / "data/production/boms.csv", encoding="utf-8-sig") as f:
    sample_boms = list(csv.DictReader(f))[:10]
sample_bom_codes = {b["bom_code"] for b in sample_boms}
sample_bom_items = {b["item_code"] for b in sample_boms}

with open(project_root / "data/production/bom_lines.csv", encoding="utf-8-sig") as f:
    sample_bom_lines = [l for l in csv.DictReader(f) if l["bom_code"] in sample_bom_codes]
sample_mat_items = {l["material_item_code"] for l in sample_bom_lines}

with open(project_root / "data/production/orders.csv", encoding="utf-8-sig") as f:
    sample_orders = [o for o in csv.DictReader(f) if o["plan_code"] in sample_plan_codes and o["bom_code"] in sample_bom_codes][:10]
sample_order_codes = {o["order_code"] for o in sample_orders}

production_needed_items = sample_plan_items | sample_bom_items | sample_mat_items

LIMIT_EMPLOYEES = 35
LIMIT_CONTRACTS = 35
LIMIT_LEAVES = 25
LIMIT_REWARDS = 20
LIMIT_PERIODS = 4

LIMIT_SUPPLIERS = 15
LIMIT_WAREHOUSES = 6
LIMIT_RECEIPTS = 15
LIMIT_ISSUES = 10

original_read_rows = loader.read_rows
allowed_employees = set()
allowed_items = set(production_needed_items)
allowed_warehouses = set()
allowed_receipts = set()
allowed_issues = set()


def filtered_read_rows(relative_path: str):
    rows = original_read_rows(relative_path)
    rel = relative_path.replace("\\", "/")

    # HR
    if rel == "human_resources/departments.csv":
        return rows
    if rel == "human_resources/job_titles.csv":
        return rows
    if rel == "human_resources/work_shifts.csv":
        return rows
    if rel == "human_resources/employees.csv":
        selected = rows[:LIMIT_EMPLOYEES]
        for r in selected:
            allowed_employees.add(r["employee_code"])
        return selected
    if rel == "human_resources/contracts.csv":
        return [r for r in rows if r["employee_code"] in allowed_employees][:LIMIT_CONTRACTS]
    if rel == "human_resources/leave_requests.csv":
        return [r for r in rows if r["employee_code"] in allowed_employees][:LIMIT_LEAVES]
    if rel == "human_resources/reward_discipline.csv":
        return [r for r in rows if r["employee_code"] in allowed_employees][:LIMIT_REWARDS]
    if rel == "human_resources/payroll_periods.csv":
        return rows[:LIMIT_PERIODS]

    # Inventory Master
    if rel == "inventory/categories.csv":
        return rows
    if rel == "inventory/suppliers.csv":
        return rows[:LIMIT_SUPPLIERS]
    if rel == "inventory/warehouses.csv":
        selected = rows[:LIMIT_WAREHOUSES]
        for r in selected:
            allowed_warehouses.add(r["warehouse_code"])
        return selected
    if rel == "inventory/locations.csv":
        return [r for r in rows if r["warehouse_code"] in allowed_warehouses]
    if rel == "inventory/stock_items.csv":
        # Include production needed items plus extra raw materials
        selected = [r for r in rows if r["item_code"] in production_needed_items]
        extras = [r for r in rows if r["item_code"] not in production_needed_items][:20]
        full_selected = selected + extras
        for r in full_selected:
            allowed_items.add(r["item_code"])
        return full_selected

    # Inventory Transactions
    if rel == "inventory/receipts.csv":
        selected = [r for r in rows if r.get("warehouse_code") in allowed_warehouses][:LIMIT_RECEIPTS]
        for r in selected:
            allowed_receipts.add(r["receipt_code"])
        return selected
    if rel == "inventory/receipt_lines.csv":
        return [r for r in rows if r["receipt_code"] in allowed_receipts and r["item_code"] in allowed_items]
    if rel == "inventory/issues.csv":
        selected = [r for r in rows if r.get("warehouse_code") in allowed_warehouses][:LIMIT_ISSUES]
        for r in selected:
            allowed_issues.add(r["issue_code"])
        return selected
    if rel == "inventory/issue_lines.csv":
        return [r for r in rows if r["issue_code"] in allowed_issues and r["item_code"] in allowed_items]
    if rel == "inventory/transfers.csv":
        return []
    if rel == "inventory/transfer_lines.csv":
        return []

    # Production
    if rel == "production/plans.csv":
        return sample_plans
    if rel == "production/plan_lines.csv":
        return [r for r in rows if r["plan_code"] in sample_plan_codes and r["item_code"] in allowed_items]
    if rel == "production/boms.csv":
        return sample_boms
    if rel == "production/bom_lines.csv":
        return [r for r in rows if r["bom_code"] in sample_bom_codes and r["material_item_code"] in allowed_items]
    if rel == "production/orders.csv":
        return sample_orders
    if rel == "production/assignments.csv":
        return [r for r in rows if r["order_code"] in sample_order_codes and r["employee_code"] in allowed_employees][:15]
    if rel == "production/material_consumptions.csv":
        return [r for r in rows if r["order_code"] in sample_order_codes and r["material_item_code"] in allowed_items][:15]
    if rel == "production/outputs.csv":
        return [r for r in rows if r["order_code"] in sample_order_codes and r["item_code"] in allowed_items][:10]

    return rows


loader.read_rows = filtered_read_rows


def main() -> int:
    username = os.environ.get("ERP_ADMIN_USERNAME")
    password = os.environ.get("ERP_ADMIN_PASSWORD")
    api_url = os.environ.get("ERP_API_BASE_URL", "http://127.0.0.1:8080")
    if not username or not password:
        print("ERP_ADMIN_USERNAME and ERP_ADMIN_PASSWORD are required.", file=sys.stderr)
        return 2

    print(f"Connecting to {api_url} as {username}...")
    client = loader.api_client(api_url, username, password)
    try:
        user = client.login()
        print(f"Authenticated as {user.get('username')} (super_admin={user.get('super_admin')}).")

        context = loader.load_context(client=client, maps={}, failures=[], skipped=[])

        print("\n--- PHASE 1: Loading Human Resources ---")
        loader.load_hr(context)

        print("\n--- PHASE 2: Loading Inventory Master ---")
        loader.load_inventory_master(context)

        print("\n--- PHASE 3: Loading Inventory Transactions ---")
        loader.load_inventory_transactions(context)

        print("\n--- PHASE 4: Loading Production ---")
        loader.load_production(context)
    except Exception as error:
        print(f"Balanced sample load failed: {error}", file=sys.stderr)
        return 1

    print("\n=======================================================")
    print("BALANCED SAMPLE LOAD FINISHED!")
    print(f"Total failures: {len(context.failures)}")
    print("=======================================================")
    return 1 if context.failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
