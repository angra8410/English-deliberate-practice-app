from __future__ import annotations

import importlib.util
import json
import shutil
import subprocess
import sys
import unittest
from pathlib import Path


TOOLS_DIR = Path(__file__).resolve().parent
SCRIPT_PATH = TOOLS_DIR / "generate_content_repository.py"
MODULE_SPEC = importlib.util.spec_from_file_location(
    "generate_content_repository",
    SCRIPT_PATH,
)
generator = importlib.util.module_from_spec(MODULE_SPEC)
assert MODULE_SPEC.loader is not None
MODULE_SPEC.loader.exec_module(generator)


class GenerateContentRepositoryTest(unittest.TestCase):
    def test_merge_prompt_metadata_infers_list_fields_and_applies_overrides(self) -> None:
        prompt = {
            "id": "vocab-prompt-1",
            "type": "open_text",
            "targetSkill": "VOCABULARY",
            "prompt": "List five phrases commonly used in client update emails.",
        }
        chapter = {
            "title": "Client updates",
            "tags": ["vocabulary", "email", "clients"],
            "points": [
                "Polite follow-up messages should confirm the next step clearly.",
                "Professional update emails often mention revised deadlines and client approval.",
            ],
            "examples": [
                {"english": "I am writing to confirm the revised delivery schedule."},
                {"english": "Please let me know if you need any further clarification."},
            ],
            "summary": "Vocabulary for professional email updates.",
        }
        book = {"title": "Business English Source"}
        override = {
            "instructions": "List five realistic expressions used in update emails.",
            "minimumWordCount": 12,
        }

        enriched = generator.merge_prompt_metadata(prompt, chapter, book, override)

        self.assertEqual("list", enriched["scoringProfile"])
        self.assertEqual("List five realistic expressions used in update emails.", enriched["instructions"])
        self.assertEqual(12, enriched["minimumWordCount"])
        self.assertEqual(5, enriched["minimumResponseItems"])
        self.assertIn("I am writing to confirm the revised delivery schedule.", enriched["modelAnswer"])
        self.assertIn("vocabulary", enriched["expectedKeywords"])

    def test_prompt_fields_take_precedence_over_overrides(self) -> None:
        prompt = {
            "id": "grammar-prompt-1",
            "type": "open_text",
            "targetSkill": "WRITING",
            "prompt": "Rewrite three sentences using inversion.",
            "instructions": "Use inversion after limiting expressions.",
            "modelAnswer": "Rarely have I seen such a rapid recovery.",
            "scoringProfile": "rewrite",
        }
        chapter = {
            "title": "Inversion",
            "tags": ["grammar", "inversion"],
            "points": ["Use inversion after negative adverbials."],
            "examples": [{"english": "Only then did the managers agree."}],
            "summary": "Formal inversion for emphasis.",
        }
        book = {"title": "Advanced Grammar"}
        override = {
            "instructions": "Override should not replace prompt instructions.",
            "modelAnswer": "Override model answer",
            "scoringProfile": "sentence_drill",
            "minimumResponseItems": 6,
        }

        enriched = generator.merge_prompt_metadata(prompt, chapter, book, override)

        self.assertEqual("Use inversion after limiting expressions.", enriched["instructions"])
        self.assertEqual("Rarely have I seen such a rapid recovery.", enriched["modelAnswer"])
        self.assertEqual("rewrite", enriched["scoringProfile"])
        self.assertEqual(6, enriched["minimumResponseItems"])

    def test_enrich_catalog_only_updates_matching_prompts(self) -> None:
        catalog = {
            "version": 2,
            "generatedAt": "2026-04-02T00:00:00+00:00",
            "books": [
                {
                    "id": "sample-book",
                    "title": "Sample Book",
                    "author": "A. Author",
                    "cefr": ["C1"],
                    "sourceType": "curated_notes",
                    "tags": ["grammar"],
                    "chapters": [
                        {
                            "id": "chapter-1",
                            "title": "Chapter 1",
                            "order": 1,
                            "cefr": ["C1"],
                            "tags": ["grammar"],
                            "summary": "Summary",
                            "points": ["Point one."],
                            "examples": [{"english": "Example sentence."}],
                            "pitfalls": [],
                            "practicePrompts": [
                                {
                                    "id": "prompt-1",
                                    "type": "open_text",
                                    "targetSkill": "WRITING",
                                    "prompt": "Write three examples.",
                                },
                                {
                                    "id": "prompt-2",
                                    "type": "open_text",
                                    "targetSkill": "WRITING",
                                    "prompt": "Write one reflection paragraph.",
                                },
                            ],
                            "related": [],
                            "metadata": {},
                        }
                    ],
                }
            ],
        }
        overrides = {
            "prompts": {
                "prompt-1": {
                    "instructions": "Only the first prompt should be overridden.",
                    "minimumResponseItems": 3,
                }
            }
        }

        enriched = generator.enrich_catalog(catalog, overrides)
        prompts = enriched["books"][0]["chapters"][0]["practicePrompts"]

        self.assertEqual("Only the first prompt should be overridden.", prompts[0]["instructions"])
        self.assertEqual(3, prompts[0]["minimumResponseItems"])
        self.assertNotIn("minimumResponseItems", prompts[1])
        self.assertNotEqual(
            prompts[0]["instructions"],
            prompts[1]["instructions"],
        )

    def test_check_mode_detects_drift(self) -> None:
        minimal_catalog = {
            "version": 2,
            "generatedAt": "2026-04-02T00:00:00+00:00",
            "books": [
                {
                    "id": "sample-book",
                    "title": "Sample Book",
                    "author": "A. Author",
                    "cefr": ["C1"],
                    "sourceType": "curated_notes",
                    "tags": ["grammar"],
                    "chapters": [
                        {
                            "id": "chapter-1",
                            "title": "Chapter 1",
                            "order": 1,
                            "cefr": ["C1"],
                            "tags": ["grammar"],
                            "summary": "Summary",
                            "points": ["Point one."],
                            "examples": [{"english": "Example sentence."}],
                            "pitfalls": [],
                            "practicePrompts": [
                                {
                                    "id": "prompt-1",
                                    "type": "open_text",
                                    "targetSkill": "WRITING",
                                    "prompt": "Write three examples.",
                                }
                            ],
                            "related": [],
                            "metadata": {},
                        }
                    ],
                }
            ],
        }

        temp_path = TOOLS_DIR / "generator_test_workspace"
        if temp_path.exists():
            shutil.rmtree(temp_path, ignore_errors=True)
        temp_path.mkdir(parents=True, exist_ok=True)
        try:
            input_path = temp_path / "raw.json"
            output_path = temp_path / "generated.json"
            overrides_path = temp_path / "overrides.json"

            input_path.write_text(json.dumps(minimal_catalog), encoding="utf-8")
            overrides_path.write_text(json.dumps({"prompts": {}}), encoding="utf-8")

            write_result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT_PATH),
                    "--input",
                    str(input_path),
                    "--output",
                    str(output_path),
                    "--overrides",
                    str(overrides_path),
                ],
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertEqual(0, write_result.returncode, write_result.stderr)

            output_path.write_text("{}", encoding="utf-8")
            check_result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT_PATH),
                    "--input",
                    str(input_path),
                    "--output",
                    str(output_path),
                    "--overrides",
                    str(overrides_path),
                    "--check",
                ],
                capture_output=True,
                text=True,
                check=False,
            )

            self.assertNotEqual(0, check_result.returncode)
            self.assertIn("out of date", check_result.stderr + check_result.stdout)
        finally:
            shutil.rmtree(temp_path, ignore_errors=True)


if __name__ == "__main__":
    unittest.main()
