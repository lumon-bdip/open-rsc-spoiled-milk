#!/usr/bin/env python3
import json
from collections import Counter
from pathlib import Path
from typing import Any, cast

from generator_common import (
    ROOT,
    fail,
    finalize_generation,
    parse_generator_args,
    render_payload,
)

DEFAULT_SOURCE_DIR = ROOT / "tools" / "generators" / "item-overrides"
DEFAULT_TARGET_PATH = (
    ROOT / "server" / "conf" / "server" / "defs" / "ItemDefsMyWorld.json"
)
CLIENT_OVERRIDE_TARGET_PATH = (
    ROOT
    / "Client_Base"
    / "src"
    / "com"
    / "openrsc"
    / "client"
    / "entityhandling"
    / "MyWorldItemOverrides.java"
)
ALLOWED_TOP_LEVEL_KEYS = {"items", "description"}
ALLOWED_ITEM_FIELDS = {
    "id",
    "name",
    "description",
    "meleeOffense",
    "rangedOffense",
    "magicOffense",
    "weaponSpeed",
    "meleeDefense",
    "rangedDefense",
    "magicDefense",
    "requiredLevel",
    "requiredSkillID",
    "isWearable",
    "appearanceID",
    "wearableID",
    "wearSlot",
    "prayerBonus",
    "weaponAimBonus",
    "weaponPowerBonus",
    "armourBonus",
    "magicBonus",
    "basePrice",
}


def validate_source_file(data: object, source_path: Path) -> list[dict[str, Any]]:
    if not isinstance(data, dict):
        fail(f"{source_path.name} must contain a top-level JSON object")

    typed_data = cast(dict[str, Any], data)

    unknown_top_level = sorted(set(typed_data.keys()) - ALLOWED_TOP_LEVEL_KEYS)
    if unknown_top_level:
        fail(
            f"{source_path.name} has unknown top-level keys: {', '.join(unknown_top_level)}"
        )

    items = typed_data.get("items")
    if not isinstance(items, list):
        fail(f"{source_path.name} must contain a top-level 'items' array")

    return cast(list[dict[str, Any]], items)


def validate_item_entry(entry: object, source_path: Path) -> dict[str, Any]:
    if not isinstance(entry, dict):
        fail(f"{source_path.name} contains a non-object entry: {entry!r}")

    typed_entry = cast(dict[str, Any], entry)

    item_id = typed_entry.get("id")
    if not isinstance(item_id, int):
        fail(f"{source_path.name} contains an entry without an integer id: {entry!r}")

    unknown_fields = sorted(set(typed_entry.keys()) - ALLOWED_ITEM_FIELDS)
    if unknown_fields:
        fail(
            f"{source_path.name} entry {item_id} has unknown fields: "
            f"{', '.join(unknown_fields)}"
        )

    if len(typed_entry) == 1:
        fail(
            f"{source_path.name} entry {item_id} must override at least one field beyond id"
        )

    for field, value in typed_entry.items():
        if field == "id":
            continue
        if field == "name":
            if not isinstance(value, str):
                fail(f"{source_path.name} entry {item_id} field {field} must be a string")
            if not value.strip():
                fail(f"{source_path.name} entry {item_id} field {field} must not be empty")
            continue
        if field == "description":
            if not isinstance(value, str):
                fail(f"{source_path.name} entry {item_id} field {field} must be a string")
            continue
        if not isinstance(value, (int, float)):
            fail(f"{source_path.name} entry {item_id} field {field} must be numeric")

    return typed_entry


def describe_item_entry(entry: dict[str, Any]) -> str:
    categories = []
    if any(
        field in entry
        for field in (
            "meleeOffense",
            "weaponSpeed",
            "requiredLevel",
            "requiredSkillID",
            "isWearable",
            "appearanceID",
            "wearableID",
            "wearSlot",
            "prayerBonus",
            "weaponAimBonus",
            "weaponPowerBonus",
        )
    ):
        categories.append("melee")
    if "name" in entry:
        categories.append("identity")
    if "description" in entry:
        categories.append("identity")
    if "rangedOffense" in entry:
        categories.append("ranged")
    if "magicOffense" in entry:
        categories.append("magic")
    if any(
        field in entry for field in ("meleeDefense", "rangedDefense", "magicDefense")
    ):
        categories.append("defense")
    if "basePrice" in entry:
        categories.append("economy")
    if not categories:
        categories.append("other")
    return "/".join(categories)


def load_source_items(source_dir: Path) -> tuple[list[dict[str, Any]], list[str]]:
    if not source_dir.exists():
        fail(f"Missing source directory: {source_dir}")

    source_paths = sorted(source_dir.glob("*.json"))
    if not source_paths:
        fail(f"No source files found in {source_dir}")

    merged_items: list[dict[str, Any]] = []
    seen_ids: dict[int, Path] = {}
    summaries: list[str] = []

    for source_path in source_paths:
        with source_path.open("r", encoding="utf-8") as handle:
            data = json.load(handle)

        items = validate_source_file(data, source_path)
        category_counts: Counter[str] = Counter()

        for entry in items:
            validated_entry = validate_item_entry(entry, source_path)
            item_id = validated_entry["id"]
            if item_id in seen_ids:
                fail(
                    f"Duplicate item id {item_id} in {source_path.name} and {seen_ids[item_id].name}"
                )
            seen_ids[item_id] = source_path
            merged_items.append(validated_entry)
            category_counts[describe_item_entry(validated_entry)] += 1

        category_summary = ", ".join(
            f"{category}={count}" for category, count in sorted(category_counts.items())
        )
        summaries.append(f"{source_path.name}: {len(items)} items [{category_summary}]")

    return merged_items, summaries


