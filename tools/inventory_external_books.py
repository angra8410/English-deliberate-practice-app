#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import sys
import unicodedata
from pathlib import Path
from typing import Any


LEVEL_ORDER = ("A1", "A2", "B1", "B2", "C1", "C2")
BOOK_EXTENSIONS = {".pdf", ".epub", ".doc", ".docx"}
AUDIO_EXTENSIONS = {".m4a", ".mp3", ".wav"}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Scan a local external books folder and build an inventory grouped by CEFR level. "
            "This is for staging source material before curating chapters into "
            "tools/content_repository.raw.json."
        )
    )
    parser.add_argument(
        "--source-root",
        type=Path,
        required=True,
        help="Root folder containing level directories such as A1, A2, B1, B2, and C1.",
    )
    parser.add_argument(
        "--format",
        choices=("text", "json"),
        default="text",
        help="Output format.",
    )
    parser.add_argument(
        "--output",
        type=Path,
        help="Optional output file. Prints to stdout when omitted.",
    )
    return parser.parse_args()


def slugify(value: str) -> str:
    normalized = unicodedata.normalize("NFKD", value)
    ascii_only = normalized.encode("ascii", "ignore").decode("ascii")
    cleaned = re.sub(r"[^A-Za-z0-9]+", "-", ascii_only).strip("-").lower()
    return cleaned or "source"


def infer_source_kind(path: Path) -> str | None:
    suffix = path.suffix.lower()
    if suffix in AUDIO_EXTENSIONS:
        return "audio"
    if suffix in BOOK_EXTENSIONS:
        lowered_parts = [part.lower() for part in path.parts]
        if any("worksheet" in part for part in lowered_parts):
            return "worksheet"
        return "book"
    return None


def clean_display_title(path: Path) -> str:
    stem = path.stem.replace("_", " ")
    stem = re.sub(r"\{[^{}]+\}", "", stem)
    stem = re.sub(r"\[[^\[\]]+\]\s*", "", stem)
    stem = re.sub(r"\s+", " ", stem).strip(" -._")
    return stem or path.name


def build_entry(level: str, path: Path, source_root: Path) -> dict[str, Any]:
    kind = infer_source_kind(path)
    if kind is None:
        raise ValueError(f"Unsupported source file: {path}")
    relative_path = path.relative_to(source_root)
    return {
        "id": f"{level.lower()}-{kind}-{slugify(str(relative_path.with_suffix('')))}",
        "level": level,
        "sourceKind": kind,
        "displayTitle": clean_display_title(path),
        "fileName": path.name,
        "relativePath": relative_path.as_posix(),
        "extension": path.suffix.lower(),
    }


def scan_level(level_root: Path, source_root: Path) -> dict[str, Any]:
    level = level_root.name.upper()
    entries = [
        build_entry(level, path, source_root)
        for path in sorted(level_root.rglob("*"))
        if path.is_file() and infer_source_kind(path) is not None
    ]
    return {
        "level": level,
        "counts": {
            "total": len(entries),
            "books": sum(1 for entry in entries if entry["sourceKind"] == "book"),
            "worksheets": sum(1 for entry in entries if entry["sourceKind"] == "worksheet"),
            "audio": sum(1 for entry in entries if entry["sourceKind"] == "audio"),
        },
        "entries": entries,
    }


def build_inventory(source_root: Path) -> dict[str, Any]:
    if not source_root.exists():
        raise SystemExit(f"Source root does not exist: {source_root}")
    if not source_root.is_dir():
        raise SystemExit(f"Source root is not a directory: {source_root}")

    levels = []
    for level_name in LEVEL_ORDER:
        level_root = source_root / level_name
        if level_root.is_dir():
            levels.append(scan_level(level_root, source_root))

    return {
        "sourceRoot": str(source_root),
        "levelCount": len(levels),
        "totalSources": sum(level["counts"]["total"] for level in levels),
        "levels": levels,
    }


def render_text(inventory: dict[str, Any]) -> str:
    lines = [
        f"Source root: {inventory['sourceRoot']}",
        f"Levels discovered: {inventory['levelCount']}",
        f"Total source files: {inventory['totalSources']}",
        "",
    ]
    for level in inventory["levels"]:
        counts = level["counts"]
        lines.append(
            f"{level['level']}: {counts['total']} files "
            f"({counts['books']} books, {counts['worksheets']} worksheets, {counts['audio']} audio)"
        )
        for entry in level["entries"]:
            lines.append(
                f"- [{entry['sourceKind']}] {entry['displayTitle']} "
                f"({entry['relativePath']})"
            )
        lines.append("")
    return "\n".join(lines).rstrip() + "\n"


def main() -> None:
    args = parse_args()
    inventory = build_inventory(args.source_root)
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    if args.format == "json":
        rendered = json.dumps(inventory, indent=2, ensure_ascii=False) + "\n"
    else:
        rendered = render_text(inventory)

    if args.output:
        args.output.write_text(rendered, encoding="utf-8")
        print(f"Wrote inventory to {args.output}")
        return

    print(rendered, end="")


if __name__ == "__main__":
    main()
