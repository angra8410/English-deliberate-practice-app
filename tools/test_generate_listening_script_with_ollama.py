from __future__ import annotations

import importlib.util
import json
import shutil
import sys
import unittest
from pathlib import Path


TOOLS_DIR = Path(__file__).resolve().parent
SCRIPT_PATH = TOOLS_DIR / "generate_listening_script_with_ollama.py"
MODULE_SPEC = importlib.util.spec_from_file_location(
    "generate_listening_script_with_ollama",
    SCRIPT_PATH,
)
script_helper = importlib.util.module_from_spec(MODULE_SPEC)
assert MODULE_SPEC.loader is not None
sys.modules[MODULE_SPEC.name] = script_helper
MODULE_SPEC.loader.exec_module(script_helper)


class GenerateListeningScriptWithOllamaTest(unittest.TestCase):
    def test_collect_candidates_finds_builtin_listening_item(self) -> None:
        temp_path = TOOLS_DIR / "listening_script_test_workspace"
        if temp_path.exists():
            shutil.rmtree(temp_path, ignore_errors=True)
        temp_path.mkdir(parents=True, exist_ok=True)
        try:
            content_dir = temp_path
            (content_dir / "activities.json").write_text(
                json.dumps(
                    [
                        {
                            "id": "listen-1",
                            "title": "Listening item",
                            "prompt": "Listen and summarize.",
                            "instructions": "Summarize the main point.",
                            "audioAsset": "audio/listen-1.wav",
                            "listeningPromptText": "A short spoken script.",
                            "evaluationTargets": ["main point"],
                            "sampleAnswer": "The speaker argues for hybrid work.",
                        }
                    ]
                ),
                encoding="utf-8",
            )

            candidates = script_helper.collect_candidates(content_dir)

            self.assertEqual(1, len(candidates))
            self.assertEqual("listen-1", candidates[0].id)
            self.assertEqual("audio/listen-1.wav", candidates[0].audio_asset)
            self.assertEqual("A short spoken script.", candidates[0].listening_prompt_text)
        finally:
            shutil.rmtree(temp_path, ignore_errors=True)

    def test_build_prompt_includes_activity_context(self) -> None:
        candidate = script_helper.ListeningCandidate(
            id="listen-1",
            title="Listening item",
            prompt="Listen and summarize.",
            instructions="Summarize the final position.",
            audio_asset="audio/listen-1.wav",
            listening_prompt_text="Current script.",
            support_note="Mention the contrast clearly.",
            evaluation_targets=["contrast", "position"],
            sample_answer="The speaker prefers a hybrid model.",
            source_path=Path("app/src/main/assets/content/activities_b2.json"),
        )

        prompt = script_helper.build_ollama_prompt(candidate, style="natural", word_count=65)

        self.assertIn("Activity id: listen-1", prompt)
        self.assertIn("Current listeningPromptText: Current script.", prompt)
        self.assertIn("Evaluation targets: contrast, position", prompt)
        self.assertIn("around 65 words", prompt)

    def test_apply_script_updates_builtin_asset_file(self) -> None:
        temp_path = TOOLS_DIR / "listening_script_test_workspace"
        if temp_path.exists():
            shutil.rmtree(temp_path, ignore_errors=True)
        temp_path.mkdir(parents=True, exist_ok=True)
        try:
            source_path = temp_path / "activities.json"
            source_path.write_text(
                json.dumps(
                    [
                        {
                            "id": "listen-1",
                            "audioAsset": "audio/listen-1.wav",
                            "prompt": "Listen and summarize.",
                        }
                    ]
                ),
                encoding="utf-8",
            )

            candidate = script_helper.ListeningCandidate(
                id="listen-1",
                title=None,
                prompt="Listen and summarize.",
                instructions=None,
                audio_asset="audio/listen-1.wav",
                listening_prompt_text=None,
                support_note=None,
                evaluation_targets=[],
                sample_answer=None,
                source_path=source_path,
            )

            message = script_helper.apply_script(
                candidate,
                "A revised listening script.",
                overrides_path=temp_path / "overrides.json",
            )

            updated = json.loads(source_path.read_text(encoding="utf-8"))
            self.assertEqual("A revised listening script.", updated[0]["listeningPromptText"])
            self.assertIn("Updated", message)
        finally:
            shutil.rmtree(temp_path, ignore_errors=True)

    def test_apply_script_updates_overrides_for_generated_catalog_prompt(self) -> None:
        temp_path = TOOLS_DIR / "listening_script_test_workspace"
        if temp_path.exists():
            shutil.rmtree(temp_path, ignore_errors=True)
        temp_path.mkdir(parents=True, exist_ok=True)
        try:
            overrides_path = temp_path / "content_metadata_overrides.json"
            overrides_path.write_text(json.dumps({"prompts": {}}), encoding="utf-8")

            candidate = script_helper.ListeningCandidate(
                id="generated-listen-1",
                title=None,
                prompt="Listen and summarize.",
                instructions=None,
                audio_asset="audio/generated.wav",
                listening_prompt_text=None,
                support_note=None,
                evaluation_targets=[],
                sample_answer=None,
                source_path=Path("app/src/main/assets/content/content_repository.json"),
            )

            message = script_helper.apply_script(
                candidate,
                "Generated override script.",
                overrides_path=overrides_path,
            )

            updated = json.loads(overrides_path.read_text(encoding="utf-8"))
            self.assertEqual(
                "Generated override script.",
                updated["prompts"]["generated-listen-1"]["listeningPromptText"],
            )
            self.assertIn("Run the content generator again", message)
        finally:
            shutil.rmtree(temp_path, ignore_errors=True)


if __name__ == "__main__":
    unittest.main()
