"""Generate readable ERD images from the project's versioned PostgreSQL migrations.

The generator intentionally reads SQL files only. It does not connect to or mutate a
database, so the diagram remains reproducible from the source of truth in Flyway.
"""

from __future__ import annotations

import argparse
import re
from collections import OrderedDict, defaultdict
from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterable

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.lines import Line2D
from matplotlib.patches import FancyBboxPatch, Rectangle

matplotlib.rcParams['svg.fonttype'] = 'none'


ROOT = Path(__file__).resolve().parents[1]
MIGRATION_ROOT = ROOT / "ERP_Backend" / "src" / "main" / "resources" / "db" / "migration"
SCHEMAS = ("identity", "hr", "inventory", "production")
SCHEMA_COLORS = {
    "identity": "#6d5dfc",
    "hr": "#0f9d8a",
    "inventory": "#e08a1e",
    "production": "#d94f70",
}
SCHEMA_LABELS = {
    "identity": "Nền tảng định danh & phân quyền",
    "hr": "Quản lý nhân sự",
    "inventory": "Quản lý kho & nguyên vật liệu",
    "production": "Quản lý sản xuất",
}


@dataclass
class ForeignKey:
    columns: tuple[str, ...]
    target_schema: str
    target_table: str
    target_columns: tuple[str, ...]
    contract: bool = False
    label: str = ""


@dataclass
class Table:
    schema: str
    name: str
    columns: OrderedDict[str, str] = field(default_factory=OrderedDict)
    primary_key: list[str] = field(default_factory=list)
    foreign_keys: list[ForeignKey] = field(default_factory=list)

    @property
    def key(self) -> str:
        return f"{self.schema}.{self.name}"


def clean_identifier(value: str) -> str:
    return value.strip().strip('"').strip().lower()


def split_columns(values: str) -> tuple[str, ...]:
    return tuple(clean_identifier(item) for item in values.split(",") if item.strip())


def extract_type(rest: str) -> str:
    rest = rest.strip().rstrip(",")
    marker = re.search(
        r"\s+(?:NOT\s+NULL|NULL|DEFAULT|PRIMARY\s+KEY|REFERENCES|UNIQUE|CHECK|COLLATE|GENERATED|CONSTRAINT)\b",
        rest,
        re.IGNORECASE,
    )
    return rest[: marker.start()].strip() if marker else rest


def ensure_table(tables: dict[str, Table], schema: str, name: str) -> Table:
    key = f"{schema}.{name}"
    if key not in tables:
        tables[key] = Table(schema=schema, name=name)
    return tables[key]


def add_fk(table: Table, fk: ForeignKey) -> None:
    signature = (
        fk.columns,
        fk.target_schema,
        fk.target_table,
        fk.target_columns,
        fk.contract,
    )
    if not any(
        (item.columns, item.target_schema, item.target_table, item.target_columns, item.contract)
        == signature
        for item in table.foreign_keys
    ):
        table.foreign_keys.append(fk)


def migration_files() -> Iterable[Path]:
    for schema in SCHEMAS:
        directory = MIGRATION_ROOT / schema
        if not directory.exists():
            continue
        files = sorted(directory.glob("*.sql"), key=lambda item: (item.name.lower()))
        yield from files


