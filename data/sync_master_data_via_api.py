#!/usr/bin/env python3
"""Synchronize display-oriented master data through the ERP API.

The fixture loader intentionally skips existing business keys to preserve
records. This operator utility is the explicit, auditable step for aligning
Vietnamese display names, addresses and supplier contact metadata with the
versioned fixture without bypassing backend validation, permissions or audit
logging.

Required environment variables:
    ERP_ADMIN_USERNAME
    ERP_ADMIN_PASSWORD

Optional:
    ERP_API_BASE_URL (default: http://127.0.0.1:8080)
"""

from __future__ import annotations

import argparse
import csv
import json
import os
import sys
from pathlib import Path
from typing import Any

from load_fixture_via_api import api_client


project_root = Path(__file__).resolve().parents[1]
api_base_url = os.environ.get("ERP_API_BASE_URL", "http://127.0.0.1:8080").rstrip("/")


def read_rows(relative_path: str) -> list[dict[str, str]]:
    with (project_root / "data" / relative_path).open(
        "r", encoding="utf-8-sig", newline=""
    ) as input_file:
        return list(csv.DictReader(input_file))


def list_items(client: api_client, path: str) -> list[dict[str, Any]]:
    result = client.get_data(path, {"page": 0, "page_size": 100})
    return result.get("items", []) if isinstance(result, dict) else []


