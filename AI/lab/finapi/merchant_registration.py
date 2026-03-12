from __future__ import annotations

import csv
import secrets
from datetime import datetime
from pathlib import Path
from typing import Iterable, Sequence

import httpx

CREATE_MERCHANT_URL = "https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/createMerchant"
DEFAULT_CATEGORY_ID = "CG-9ca85f66311a23d"
SUCCESS_FIELDS = ("merchantId", "merchantName")
FAILURE_FIELDS = ("merchantName", "responseCode", "responseMessage", "errorType")


def discover_merchant_csv_paths(input_path: str | Path) -> list[Path]:
    path = Path(input_path)
    if path.is_file():
        return [path]
    if not path.exists():
        raise FileNotFoundError(f"Input path does not exist: {path}")
    if not path.is_dir():
        raise ValueError(f"Input path must be a CSV file or directory: {path}")

    return sorted(
        [csv_path for csv_path in path.glob("*.csv") if "상가(상권)정보" in csv_path.name],
        key=lambda csv_path: csv_path.name,
    )


def load_merchant_names(
    csv_paths: Sequence[str | Path],
    merchant_name_column: str = "상호명",
    encoding: str = "utf-8-sig",
) -> list[str]:
    seen: set[str] = set()
    merchant_names: list[str] = []

    for csv_path in csv_paths:
        path = Path(csv_path)
        with path.open("r", encoding=encoding, newline="") as file:
            reader = csv.DictReader(file)
            if reader.fieldnames is None or merchant_name_column not in reader.fieldnames:
                raise KeyError(f"Column '{merchant_name_column}' not found in {path}")

            for row in reader:
                merchant_name = (row.get(merchant_name_column) or "").strip()
                if merchant_name and merchant_name not in seen:
                    seen.add(merchant_name)
                    merchant_names.append(merchant_name)

    return merchant_names


def build_create_merchant_header(
    api_key: str,
    institution_code: str = "00100",
    fintech_app_no: str = "001",
    now: datetime | None = None,
) -> dict[str, str]:
    current = now or datetime.now()
    transmission_date = current.strftime("%Y%m%d")
    transmission_time = current.strftime("%H%M%S")
    unique_no = f"{current.strftime('%Y%m%d%H%M%S')}{secrets.randbelow(1_000_000):06d}"

    return {
        "apiName": "createMerchant",
        "transmissionDate": transmission_date,
        "transmissionTime": transmission_time,
        "institutionCode": institution_code,
        "fintechAppNo": fintech_app_no,
        "apiServiceCode": "createMerchant",
        "institutionTransactionUniqueNo": unique_no,
        "apiKey": api_key,
    }


def build_create_merchant_payload(
    merchant_name: str,
    api_key: str,
    category_id: str = DEFAULT_CATEGORY_ID,
    institution_code: str = "00100",
    fintech_app_no: str = "001",
) -> dict[str, object]:
    return {
        "Header": build_create_merchant_header(
            api_key=api_key,
            institution_code=institution_code,
            fintech_app_no=fintech_app_no,
        ),
        "categoryId": category_id,
        "merchantName": merchant_name,
    }


def select_matching_record(records: Iterable[dict[str, object]], merchant_name: str) -> dict[str, object]:
    record_list = list(records)
    matches = [
        record
        for record in record_list
        if str(record.get("merchantName", "")).strip() == merchant_name
    ]
    if matches:
        return matches[-1]
    if len(record_list) == 1:
        return record_list[0]
    raise ValueError(f"Unable to find matching merchant record for '{merchant_name}'")


def register_merchants(
    merchant_names: Sequence[str],
    api_key: str,
    client: httpx.Client | None = None,
    *,
    category_id: str = DEFAULT_CATEGORY_ID,
    base_url: str = CREATE_MERCHANT_URL,
    institution_code: str = "00100",
    fintech_app_no: str = "001",
    timeout: float = 30.0,
) -> tuple[list[dict[str, str]], list[dict[str, str]]]:
    success_rows: list[dict[str, str]] = []
    failure_rows: list[dict[str, str]] = []
    owns_client = client is None
    http_client = client or httpx.Client()

    try:
        for merchant_name in merchant_names:
            payload = build_create_merchant_payload(
                merchant_name=merchant_name,
                api_key=api_key,
                category_id=category_id,
                institution_code=institution_code,
                fintech_app_no=fintech_app_no,
            )
            try:
                response = http_client.post(base_url, json=payload, timeout=timeout)
                response.raise_for_status()
                response_json = response.json()
            except httpx.HTTPError as exc:
                failure_rows.append(
                    {
                        "merchantName": merchant_name,
                        "responseCode": str(getattr(getattr(exc, "response", None), "status_code", "")),
                        "responseMessage": str(exc),
                        "errorType": "http_error",
                    }
                )
                continue
            except ValueError as exc:
                failure_rows.append(
                    {
                        "merchantName": merchant_name,
                        "responseCode": "",
                        "responseMessage": str(exc),
                        "errorType": "json_error",
                    }
                )
                continue

            header = response_json.get("Header", {})
            response_code = str(header.get("responseCode", ""))
            response_message = str(header.get("responseMessage", ""))
            if response_code != "H0000":
                failure_rows.append(
                    {
                        "merchantName": merchant_name,
                        "responseCode": response_code,
                        "responseMessage": response_message,
                        "errorType": "api_error",
                    }
                )
                continue

            records = response_json.get("REC", [])
            try:
                record = select_matching_record(records, merchant_name)
                success_rows.append(
                    {
                        "merchantId": str(record["merchantId"]),
                        "merchantName": str(record["merchantName"]).strip(),
                    }
                )
            except (KeyError, TypeError, ValueError) as exc:
                failure_rows.append(
                    {
                        "merchantName": merchant_name,
                        "responseCode": response_code,
                        "responseMessage": str(exc),
                        "errorType": "response_error",
                    }
                )
    finally:
        if owns_client:
            http_client.close()

    return success_rows, failure_rows


def write_success_rows(output_path: str | Path, rows: Sequence[dict[str, str]]) -> None:
    _write_rows(output_path, SUCCESS_FIELDS, rows)


def write_failure_rows(output_path: str | Path, rows: Sequence[dict[str, str]]) -> None:
    _write_rows(output_path, FAILURE_FIELDS, rows)


def _write_rows(
    output_path: str | Path,
    fieldnames: Sequence[str],
    rows: Sequence[dict[str, str]],
) -> None:
    path = Path(output_path)
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8-sig", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=fieldnames)
        writer.writeheader()
        for row in rows:
            writer.writerow({field: row.get(field, "") for field in fieldnames})