def parse_schema() -> dict[str, Table]:
    tables: dict[str, Table] = {}
    sql_files = list(migration_files())

    # First pass: create tables and read their columns, inline keys and table constraints.
    for path in sql_files:
        text = path.read_text(encoding="utf-8")
        for match in re.finditer(
            r"CREATE\s+TABLE\s+(?P<schema>[a-z_][a-z0-9_]*)\.(?P<table>[a-z_][a-z0-9_]*)\s*\((?P<body>.*?)\)\s*;",
            text,
            flags=re.IGNORECASE | re.DOTALL,
        ):
            schema = clean_identifier(match.group("schema"))
            table = ensure_table(tables, schema, clean_identifier(match.group("table")))
            body = match.group("body")
            for line in body.splitlines():
                line = line.strip()
                column_match = re.match(r"([a-z_][a-z0-9_]*)\s+(.+)", line, flags=re.IGNORECASE)
                if not column_match:
                    continue
                column = clean_identifier(column_match.group(1))
                # Constraint lines begin with an uppercase keyword in the migrations.
                if column.upper() in {"PRIMARY", "CONSTRAINT", "UNIQUE", "CHECK", "FOREIGN"}:
                    continue
                rest = column_match.group(2)
                table.columns.setdefault(column, extract_type(rest))
                if re.search(r"\bPRIMARY\s+KEY\b", rest, re.IGNORECASE):
                    if column not in table.primary_key:
                        table.primary_key.append(column)

            for pk_match in re.finditer(r"PRIMARY\s+KEY\s*\(([^)]+)\)", body, flags=re.IGNORECASE):
                for column in split_columns(pk_match.group(1)):
                    if column not in table.primary_key:
                        table.primary_key.append(column)
            for fk_match in re.finditer(
                r"FOREIGN\s+KEY\s*\(([^)]+)\)\s+REFERENCES\s+([a-z_][a-z0-9_]*)\.([a-z_][a-z0-9_]*)\s*\(([^)]+)\)",
                body,
                flags=re.IGNORECASE,
            ):
                add_fk(
                    table,
                    ForeignKey(
                        columns=split_columns(fk_match.group(1)),
                        target_schema=clean_identifier(fk_match.group(2)),
                        target_table=clean_identifier(fk_match.group(3)),
                        target_columns=split_columns(fk_match.group(4)),
                    ),
                )

    # Second pass: apply later ADD COLUMN/ADD CONSTRAINT migration changes.
    for path in sql_files:
        text = path.read_text(encoding="utf-8")
        for match in re.finditer(
            r"ALTER\s+TABLE\s+(?P<schema>[a-z_][a-z0-9_]*)\.(?P<table>[a-z_][a-z0-9_]*).*?ADD\s+COLUMN\s+(?P<column>[a-z_][a-z0-9_]*)\s+(?P<rest>[^;\n]+)",
            text,
            flags=re.IGNORECASE | re.DOTALL,
        ):
            table = ensure_table(tables, clean_identifier(match.group("schema")), clean_identifier(match.group("table")))
            table.columns.setdefault(clean_identifier(match.group("column")), extract_type(match.group("rest")))
        for fk_match in re.finditer(
            r"ALTER\s+TABLE\s+([a-z_][a-z0-9_]*)\.([a-z_][a-z0-9_]*).*?FOREIGN\s+KEY\s*\(([^)]+)\)\s+REFERENCES\s+([a-z_][a-z0-9_]*)\.([a-z_][a-z0-9_]*)\s*\(([^)]+)\)",
            text,
            flags=re.IGNORECASE | re.DOTALL,
        ):
            table = ensure_table(tables, clean_identifier(fk_match.group(1)), clean_identifier(fk_match.group(2)))
            add_fk(
                table,
                ForeignKey(
                    columns=split_columns(fk_match.group(3)),
                    target_schema=clean_identifier(fk_match.group(4)),
                    target_table=clean_identifier(fk_match.group(5)),
                    target_columns=split_columns(fk_match.group(6)),
                ),
            )

    add_contract_references(tables)
    return dict(sorted(tables.items()))


def add_contract_references(tables: dict[str, Table]) -> None:
    """Add documented cross-module references that are intentionally not DB FKs."""

    refs = [
        ("identity.user_account", ("employee_id",), "hr.employee", ("employee_id",), "employee contract"),
        ("production.production_plan_line", ("stock_item_id",), "inventory.stock_item", ("stock_item_id",), "inventory contract"),
        ("production.bom", ("stock_item_id",), "inventory.stock_item", ("stock_item_id",), "inventory contract"),
        ("production.bom_line", ("material_stock_item_id",), "inventory.stock_item", ("stock_item_id",), "inventory contract"),
        ("production.production_order", ("stock_item_id",), "inventory.stock_item", ("stock_item_id",), "inventory contract"),
        ("production.production_order_material_requirement", ("material_stock_item_id",), "inventory.stock_item", ("stock_item_id",), "inventory contract"),
        ("production.production_assignment", ("employee_id",), "hr.employee", ("employee_id",), "hr contract"),
        ("production.production_assignment", ("work_shift_id",), "hr.work_shift", ("work_shift_id",), "hr contract"),
        ("production.material_consumption", ("inventory_issue_line_id",), "inventory.issue_line", ("issue_line_id",), "inventory contract"),
        ("production.production_output", ("inventory_stock_lot_id",), "inventory.stock_lot", ("stock_lot_id",), "inventory contract"),
        ("production.production_output", ("inventory_receipt_id",), "inventory.receipt", ("receipt_id",), "inventory contract"),
    ]
    for source, columns, target, target_columns, label in refs:
        if source not in tables or target not in tables:
            continue
        add_fk(
            tables[source],
            ForeignKey(
                columns=columns,
                target_schema=target.split(".")[0],
                target_table=target.split(".")[1],
                target_columns=target_columns,
                contract=True,
                label=label,
            ),
        )


