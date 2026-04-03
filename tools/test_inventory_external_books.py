from __future__ import annotations

import importlib.util
import shutil
import unittest
from pathlib import Path


TOOLS_DIR = Path(__file__).resolve().parent
SCRIPT_PATH = TOOLS_DIR / "inventory_external_books.py"
MODULE_SPEC = importlib.util.spec_from_file_location(
    "inventory_external_books",
    SCRIPT_PATH,
)
inventory_module = importlib.util.module_from_spec(MODULE_SPEC)
assert MODULE_SPEC.loader is not None
MODULE_SPEC.loader.exec_module(inventory_module)


class InventoryExternalBooksTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = TOOLS_DIR / "tmp_inventory_external_books"
        if self.temp_dir.exists():
            shutil.rmtree(self.temp_dir, ignore_errors=True)
        self.temp_dir.mkdir(parents=True, exist_ok=True)

    def tearDown(self) -> None:
        shutil.rmtree(self.temp_dir, ignore_errors=True)

    def test_build_inventory_groups_sources_by_level_and_kind(self) -> None:
        (self.temp_dir / "A1" / "books").mkdir(parents=True)
        (self.temp_dir / "A1" / "audio").mkdir(parents=True)
        (self.temp_dir / "B2").mkdir(parents=True)

        (self.temp_dir / "A1" / "books" / "Oxford English File Elementary.pdf").write_text(
            "placeholder",
            encoding="utf-8",
        )
        (self.temp_dir / "A1" / "books" / "BBC_Interview_Worksheets" / "U01.pdf").parent.mkdir()
        (self.temp_dir / "A1" / "books" / "BBC_Interview_Worksheets" / "U01.pdf").write_text(
            "placeholder",
            encoding="utf-8",
        )
        (self.temp_dir / "A1" / "audio" / "lesson1.m4a").write_text("placeholder", encoding="utf-8")
        (self.temp_dir / "B2" / "English Vocabulary in Use Upper-Intermediate.pdf").write_text(
            "placeholder",
            encoding="utf-8",
        )
        (self.temp_dir / "B2" / "notes.txt").write_text("ignore me", encoding="utf-8")

        inventory = inventory_module.build_inventory(self.temp_dir)

        self.assertEqual(2, inventory["levelCount"])
        self.assertEqual(4, inventory["totalSources"])

        a1 = next(level for level in inventory["levels"] if level["level"] == "A1")
        self.assertEqual(3, a1["counts"]["total"])
        self.assertEqual(1, a1["counts"]["books"])
        self.assertEqual(1, a1["counts"]["worksheets"])
        self.assertEqual(1, a1["counts"]["audio"])

        b2 = next(level for level in inventory["levels"] if level["level"] == "B2")
        self.assertEqual(1, b2["counts"]["books"])
        self.assertEqual(0, b2["counts"]["audio"])

    def test_render_text_includes_counts_and_relative_paths(self) -> None:
        inventory = {
            "sourceRoot": str(self.temp_dir),
            "levelCount": 1,
            "totalSources": 1,
            "levels": [
                {
                    "level": "C1",
                    "counts": {"total": 1, "books": 1, "worksheets": 0, "audio": 0},
                    "entries": [
                        {
                            "sourceKind": "book",
                            "displayTitle": "Advanced Grammar in Use",
                            "relativePath": "C1/advanced-grammar.pdf",
                        }
                    ],
                }
            ],
        }

        rendered = inventory_module.render_text(inventory)

        self.assertIn("C1: 1 files (1 books, 0 worksheets, 0 audio)", rendered)
        self.assertIn("Advanced Grammar in Use", rendered)
        self.assertIn("C1/advanced-grammar.pdf", rendered)


if __name__ == "__main__":
    unittest.main()
