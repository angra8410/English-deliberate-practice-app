#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import unicodedata
from pathlib import Path
from typing import Any


DEFAULT_CONTENT_DIR = Path("app/src/main/assets/content")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Scaffold a new built-in listening activity in activities_b2.json or "
            "activities_c1.json for the local Ollama + Piper workflow."
        )
    )
    parser.add_argument("--level", required=True, choices=("B2", "C1"))
    parser.add_argument("--title", required=True, help="User-facing listening activity title.")
    parser.add_argument("--prompt", required=True, help="Prompt shown to the learner.")
    parser.add_argument("--id", help="Optional activity id. Defaults to a slug derived from the level and title.")
    parser.add_argument("--unit-id", help="Optional unit id. Defaults to the level's existing listening unit.")
    parser.add_argument(
        "--audio-asset",
        help="Optional repo-relative audio asset path. Defaults to audio/<derived_name>.wav",
    )
    parser.add_argument(
        "--listening-prompt-text",
        help="Optional spoken script. Leave blank if you want Ollama to generate it later.",
    )
    parser.add_argument(
        "--instructions",
        help="Optional learner instructions. Defaults to a listening-summary instruction for the level.",
    )
    parser.add_argument(
        "--starter-text",
        help="Optional starter text. Defaults to a level-appropriate summary opener.",
    )
    parser.add_argument(
        "--support-note",
        help="Optional support note. Defaults to local workflow guidance for script and audio generation.",
    )
    parser.add_argument(
        "--sample-answer",
        help="Optional sample answer. Leave blank if you want to author it later.",
    )
    parser.add_argument(
        "--evaluation-targets",
        nargs="*",
        default=[],
        help="Optional keyword targets for evaluation.",
    )
    parser.add_argument(
        "--tags",
        nargs="*",
        default=[],
        help="Optional extra tags. The helper always adds listening and summary.",
    )
    parser.add_argument(
        "--difficulty",
        type=int,
        choices=(1, 2, 3, 4),
        help="Optional difficulty override. Defaults to 3 for B2 and 4 for C1.",
    )
    parser.add_argument(
        "--minimum-word-count",
        type=int,
        help="Optional minimum word count for the learner response.",
    )
    parser.add_argument(
        "--minimum-keyword-matches",
        type=int,
        help="Optional keyword-match requirement for evaluation.",
    )
    parser.add_argument(
        "--requires-contrast-marker",
        action="store_true",
        help="Explicitly require a contrast marker in the learner summary.",
    )
    parser.add_argument(
        "--content-dir",
        type=Path,
        default=DEFAULT_CONTENT_DIR,
        help="Directory containing activities_b2.json and activities_c1.json.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print the new activity JSON and next commands without writing the file.",
    )
    return parser.parse_args()


def slugify(value: str, separator: str = "-") -> str:
    normalized = unicodedata.normalize("NFKD", value)
    ascii_only = normalized.encode("ascii", "ignore").decode("ascii")
    cleaned = re.sub(r"[^A-Za-z0-9]+", separator, ascii_only).strip(separator)
    lowered = cleaned.lower()
    return lowered or "listening-item"


def content_file_for_level(level: str, content_dir: Path) -> Path:
    return content_dir / f"activities_{level.lower()}.json"


def units_file_for_level(level: str, content_dir: Path) -> Path:
    return content_dir / f"units_{level.lower()}.json"


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def load_listening_unit_id(level: str, content_dir: Path) -> str:
    units_path = units_file_for_level(level, content_dir)
    units = load_json(units_path)
    listening_unit = next((unit for unit in units if unit.get("skill") == "LISTENING"), None)
    if listening_unit is None or not isinstance(listening_unit.get("id"), str):
        raise SystemExit(f"Could not find a LISTENING unit inside {units_path}")
    return listening_unit["id"]


def default_instructions(level: str) -> str:
    if level == "C1":
        return "Write 80 to 100 words summarizing the speaker's main claim and key supporting detail."
    return "Write a short summary of the speaker's conclusion and one contrast they mention."


def default_starter_text(level: str) -> str:
    if level == "C1":
        return "The speaker argues that..."
    return "The speaker ultimately argues that..."