def key_fields(table: Table, show_all: bool) -> list[str]:
    if show_all:
        return list(table.columns)
    fk_columns = {column for fk in table.foreign_keys for column in fk.columns}
    candidates: list[str] = []
    for column in table.primary_key:
        if column in table.columns and column not in candidates:
            candidates.append(column)
    for column in table.columns:
        if column in fk_columns or column.endswith("_code") or column in {"status", "full_name", "item_name", "department_name", "warehouse_name"}:
            if column not in candidates:
                candidates.append(column)
    for column in table.columns:
        if len(candidates) >= 8:
            break
        if column not in candidates and column not in {"created_at", "updated_at", "created_by_user_id", "updated_by_user_id"}:
            candidates.append(column)
    return candidates[:8]


def field_line(table: Table, column: str) -> str:
    prefix = "PK " if column in table.primary_key else ""
    fk_targets = [fk for fk in table.foreign_keys if column in fk.columns]
    if fk_targets:
        prefix = "FK "
    return f"{prefix}{column}: {table.columns.get(column, 'reference')}"


def place_group(tables: list[Table], left: float, bottom: float, width: float, height: float, columns: int, show_all: bool) -> dict[str, tuple[float, float, float, float]]:
    positions: dict[str, tuple[float, float, float, float]] = {}
    column_width = width / columns
    top = bottom + height - 0.55
    y_values = [top for _ in range(columns)]
    for index, table in enumerate(tables):
        col = index % columns
        display = key_fields(table, show_all)
        box_height = 0.62 + 0.245 * (len(display) + (1 if len(display) < len(table.columns) and not show_all else 0))
        x = left + col * column_width + 0.12
        y = y_values[col] - box_height
        # Keep boxes inside their cluster. The chosen cluster dimensions leave a generous margin.
        positions[table.key] = (x, y, column_width - 0.24, box_height)
        y_values[col] = y - 0.32
    return positions


def draw_table(ax, table: Table, position: tuple[float, float, float, float], show_all: bool) -> None:
    x, y, width, height = position
    color = SCHEMA_COLORS[table.schema]
    patch = FancyBboxPatch(
        (x, y),
        width,
        height,
        boxstyle="round,pad=0.035,rounding_size=0.11",
        linewidth=1.25,
        edgecolor=color,
        facecolor="#ffffff",
        zorder=4,
    )
    ax.add_patch(patch)
    ax.text(x + 0.16, y + height - 0.28, table.name, fontsize=8.0 if show_all else 8.4, fontweight="bold", color="#132238", va="top", zorder=5)
    ax.text(x + width - 0.12, y + height - 0.28, table.schema, fontsize=5.9, color=color, ha="right", va="top", zorder=5)
    fields = key_fields(table, show_all)
    for index, column in enumerate(fields):
        ax.text(x + 0.16, y + height - 0.64 - index * 0.245, field_line(table, column), fontsize=5.7 if show_all else 6.1, color="#334155", va="top", zorder=5)
    if not show_all and len(fields) < len(table.columns):
        hidden = len(table.columns) - len(fields)
        ax.text(x + 0.16, y + 0.14, f"+ {hidden} trường kỹ thuật/nghiệp vụ", fontsize=5.7, color="#64748b", va="bottom", style="italic", zorder=5)


def edge_points(source: tuple[float, float, float, float], target: tuple[float, float, float, float]) -> tuple[tuple[float, float], tuple[float, float]]:
    sx, sy, sw, sh = source
    tx, ty, tw, th = target
    source_center = (sx + sw / 2, sy + sh / 2)
    target_center = (tx + tw / 2, ty + th / 2)
    # Start/end at the nearest horizontal or vertical face to reduce overlap with labels.
    if abs(target_center[0] - source_center[0]) >= abs(target_center[1] - source_center[1]):
        start = (sx + sw if target_center[0] >= source_center[0] else sx, source_center[1])
        end = (tx if target_center[0] >= source_center[0] else tx + tw, target_center[1])
    else:
        start = (source_center[0], sy + sh if target_center[1] >= source_center[1] else sy)
        end = (target_center[0], ty if target_center[1] >= source_center[1] else ty + th)
    return start, end


