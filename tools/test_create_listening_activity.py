from __future__ import annotations

import importlib.util
import json
import shutil
import sys
import unittest
from argparse import Namespace
from pathlib import Path


TOOLS_DIR = Path(__file__).resolve().parent
SCRIPT_PATH = TOOLS_DIR / "create_listening_activity.py"
MODULE_SPEC = importlib.util.spec_from_file_location(
    "create_listening_activity",
    SCRIPT_PATH,
)
authoring_helper = importlib.util.module_from_spec(MODULE_SPEC)
assert MODULE_SPEC.loader is not None
sys.modules[MODULE_SPEC.name] = authoring_helper
MODULE_SPEC.loader.exec_module(authoring_helper)


class CreateListeningActivityTest(unittest.TestCase):
    def test_build_activity_uses_level_defaults(self) -> None:
        args = Namespace(
            level="C1",
            title="Vendor handover call",
            prompt="Listen and summarize the final agreement.",
            id=None,
            unit_id="c1_listening_dense_input",
            audio_asset=None,
            listening_prompt_text=None,
            instructions=None,
            starter_text=None,
            support_note=None,
            sample_answer=None,
            evaluation_targets=["agreement", "deadline"],
            tags=["handover"],
            difficulty=None,
            minimum_word_count=None,
            minimum_keyword_matches=None,
            requires_contrast_marker=False,
        )

        activity = authoring_helper.build_activity(args)

        self.assertEqual("c1-listen-vendor-handover-call", activity["id"])
        self.assertEqual("audio/c1_listen_vendor_handover_call.wav", activity["audioAsset"])
        self.assertEqual("c1_listening_dense_input", activity["unitId"])
        self.assertEqual("LISTENING", activity["skill"])
        self.assertEqual("LISTEN_AND_SUMMARIZE", activity["exerciseType"])
        self.assertEqual(4, activity["difficulty"])
        self.assertEqual("The speaker argues that...", activity["starterText"])
        self.assertEqual(["listening", "summary", "handover"], activity["tags"])
        self.assertNotIn("listeningPromptText", activity)

    def test_load_listening_unit_id_reads_level_unit_asset(self) -> None:
        temp_path = TOOLS_DIR / "create_listening_activity_test_workspace"
        if temp_path.exists():
            shutil.rmtree(temp_path, ignore_errors=True)
        temp_path.mkdir(parents=True, exist_ok=True)
        try:
            (temp_path / "units_b2.json").write_text(
                json.dumps(
                    [
                        {"id": "b2_reading", "skill": "READING"},
                        {"id": "b2_listening", "skill": "LISTENING"},
                    ]
                ),
                encoding="utf-8",
            )

            unit_id = authoring_helper.load_listening_unit_id("B2", temp_path)

            self.assertEqual("b2_listening", unit_id)
        finally:
            shutil.rmtree(temp_path, ignore_errors=True)

    def test_append_activity_rejects_duplicate_id_and_audio(self) -> None:
        temp_path = TOOLS_DIR / "create_listening_activity_test_workspace"
        if temp_path.exists():
            shutil.rmtree(temp_path, ignore_errors=True)
        temp_path.mkdir(parents=True, exist_ok=True)
        try:
            activities_path = temp_path / "activities_b2.json"
            activities_path.write_text(
                json.dumps(
                    [
                        {
                            "id": "b2-listen-existing",
                            "audioAsset": "audio/b2_listen_existing.wav",
                        }
                    ]
                ),
                encoding="utf-8",
            )

            with self.assertRaises(SystemExit):
                authoring_helper.append_activity(
                    activities_path,
                    {
                        "id": "b2-listen-existing",
                        "audioAsset": "audio/b2_listen_new.wav",
                    },
                )

            with self.assertRaises(SystemExit):
                authoring_helper.append_activity(
                    activities_path,
                    {
                        "id": "b2-listen-new",
                        "audioAsset": "audio/b2_listen_existing.wav",
                    },
                )
        finally:
            shutil.rmtree(temp_path, ignore_errors=True)

    def test_append_activity_writes_new_listening_item(self) -> None:
        temp_path = TOOLS_DIR / "create_listening_activity_test_workspace"
        if temp_path.exists():
            shutil.rmtree(temp_path, ignore_errors=True)
        temp_path.mkdir(parents=True, exist_ok=True)
        try:
            activities_path = temp_path / "activities_c1.json"
            activities_path.write_text(json.dumps([]), encoding="utf-8")

            authoring_helper.append_activity(
                activities_path,
                {
                    "id": "c1-listen-supply-update",
                    "audioAsset": "audio/c1_listen_supply_update.wav",
                    "title": "Supply update",
                },
            )

            payload = json.loads(activities_path.read_text(encoding="utf-8"))
            self.assertEqual(1, len(payload))
            self.assertEqual("c1-listen-supply-update", payload[0]["id"])
        finally:
            shutil.rmtree(temp_path, ignore_errors=True)


if __name__ == "__main__":
    unittest.main()