def java_string(value: str | None) -> str:
    if value is None:
        return "null"
    escaped = (
        value
        .replace("\\", "\\\\")
        .replace('"', '\\"')
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
    )
    return f'"{escaped}"'


def render_client_overrides(items: list[dict[str, Any]]) -> str:
    client_fields = (
        "name",
        "description",
        "basePrice",
        "isWearable",
        "wearableID",
    )
    client_items = [
        entry for entry in items if any(field in entry for field in client_fields)
    ]
    lines = [
        "package com.openrsc.client.entityhandling;",
        "",
        "import com.openrsc.client.entityhandling.defs.ItemDef;",
        "",
        "import java.util.List;",
        "",
        "// Generated by tools/generators/generate-item-overrides.py.",
        "final class MyWorldItemOverrides {",
        "\tprivate static final ItemOverride[] OVERRIDES = {",
    ]
    for entry in client_items:
        lines.append(
            "\t\tnew ItemOverride("
            + ", ".join(
                [
                    str(int(entry["id"])),
                    java_string(entry.get("name")),
                    java_string(entry.get("description")),
                    str(int(entry["basePrice"])) if "basePrice" in entry else "-1",
                    str(int(entry["isWearable"])) if "isWearable" in entry else "-1",
                    str(int(entry["wearableID"])) if "wearableID" in entry else "-1",
                ]
            )
            + "),"
        )
    lines.extend(
        [
        "\t};",
            "",
            "\tprivate MyWorldItemOverrides() {",
            "\t}",
            "",
            "\tstatic void apply(List<ItemDef> items) {",
            "\t\tfor (ItemOverride override : OVERRIDES) {",
            "\t\t\tapplyOverride(items, override);",
            "\t\t}",
            "\t}",
            "",
            "\tprivate static void applyOverride(List<ItemDef> items, ItemOverride override) {",
            "\t\tint itemId = override.itemId;",
            "\t\tif (itemId < 0 || itemId >= items.size()) {",
            "\t\t\treturn;",
            "\t\t}",
            "\t\tItemDef item = items.get(itemId);",
            "\t\tif (item == null || item.id != itemId || \"Unobtanium\".equals(item.getName())) {",
            "\t\t\treturn;",
            "\t\t}",
            "\t\tif (override.name != null) {",
            "\t\t\titem.name = override.name;",
            "\t\t}",
            "\t\tif (override.description != null) {",
            "\t\t\titem.description = override.description;",
            "\t\t}",
            "\t\tif (override.basePrice >= 0) {",
            "\t\t\titem.basePrice = override.basePrice;",
            "\t\t}",
            "\t\tif (override.isWearable >= 0) {",
            "\t\t\titem.wieldable = override.isWearable != 0;",
            "\t\t}",
            "\t\tif (override.wearableId >= 0) {",
            "\t\t\titem.wearableID = override.wearableId;",
            "\t\t}",
            "\t}",
            "",
            "\tprivate static final class ItemOverride {",
            "\t\tprivate final int itemId;",
            "\t\tprivate final String name;",
            "\t\tprivate final String description;",
            "\t\tprivate final int basePrice;",
            "\t\tprivate final int isWearable;",
            "\t\tprivate final int wearableId;",
            "",
            "\t\tprivate ItemOverride(int itemId, String name, String description, int basePrice, int isWearable, int wearableId) {",
            "\t\t\tthis.itemId = itemId;",
            "\t\t\tthis.name = name;",
            "\t\t\tthis.description = description;",
            "\t\t\tthis.basePrice = basePrice;",
            "\t\t\tthis.isWearable = isWearable;",
            "\t\t\tthis.wearableId = wearableId;",
            "\t\t}",
            "\t}",
            "}",
            "",
        ]
    )
    return "\n".join(lines)


def finalize_client_overrides(check: bool, rendered: str) -> None:
    command = "python3 ./tools/generators/run-generators.py --only item"
    if check:
        if not CLIENT_OVERRIDE_TARGET_PATH.exists():
            fail(f"Missing generated file: {CLIENT_OVERRIDE_TARGET_PATH}; run {command}")
        if CLIENT_OVERRIDE_TARGET_PATH.read_text(encoding="utf-8") != rendered:
            fail(f"{CLIENT_OVERRIDE_TARGET_PATH} is out of date; run {command}")
        print(f"Validated {CLIENT_OVERRIDE_TARGET_PATH}")
    else:
        temporary_path = CLIENT_OVERRIDE_TARGET_PATH.with_suffix(".java.tmp")
        temporary_path.write_text(rendered, encoding="utf-8")
        temporary_path.replace(CLIENT_OVERRIDE_TARGET_PATH)
        print(f"Generated {CLIENT_OVERRIDE_TARGET_PATH}")


def main() -> None:
    args = parse_generator_args(
        "Build or validate MyWorld item override definitions.",
        DEFAULT_SOURCE_DIR,
        DEFAULT_TARGET_PATH,
    )
    source_dir = args.source_dir.resolve()
    target_path = args.target.resolve()
    merged_items, summaries = load_source_items(source_dir)
    payload = {"items": merged_items}
    rendered = render_payload(payload)
    rendered_client_overrides = render_client_overrides(merged_items)

    finalize_generation(
        check=args.check,
        source_dir=source_dir,
        target_path=target_path,
        rendered_payload=rendered,
        entry_count=len(merged_items),
        entry_label="items",
        generator_command="python3 ./tools/generators/run-generators.py --only item",
        summaries=summaries,
    )
    if (
        source_dir == DEFAULT_SOURCE_DIR.resolve()
        and target_path == DEFAULT_TARGET_PATH.resolve()
    ):
        finalize_client_overrides(args.check, rendered_client_overrides)


if __name__ == "__main__":
    main()