def sync_master_data(client: api_client) -> dict[str, int]:
    updated = 0
    counts: dict[str, int] = {}

    def put(path: str, payload: dict[str, Any]) -> None:
        nonlocal updated
        client.request("PUT", path, payload)
        updated += 1

    departments = list_items(client, "/api/v1/human_resources/master_data/departments")
    department_by_code = {item["department_code"]: item for item in departments}
    department_rows = read_rows("human_resources/departments.csv")
    for row in department_rows:
        current = department_by_code[row["department_code"]]
        parent = department_by_code.get(row.get("parent_department_code") or "")
        put(
            f"/api/v1/human_resources/master_data/departments/{current['department_id']}",
            {
                "department_code": row["department_code"],
                "department_name": row["department_name"],
                "parent_department_id": parent["department_id"] if parent else None,
                "status": current.get("status") or "active",
            },
        )
    counts["departments"] = len(department_rows)

    job_titles = list_items(client, "/api/v1/human_resources/master_data/job_titles")
    job_title_by_code = {item["job_title_code"]: item for item in job_titles}
    job_title_rows = read_rows("human_resources/job_titles.csv")
    for row in job_title_rows:
        current = job_title_by_code[row["job_title_code"]]
        put(
            f"/api/v1/human_resources/master_data/job_titles/{current['job_title_id']}",
            {
                "job_title_code": row["job_title_code"],
                "job_title_name": row["job_title_name"],
                "description": current.get("description")
                or f"Mức tham chiếu {row.get('base_salary_reference', '')} VND.",
                "status": current.get("status") or "active",
            },
        )
    counts["job_titles"] = len(job_title_rows)

    work_shifts = list_items(client, "/api/v1/human_resources/master_data/work_shifts")
    work_shift_by_code = {item["shift_code"]: item for item in work_shifts}
    work_shift_rows = read_rows("human_resources/work_shifts.csv")
    for row in work_shift_rows:
        current = work_shift_by_code[row["shift_code"]]
        put(
            f"/api/v1/human_resources/master_data/work_shifts/{current['work_shift_id']}",
            {
                "shift_code": row["shift_code"],
                "shift_name": row["shift_name"],
                "starts_at": row["starts_at"],
                "ends_at": row["ends_at"],
                "status": current.get("status") or "active",
            },
        )
    counts["work_shifts"] = len(work_shift_rows)

    units = list_items(client, "/api/v1/inventory/master_data/manage/units")
    unit_names = {
        "kg": "Kilôgam",
        "litre": "Lít",
        "piece": "Cái",
        "gram": "Gam",
        "box": "Thùng",
        "metre": "Mét",
        "roll": "Cuộn",
    }
    unit_count = 0
    for current in units:
        name = unit_names.get(current["unit_code"])
        if not name:
            continue
        put(
            f"/api/v1/inventory/master_data/units/{current['unit_of_measure_id']}",
            {
                "unit_code": current["unit_code"],
                "unit_name": name,
                "decimal_places": current.get("decimal_places", 3),
                "status": current.get("status") or "active",
            },
        )
        unit_count += 1
    counts["units"] = unit_count

    categories = list_items(client, "/api/v1/inventory/master_data/manage/categories")
    category_by_code = {item["category_code"]: item for item in categories}
    category_rows = read_rows("inventory/categories.csv")
    for row in category_rows:
        current = category_by_code[row["category_code"]]
        put(
            f"/api/v1/inventory/master_data/categories/{current['item_category_id']}",
            {
                "category_code": row["category_code"],
                "category_name": row["category_name"],
                "status": current.get("status") or "active",
            },
        )
    counts["categories"] = len(category_rows)

    suppliers = list_items(client, "/api/v1/inventory/master_data/manage/suppliers")
    supplier_by_code = {item["supplier_code"]: item for item in suppliers}
    supplier_rows = read_rows("inventory/suppliers.csv")
    for row in supplier_rows:
        current = supplier_by_code[row["supplier_code"]]
        put(
            f"/api/v1/inventory/master_data/suppliers/{current['supplier_id']}",
            {
                "supplier_code": row["supplier_code"],
                "supplier_name": row["supplier_name"],
                "phone_number": row.get("phone_number") or None,
                "email": row.get("email") or None,
                "address": row.get("address") or None,
                "status": row.get("status") or current.get("status") or "active",
            },
        )
    counts["suppliers"] = len(supplier_rows)

    warehouses = list_items(client, "/api/v1/inventory/master_data/manage/warehouses")
    warehouse_by_code = {item["warehouse_code"]: item for item in warehouses}
    warehouse_rows = read_rows("inventory/warehouses.csv")
    for row in warehouse_rows:
        current = warehouse_by_code[row["warehouse_code"]]
        put(
            f"/api/v1/inventory/master_data/warehouses/{current['warehouse_id']}",
            {
                "warehouse_code": row["warehouse_code"],
                "warehouse_name": row["warehouse_name"],
                "address": row.get("address") or None,
                "status": row.get("status") or current.get("status") or "active",
            },
        )
    counts["warehouses"] = len(warehouse_rows)

    location_rows = read_rows("inventory/locations.csv")
    location_count = 0
    for warehouse_code, warehouse in warehouse_by_code.items():
        locations = list_items(
            client,
            f"/api/v1/inventory/master_data/manage/warehouses/{warehouse['warehouse_id']}/locations",
        )
        location_by_code = {item["location_code"]: item for item in locations}
        for row in [item for item in location_rows if item["warehouse_code"] == warehouse_code]:
            current = location_by_code[row["location_code"]]
            put(
                f"/api/v1/inventory/master_data/locations/{current['warehouse_location_id']}",
                {
                    "warehouse_id": warehouse["warehouse_id"],
                    "location_code": row["location_code"],
                    "location_name": row["location_name"],
                    "status": row.get("status") or current.get("status") or "active",
                },
            )
            location_count += 1
    counts["locations"] = location_count
    return {"updated": updated, **counts}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--api-base-url",
        default=api_base_url,
        help="ERP API base URL (default: ERP_API_BASE_URL or http://127.0.0.1:8080)",
    )
    args = parser.parse_args()
    username = os.environ.get("ERP_ADMIN_USERNAME")
    password = os.environ.get("ERP_ADMIN_PASSWORD")
    if not username or not password:
        parser.error("ERP_ADMIN_USERNAME and ERP_ADMIN_PASSWORD are required.")
    client = api_client(args.api_base_url, username, password)
    client.login()
    result = sync_master_data(client)
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