def default_support_note() -> str:
    return (
        "Use the local authoring workflow: generate listeningPromptText with Ollama if needed, "
        "then synthesize the bundled .wav with Piper."
    )


def default_difficulty(level: str) -> int:
    return 4 if level == "C1" else 3


def default_id(level: str, title: str) -> str:
    return f"{level.lower()}-listen-{slugify(title)}"


def default_audio_asset(activity_id: str) -> str:
    file_slug = slugify(activity_id, separator="_")
    return f"audio/{file_slug}.wav"


def build_activity(args: argparse.Namespace) -> dict[str, Any]:
    activity_id = args.id or default_id(args.level, args.title)
    audio_asset = args.audio_asset or default_audio_asset(activity_id)
    tags = list(dict.fromkeys(["listening", "summary", *args.tags]))

    activity: dict[str, Any] = {
        "id": activity_id,
        "unitId": args.unit_id,
        "title": args.title,
        "level": args.level,
        "skill": "LISTENING",
        "exerciseType": "LISTEN_AND_SUMMARIZE",
        "prompt": args.prompt,
        "instructions": args.instructions or default_instructions(args.level),
        "tags": tags,
        "difficulty": args.difficulty or default_difficulty(args.level),
        "starterText": args.starter_text or default_starter_text(args.level),
        "audioAsset": audio_asset,
        "supportNote": args.support_note or default_support_note(),
        "evaluationTargets": args.evaluation_targets,
    }

    if args.listening_prompt_text:
        activity["listeningPromptText"] = args.listening_prompt_text
    if args.sample_answer:
        activity["sampleAnswer"] = args.sample_answer
    if args.minimum_word_count:
        activity["minimumWordCount"] = args.minimum_word_count
    if args.minimum_keyword_matches:
        activity["minimumKeywordMatches"] = args.minimum_keyword_matches
    if args.requires_contrast_marker:
        activity["requiresContrastMarker"] = True

    return activity


def validate_new_activity(existing: list[dict[str, Any]], new_activity: dict[str, Any]) -> None:
    duplicate_id = next((item for item in existing if item.get("id") == new_activity["id"]), None)
    if duplicate_id is not None:
        raise SystemExit(f"Activity id '{new_activity['id']}' already exists.")

    duplicate_audio = next(
        (item for item in existing if item.get("audioAsset") == new_activity["audioAsset"]),
        None,
    )
    if duplicate_audio is not None:
        raise SystemExit(
            "Audio asset path already exists on another activity: "
            f"{new_activity['audioAsset']} (activity id '{duplicate_audio.get('id')}')."
        )


def append_activity(path: Path, new_activity: dict[str, Any]) -> None:
    payload = load_json(path)
    if not isinstance(payload, list):
        raise SystemExit(f"Expected {path} to contain a top-level JSON array.")
    validate_new_activity(payload, new_activity)
    payload.append(new_activity)
    path.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def next_commands(activity: dict[str, Any]) -> list[str]:
    activity_id = activity["id"]
    commands = []
    if "listeningPromptText" not in activity:
        commands.append(
            f"python tools/generate_listening_script_with_ollama.py --prompt-id {activity_id} --apply"
        )
    commands.append(
        "powershell -ExecutionPolicy Bypass -File tools/generate_listening_audio.ps1 "
        f"-Engine piper -PromptIds {activity_id} -Overwrite"
    )
    return commands


def main() -> None:
    args = parse_args()
    target_path = content_file_for_level(args.level, args.content_dir)
    if not target_path.exists():
        raise SystemExit(f"Could not find target content file: {target_path}")

    if not args.unit_id:
        args.unit_id = load_listening_unit_id(args.level, args.content_dir)

    activity = build_activity(args)

    if args.dry_run:
        print(json.dumps(activity, indent=2, ensure_ascii=False))
        print()
        print("Next commands:")
        for command in next_commands(activity):
            print(command)
        return

    append_activity(target_path, activity)
    print(f"Added listening activity '{activity['id']}' to {target_path}")
    print("Next commands:")
    for command in next_commands(activity):
        print(command)


if __name__ == "__main__":
    main()
