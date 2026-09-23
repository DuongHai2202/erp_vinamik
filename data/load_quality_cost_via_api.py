
#!/usr/bin/env python3
"""Load quality and cost fixtures through the authenticated ERP API.

This loader never writes PostgreSQL directly. It resolves stock, order and output
references through their public read contracts, then creates quality/cost records
through the quality_cost service so validation, authorization and audit remain active.
"""

from __future__ import annotations

import json
import os
import sys
from pathlib import Path
from typing import Any

project_root = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(project_root / "data"))

from load_fixture_via_api import api_client, api_error, read_rows


api_base_url = os.environ.get("ERP_API_BASE_URL", "http://127.0.0.1:8080").rstrip("/")
username = os.environ.get("ERP_ADMIN_USERNAME")
password = os.environ.get("ERP_ADMIN_PASSWORD")
if not username or not password:
    raise SystemExit("Set ERP_ADMIN_USERNAME and ERP_ADMIN_PASSWORD before loading quality_cost data.")


def all_items(client: api_client, path: str, fixed_query: dict[str, Any] | None = None) -> list[dict[str, Any]]:
    result: list[dict[str, Any]] = []
    page = 0
    while True:
        query = {"page": page, "page_size": 100}
        if fixed_query:
            query.update(fixed_query)
        data = client.get_data(path, query)
        items = data.get("items", []) if isinstance(data, dict) else []
        result.extend(items)
        if not isinstance(data, dict) or page + 1 >= int(data.get("total_pages", 0)):
            return result
        page += 1


def quality_items(client: api_client, resource: str) -> list[dict[str, Any]]:
    return all_items(client, f"/api/v1/quality_cost/{resource}")


def index_by(items: list[dict[str, Any]], field: str) -> dict[str, int]:
    return {
        str(item[field]): int(item["id"])
        for item in items
        if item.get(field) is not None and item.get("id") is not None
    }


def find_output_ids(client: api_client, order_map: dict[str, int], output_keys: set[str]) -> dict[str, int]:
    result: dict[str, int] = {}
    for order_code, order_id in order_map.items():
        if not any(key.startswith("production_output_") for key in output_keys):
            break
        data = client.get_data(f"/api/v1/production/orders/{order_id}/outputs", {"page": 0, "page_size": 100})
        for item in data if isinstance(data, list) else data.get("items", []):
            key = item.get("idempotency_key")
            if key in output_keys and item.get("production_output_id") is not None:
                result[key] = int(item["production_output_id"])
    return result


def create_or_get(
    client: api_client,
    resource: str,
    code_field: str,
    code: str,
    payload: dict[str, Any],
    existing: dict[str, int],
    created: list[dict[str, str]],
    skipped: list[dict[str, str]],
) -> int:
    if code in existing:
        skipped.append({"resource": resource, "code": code, "reason": "already exists"})
        return existing[code]
    value = client.post_data(f"/api/v1/quality_cost/{resource}", payload)
    record_id = int(value.get("id")) if isinstance(value, dict) and value.get("id") is not None else None
    if record_id is None:
        raise api_error("POST", f"/api/v1/quality_cost/{resource}", 500, "The response did not contain a record id.", value)
    existing[code] = record_id
    created.append({"resource": resource, "code": code})
    return record_id


def advance_status(
    client: api_client,
    resource: str,
    record_id: int,
    target: str,
    paths: dict[str, list[str]],
    failures: list[dict[str, str]],
) -> None:
    detail = client.get_data(f"/api/v1/quality_cost/{resource}/{record_id}")
    current = str(detail.get("status", "")) if isinstance(detail, dict) else ""
    if current == target or not target:
        return
    path = paths.get(target)
    if not path or current not in path:
        failures.append({"resource": resource, "id": str(record_id), "error": f"Cannot advance {current} to {target}."})
        return
    start = path.index(current)
    try:
        for next_status in path[start + 1:]:
            client.post_data(f"/api/v1/quality_cost/{resource}/{record_id}/status", {"status": next_status, "decision_note": "Nạp dữ liệu quy trình cho môi trường kiểm thử."})
            if next_status == target:
                return
    except Exception as error:
        failures.append({"resource": resource, "id": str(record_id), "error": str(error)})


