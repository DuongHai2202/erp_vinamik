#!/usr/bin/env python3
"""Submit production-like internal account requests through the public API.

Passwords are intentionally accepted only from the environment or generated in
memory for a one-time local fixture load. They are never written to CSV/logs.
The resulting requests stay pending until an ERP administrator verifies the
linked HR employee and assigns the least-privilege role.
"""
from __future__ import annotations

import argparse
import csv
import os
import secrets
import sys
from pathlib import Path

import requests


ROOT = Path(__file__).resolve().parent.parent
CSV_PATH = ROOT / "data" / "identity" / "registration_requests.csv"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Load internal account registration requests through the ERP API.")
    parser.add_argument("--base-url", default=os.getenv("ERP_API_BASE_URL", "http://127.0.0.1:8080"))
    parser.add_argument("--csv", type=Path, default=CSV_PATH)
    parser.add_argument(
        "--password-env",
        default="ERP_IDENTITY_SEED_PASSWORD",
        help="Environment variable containing a temporary password (minimum 15 characters).",
    )
    parser.add_argument(
        "--generate-password",
        action="store_true",
        help="Generate a temporary password in memory for this run; it is not displayed or persisted.",
    )
    return parser.parse_args()


def csrf_session(base_url: str) -> tuple[requests.Session, dict[str, str]]:
    session = requests.Session()
    response = session.get(f"{base_url.rstrip('/')}/api/v1/auth/csrf", timeout=30)
    response.raise_for_status()
    token = session.cookies.get("XSRF-TOKEN")
    if not token:
        raise RuntimeError("The backend did not issue an XSRF-TOKEN cookie.")
    return session, {"X-XSRF-TOKEN": token}


def response_message(response: requests.Response) -> str:
    try:
        body = response.json()
        return str(body.get("message") or body.get("error") or "")
    except ValueError:
        return response.text[:200]


def main() -> int:
    args = parse_args()
    if args.generate_password:
        password = secrets.token_urlsafe(24)
    else:
        password = os.getenv(args.password_env, "")
    if len(password) < 15:
        raise SystemExit(
            f"Set {args.password_env} to a temporary password of at least 15 characters "
            "or pass --generate-password."
        )
    if not args.csv.exists():
        raise SystemExit(f"CSV file does not exist: {args.csv}")

    session, headers = csrf_session(args.base_url)
    accepted = 0
    skipped = 0
    failed = 0
    with args.csv.open("r", encoding="utf-8-sig", newline="") as handle:
        for row in csv.DictReader(handle):
            payload = {
                "full_name": row["full_name"].strip(),
                "work_email": row["work_email"].strip(),
                "employee_code": row["employee_code"].strip() or None,
                "username": row["username"].strip(),
                "password": password,
            }
            response = session.post(
                f"{args.base_url.rstrip('/')}/api/v1/auth/registration-requests",
                json=payload,
                headers=headers,
                timeout=30,
            )
            if response.status_code in (200, 201, 202):
                accepted += 1
                continue
            if response.status_code in (400, 409):
                skipped += 1
                print(f"SKIP {payload['username']}: {response_message(response)}")
                continue
            failed += 1
            print(f"FAIL {payload['username']}: HTTP {response.status_code} {response_message(response)}")

    print(f"Registration requests processed: accepted={accepted}, skipped={skipped}, failed={failed}")
    return 1 if failed else 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except requests.RequestException as exc:
        print(f"API request failed: {exc}", file=sys.stderr)
        raise SystemExit(1) from exc