def render(tables: dict[str, Table], output: Path, show_all: bool) -> None:
    fig_width, fig_height = (30, 56) if show_all else (28, 44)
    fig, ax = plt.subplots(figsize=(fig_width, fig_height), dpi=150)
    ax.set_xlim(0, 28)
    ax.set_ylim(0, 56 if show_all else 44)
    ax.axis("off")
    fig.patch.set_facecolor("#f8fafc")
    ax.set_facecolor("#f8fafc")

    title = "Vinamik — Sơ đồ ERD cơ sở dữ liệu" + (" (đầy đủ trường)" if show_all else " (tổng quan dễ đọc)")
    ax.text(0.25, (55.4 if show_all else 43.4), title, fontsize=20, fontweight="bold", color="#0f172a", va="top")
    ax.text(0.25, (54.55 if show_all else 42.55), "Solid: khóa ngoại PostgreSQL  •  Dashed: hợp đồng tham chiếu giữa module  •  Bốn schema đang triển khai", fontsize=8.5, color="#475569", va="top")

    group_specs = {
        "identity": (0.15, 35.0 if show_all else 27.4, 8.2, 18.3 if show_all else 14.3, 2),
        "hr": (8.65, 35.0 if show_all else 27.4, 19.1, 18.3 if show_all else 14.3, 4),
        "inventory": (0.15, 0.4, 17.8, 33.1 if show_all else 25.8, 4),
        "production": (18.1, 0.4, 9.7, 33.1 if show_all else 25.8, 3),
    }
    positions: dict[str, tuple[float, float, float, float]] = {}
    for schema in SCHEMAS:
        left, bottom, width, height, columns = group_specs[schema]
        group = [item for item in tables.values() if item.schema == schema]
        group.sort(key=lambda item: item.name)
        positions.update(place_group(group, left, bottom, width, height, columns, show_all))
        ax.add_patch(
            Rectangle((left, bottom), width, height, linewidth=1.2, edgecolor=SCHEMA_COLORS[schema], facecolor=SCHEMA_COLORS[schema], alpha=0.045, zorder=0)
        )
        ax.text(left + 0.18, bottom + height - 0.18, SCHEMA_LABELS[schema], fontsize=10.3, fontweight="bold", color=SCHEMA_COLORS[schema], va="top", zorder=2)

    # Draw relationships behind the table boxes. A child points to the referenced parent.
    for table in tables.values():
        if table.key not in positions:
            continue
        for fk in table.foreign_keys:
            target_key = f"{fk.target_schema}.{fk.target_table}"
            if target_key not in positions:
                continue
            start, end = edge_points(positions[table.key], positions[target_key])
            if fk.contract:
                color, linestyle, alpha, width = "#64748b", (0, (4, 3)), 0.55, 0.9
            else:
                color, linestyle, alpha, width = "#334155", "solid", 0.38, 0.75
            ax.annotate(
                "",
                xy=end,
                xytext=start,
                arrowprops={
                    "arrowstyle": "-|>",
                    "color": color,
                    "linestyle": linestyle,
                    "linewidth": width,
                    "alpha": alpha,
                    "shrinkA": 2,
                    "shrinkB": 2,
                    "connectionstyle": "arc3,rad=0.045",
                },
                zorder=1,
            )

    for table in tables.values():
        draw_table(ax, table, positions[table.key], show_all)

    legend = [
        Line2D([0], [0], color="#334155", lw=1.2, label="Khóa ngoại trong PostgreSQL"),
        Line2D([0], [0], color="#64748b", lw=1.2, linestyle=(0, (4, 3)), label="Tham chiếu hợp đồng module"),
        Line2D([0], [0], marker="s", color="w", markerfacecolor="#ffffff", markeredgecolor="#64748b", markersize=8, label="PK/FK hiển thị trong từng bảng"),
    ]
    ax.legend(handles=legend, loc="lower left", bbox_to_anchor=(0.15, -0.006), frameon=False, fontsize=8.2, ncol=3)
    fig.savefig(output, dpi=150, bbox_inches="tight", pad_inches=0.24)
    fig.savefig(output.with_suffix(".svg"), format="svg", bbox_inches="tight", pad_inches=0.24)
    plt.close(fig)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--full", action="store_true", help="show every parsed column instead of key fields")
    parser.add_argument("--output", type=Path, default=ROOT / "docs" / "database_erd_overview.png")
    args = parser.parse_args()
    tables = parse_schema()
    if not tables:
        raise SystemExit("No tables found in Flyway migrations")
    render(tables, args.output, args.full)
    print(f"Generated {len(tables)} tables from migrations")
    for schema in SCHEMAS:
        count = sum(1 for table in tables.values() if table.schema == schema)
        print(f"  {schema}: {count}")


if __name__ == "__main__":
    main()