def main() -> int:
    client = api_client(api_base_url, username, password)
    client.login()
    created: list[dict[str, str]] = []
    skipped: list[dict[str, str]] = []
    failures: list[dict[str, str]] = []

    stock_items = (
        all_items(client, "/api/v1/inventory/materials", {"item_type": "raw_material"})
        + all_items(client, "/api/v1/inventory/materials", {"item_type": "finished_product"})
    )
    stock_map = {str(item["item_code"]): int(item["stock_item_id"]) for item in stock_items if item.get("item_code") and item.get("stock_item_id") is not None}
    stock_name_map = {str(item["item_code"]): str(item.get("item_name") or "") for item in stock_items}
    orders = all_items(client, "/api/v1/production/orders")
    order_map = {str(item["order_code"]): int(item["production_order_id"]) for item in orders if item.get("order_code") and item.get("production_order_id") is not None}

    rule_rows = read_rows("quality_cost/cost_rules.csv")
    period_rows = read_rows("quality_cost/cost_periods.csv")
    inspection_rows = read_rows("quality_cost/inspections.csv")
    nonconformance_rows = read_rows("quality_cost/nonconformances.csv")
    calculation_rows = read_rows("quality_cost/calculations.csv")
    price_rows = read_rows("quality_cost/price_proposals.csv")

    existing_rules = index_by(quality_items(client, "rules"), "code")
    existing_periods = index_by(quality_items(client, "periods"), "code")
    existing_inspections = index_by(quality_items(client, "inspections"), "code")
    existing_nonconformances = index_by(quality_items(client, "nonconformances"), "code")
    existing_prices = index_by(quality_items(client, "price_proposals"), "code")
    existing_calculations = {}
    for item in quality_items(client, "calculations"):
        notes = str(item.get("notes") or "")
        for row in calculation_rows:
            if row["calculation_code"] in notes and item.get("id") is not None:
                existing_calculations[row["calculation_code"]] = int(item["id"])

    output_keys = {row["production_output_code"] for row in inspection_rows if row.get("production_output_code")}
    related_order_codes = {row["production_order_code"] for row in inspection_rows if row.get("production_order_code")}
    related_order_map = {code: order_map[code] for code in related_order_codes if code in order_map}
    output_map = find_output_ids(client, related_order_map, output_keys)

    rule_ids: dict[str, int] = {}
    for row in rule_rows:
        try:
            rule_ids[row["rule_code"]] = create_or_get(client, "rules", "code", row["rule_code"], {
                "rule_code": row["rule_code"],
                "material_valuation_method": row["material_valuation_method"],
                "labor_basis": row["labor_basis"],
                "overhead_basis": row["overhead_basis"],
                "defective_policy": row["defective_policy"],
                "effective_from": row["effective_from"],
                "effective_to": row.get("effective_to") or None,
                "currency_code": row["currency_code"],
                "rounding_scale": int(row["rounding_scale"]),
                "notes": row["notes"],
            }, existing_rules, created, skipped)
        except Exception as error:
            failures.append({"resource": "rules", "code": row["rule_code"], "error": str(error)})

    period_ids: dict[str, int] = {}
    for row in period_rows:
        try:
            rule_id = rule_ids[row["rule_code"]]
            period_ids[row["period_code"]] = create_or_get(client, "periods", "code", row["period_code"], {
                "period_code": row["period_code"],
                "starts_on": row["starts_on"],
                "ends_on": row["ends_on"],
                "rule_version_id": rule_id,
                "notes": row["notes"],
            }, existing_periods, created, skipped)
        except Exception as error:
            failures.append({"resource": "periods", "code": row["period_code"], "error": str(error)})

    inspection_ids: dict[str, int] = {}
    for row in inspection_rows:
        try:
            order_id = order_map[row["production_order_code"]]
            stock_id = stock_map[row["stock_item_code"]]
            output_id = output_map.get(row.get("production_output_code", ""))
            inspection_ids[row["inspection_code"]] = create_or_get(client, "inspections", "code", row["inspection_code"], {
                "inspection_code": row["inspection_code"],
                "production_order_id": order_id,
                "production_output_id": output_id,
                "stock_item_id": stock_id,
                "lot_code": row.get("lot_code") or None,
                "inspected_quantity": row["inspected_quantity"],
                "good_quantity": row["good_quantity"],
                "defective_quantity": row["defective_quantity"],
                "inspected_on": row["inspected_on"],
                "notes": row["notes"],
            }, existing_inspections, created, skipped)
        except Exception as error:
            failures.append({"resource": "inspections", "code": row["inspection_code"], "error": str(error)})

    for row in nonconformance_rows:
        try:
            inspection_id = inspection_ids[row["inspection_code"]]
            create_or_get(client, "nonconformances", "code", row["nonconformance_code"], {
                "nonconformance_code": row["nonconformance_code"],
                "inspection_id": inspection_id,
                "defect_code": row["defect_code"],
                "quantity": row["quantity"],
                "disposition": row["disposition"],
                "notes": row["notes"],
            }, existing_nonconformances, created, skipped)
        except Exception as error:
            failures.append({"resource": "nonconformances", "code": row["nonconformance_code"], "error": str(error)})

    calculation_ids: dict[str, int] = {}
    for row in calculation_rows:
        try:
            calculation_ids[row["calculation_code"]] = create_or_get(client, "calculations", "calculation_code", row["calculation_code"], {
                "cost_period_id": period_ids[row["period_code"]],
                "production_order_id": order_map[row["production_order_code"]],
                "stock_item_id": stock_map[row["stock_item_code"]],
                "stock_item_code": row["stock_item_code"],
                "stock_item_name": stock_name_map.get(row["stock_item_code"], row["stock_item_name"]),
                "good_quantity": row["good_quantity"],
                "material_cost": row["material_cost"],
                "direct_labor_cost": row["direct_labor_cost"],
                "overhead_cost": row["overhead_cost"],
                "adjustment_amount": row["adjustment_amount"],
                "notes": row["notes"],
            }, existing_calculations, created, skipped)
        except Exception as error:
            failures.append({"resource": "calculations", "code": row["calculation_code"], "error": str(error)})

    for row in price_rows:
        try:
            create_or_get(client, "price_proposals", "code", row["proposal_code"], {
                "proposal_code": row["proposal_code"],
                "cost_period_id": period_ids[row["period_code"]],
                "stock_item_id": stock_map[row["stock_item_code"]],
                "stock_item_code": row["stock_item_code"],
                "stock_item_name": stock_name_map.get(row["stock_item_code"], row["stock_item_name"]),
                "unit_cost": row["unit_cost"],
                "margin_percent": row["margin_percent"],
                "proposed_price": row["proposed_price"],
                "effective_on": row["effective_on"],
                "currency_code": row["currency_code"],
                "notes": row["notes"],
            }, existing_prices, created, skipped)
        except Exception as error:
            failures.append({"resource": "price_proposals", "code": row["proposal_code"], "error": str(error)})

    inspection_paths = {
        "submitted": ["draft", "submitted"],
        "passed": ["draft", "submitted", "passed"],
        "failed": ["draft", "submitted", "failed"],
        "held": ["draft", "submitted", "held"],
        "released": ["draft", "submitted", "held", "released"],
        "cancelled": ["draft", "cancelled"],
    }
    nonconformance_paths = {
        "in_progress": ["open", "in_progress"],
        "resolved": ["open", "in_progress", "resolved"],
        "cancelled": ["open", "cancelled"],
    }
    period_paths = {
        "open": ["draft", "open"],
        "calculating": ["draft", "open", "calculating"],
        "calculated": ["draft", "open", "calculating", "calculated"],
        "approved": ["draft", "open", "calculating", "calculated", "approved"],
        "locked": ["draft", "open", "calculating", "calculated", "approved", "locked"],
        "cancelled": ["draft", "cancelled"],
    }
    calculation_paths = {
        "approved": ["calculated", "approved"],
        "locked": ["calculated", "approved", "locked"],
        "cancelled": ["calculated", "cancelled"],
    }
    price_paths = {
        "pending": ["draft", "pending"],
        "approved": ["draft", "pending", "approved"],
        "rejected": ["draft", "pending", "rejected"],
        "published": ["draft", "pending", "approved", "published"],
        "cancelled": ["draft", "cancelled"],
    }
    for row in inspection_rows:
        if row["inspection_code"] in inspection_ids:
            advance_status(client, "inspections", inspection_ids[row["inspection_code"]], row.get("target_status", ""), inspection_paths, failures)
    for row in nonconformance_rows:
        if row["nonconformance_code"] in existing_nonconformances:
            advance_status(client, "nonconformances", existing_nonconformances[row["nonconformance_code"]], row.get("target_status", ""), nonconformance_paths, failures)
    for row in period_rows:
        if row["period_code"] in period_ids:
            advance_status(client, "periods", period_ids[row["period_code"]], row.get("target_status", ""), period_paths, failures)
    for row in calculation_rows:
        if row["calculation_code"] in calculation_ids:
            advance_status(client, "calculations", calculation_ids[row["calculation_code"]], row.get("target_status", ""), calculation_paths, failures)
    for row in price_rows:
        if row["proposal_code"] in existing_prices:
            advance_status(client, "price_proposals", existing_prices[row["proposal_code"]], row.get("target_status", ""), price_paths, failures)

    report = {
        "api_base_url": api_base_url,
        "created": len(created),
        "skipped": len(skipped),
        "failures": failures,
        "created_by_resource": {},
    }
    for entry in created:
        report["created_by_resource"][entry["resource"]] = report["created_by_resource"].get(entry["resource"], 0) + 1
    (project_root / "data" / "quality_cost_load_report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
