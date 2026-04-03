#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from dataclasses import dataclass
from pathlib import Path
from typing import Any
from urllib import error, request


DEFAULT_CONFIG_PATH = Path("tools/local_audio_pipeline.config.json")
DEFAULT_CONTENT_DIR = Path("app/src/main/assets/content")
DEFAULT_OVERRIDES_PATH = Path("tools/content_metadata_overrides.json")
DEFAULT_ASSETS_ROOT = Path("app/src/main/assets")


@dataclass
class ListeningCandidate:
    id: str
    title: str | None
    prompt: str | None
    instructions: str | None
    audio_asset: str
    listening_prompt_text: str | None
    support_note: str | None
    evaluation_targets: list[str]
    sample_answer: str | None
    source_path: Path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Generate or improve a listeningPromptText script for a listening activity "
            "using a local Ollama model."
        )
    )
    target_group = parser.add_mutually_exclusive_group(required=True)
    target_group.add_argument("--prompt-id", help="Single listening prompt id to target.")
    target_group.add_argument(
        "--prompt-ids",
        nargs="+",
        help="One or more listening prompt ids to target in bulk.",
    )
    target_group.add_argument(
        "--all-candidates",
        action="store_true",
        help="Target every listening candidate discovered in the content JSON files.",
    )
    parser.add_argument(
        "--content-dir",
        type=Path,
        default=DEFAULT_CONTENT_DIR,
        help="Directory containing the content JSON files.",
    )
    parser.add_argument(
        "--config",
        type=Path,
        default=DEFAULT_CONFIG_PATH,
        help="Optional local pipeline config JSON with ollama defaults.",
    )
    parser.add_argument(
        "--overrides",
        type=Path,
        default=DEFAULT_OVERRIDES_PATH,
        help="Prompt override JSON used when applying generated scripts for book-catalog prompts.",
    )
    parser.add_argument(
        "--assets-root",
        type=Path,
        default=DEFAULT_ASSETS_ROOT,
        help="Root directory used to check whether bundled audio files already exist.",
    )
    parser.add_argument(
        "--model",
        help="Local Ollama model to use. Defaults to config value or gemma3:27b.",
    )
    parser.add_argument(
        "--host",
        help="Ollama host URL. Defaults to config value or http://127.0.0.1:11434.",
    )
    parser.add_argument(
        "--style",
        choices=("natural", "exam", "dialogue"),
        default="natural",
        help="Target script style for the generated listening text.",
    )
    parser.add_argument(
        "--word-count",
        type=int,
        default=70,
        help="Approximate target length for the spoken script.",
    )
    parser.add_argument(
        "--missing-script-only",
        action="store_true",
        help="Only target listening items that do not have listeningPromptText yet.",
    )
    parser.add_argument(
        "--missing-audio-only",
        action="store_true",
        help="Only target listening items whose bundled audio file is still missing.",
    )
    parser.add_argument(
        "--limit",
        type=int,
        help="Optional maximum number of prompts to process after filtering.",
    )
    parser.add_argument(
        "--apply",
        action="store_true",
        help="Write the generated listeningPromptText back to the correct source JSON.",
    )
    parser.add_argument(
        "--print-prompt",
        action="store_true",
        help="Print the Ollama prompt payload instead of calling the model.",
    )
    parser.add_argument(
        "--list-candidates",
        action="store_true",
        help="List the filtered candidates and exit without calling Ollama.",
    )
    parser.add_argument(
        "--list-ids",
        action="store_true",
        help="Print only the filtered prompt ids, one per line, and exit.",
    )
    return parser.parse_args()


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def load_optional_json(path: Path) -> dict[str, Any]:
    if not path.exists():
        return {}
    return load_json(path)


def config_value(config: dict[str, Any], key: str) -> Any:
    return config.get(key)


def collect_candidates(content_dir: Path) -> list[ListeningCandidate]:
    candidates: list[ListeningCandidate] = []
    for json_path in sorted(content_dir.glob("*.json")):
        payload = load_json(json_path)
        walk_candidates(payload, json_path, candidates)
    return candidates


def walk_candidates(node: Any, source_path: Path, candidates: list[ListeningCandidate]) -> None:
    if isinstance(node, list):
        for item in node:
            walk_candidates(item, source_path, candidates)
        return

    if not isinstance(node, dict):
        return

    if isinstance(node.get("audioAsset"), str) and node["audioAsset"].strip():
        candidates.append(
            ListeningCandidate(
                id=str(node.get("id", "")).strip(),
                title=clean_optional_string(node.get("title")),
                prompt=clean_optional_string(node.get("prompt")),
                instructions=clean_optional_string(node.get("instructions")),
                audio_asset=node["audioAsset"].strip(),
                listening_prompt_text=clean_optional_string(node.get("listeningPromptText")),
                support_note=clean_optional_string(node.get("supportNote")),
                evaluation_targets=[
                    str(item).strip()
                    for item in node.get("evaluationTargets", [])
                    if str(item).strip()
                ],
                sample_answer=clean_optional_string(node.get("sampleAnswer") or node.get("modelAnswer")),
                source_path=source_path,
            )
        )

    for value in node.values():
        walk_candidates(value, source_path, candidates)


def clean_optional_string(value: Any) -> str | None:
    if isinstance(value, str) and value.strip():
        return value.strip()
    return None


def build_ollama_prompt(candidate: ListeningCandidate, style: str, word_count: int) -> str:
    style_instruction = {
        "natural": "Write a short natural spoken monologue for listening practice.",
        "exam": "Write a clear exam-style listening script with natural pacing and explicit contrast.",
        "dialogue": "Write a short spoken exchange between two people for listening practice.",
    }[style]

    current_script = candidate.listening_prompt_text or "None yet."
    evaluation_targets = ", ".join(candidate.evaluation_targets) if candidate.evaluation_targets else "None specified."
    sample_answer = candidate.sample_answer or "None provided."
    instructions = candidate.instructions or "None provided."
    prompt_text = candidate.prompt or "None provided."
    support_note = candidate.support_note or "None provided."

    return f"""You are writing local listening-practice audio scripts for a personal English study app.

Task:
- {style_instruction}
- Keep the script around {word_count} words.
- Use clean spoken English that sounds natural when read aloud by a TTS engine.
- Prefer shorter sentences, clear punctuation, and audible discourse markers.
- Preserve the meaning of the listening task.
- Do not add labels, bullet points, quotation marks, speaker names, or commentary.
- Return only the final spoken script text.

Activity id: {candidate.id}
Title: {candidate.title or "None provided."}
Prompt shown to learner: {prompt_text}
Instructions: {instructions}
Current listeningPromptText: {current_script}
Support note: {support_note}
Evaluation targets: {evaluation_targets}
Sample answer: {sample_answer}
Audio asset path: {candidate.audio_asset}
"""


def call_ollama(host: str, model: str, prompt: str) -> str:
    payload = {
        "model": model,
        "prompt": prompt,
        "stream": False,
        "options": {
            "temperature": 0.3,
        },
    }
    req = request.Request(
        url=f"{host.rstrip('/')}/api/generate",
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        with request.urlopen(req, timeout=180) as response:
            raw = response.read().decode("utf-8")
    except error.URLError as exc:
        raise SystemExit(f"Could not reach Ollama at {host}: {exc}") from exc

    data = json.loads(raw)
    generated = data.get("response", "")
    if not isinstance(generated, str) or not generated.strip():
        raise SystemExit("Ollama returned an empty response.")
    return sanitize_script(generated)


def sanitize_script(text: str) -> str:
    cleaned = text.strip()
    if cleaned.startswith("```"):
        cleaned = cleaned.strip("`").strip()
        if "\n" in cleaned:
            cleaned = cleaned.split("\n", 1)[1].strip()
    cleaned = cleaned.strip().strip('"').strip("'").strip()
    cleaned = normalize_ascii_punctuation(cleaned)
    return " ".join(cleaned.split())


def normalize_ascii_punctuation(text: str) -> str:
    return (
        text.replace("\u2019", "'")
        .replace("\u2018", "'")
        .replace("\u201c", '"')
        .replace("\u201d", '"')
        .replace("\u2013", "-")
        .replace("\u2014", "-")
        .replace("\u2026", "...")
    )


def audio_output_path(candidate: ListeningCandidate, assets_root: Path) -> Path:
    return assets_root / Path(candidate.audio_asset.replace("/", "/"))


def audio_exists(candidate: ListeningCandidate, assets_root: Path) -> bool:
    return audio_output_path(candidate, assets_root).exists()


def select_candidates(
    candidates: list[ListeningCandidate],
    *,
    prompt_id: str | None,
    prompt_ids: list[str] | None,
    all_candidates: bool,
    missing_script_only: bool,
    missing_audio_only: bool,
    assets_root: Path,
    limit: int | None,
) -> list[ListeningCandidate]:
    if prompt_id:
        selected = [candidate for candidate in candidates if candidate.id == prompt_id]
    elif prompt_ids:
        wanted_ids = set(prompt_ids)
        selected = [candidate for candidate in candidates if candidate.id in wanted_ids]
    elif all_candidates:
        selected = list(candidates)
    else:
        selected = []

    if missing_script_only:
        selected = [candidate for candidate in selected if not candidate.listening_prompt_text]
    if missing_audio_only:
        selected = [candidate for candidate in selected if not audio_exists(candidate, assets_root)]
    if limit is not None:
        selected = selected[:limit]

    return selected


def print_candidates(candidates: list[ListeningCandidate], assets_root: Path) -> None:
    if not candidates:
        print("No candidates matched the current filters.")
        return

    header = f"{'Id':<36} {'HasScript':<10} {'AudioExists':<11} SourcePath"
    print(header)
    print("-" * len(header))
    for candidate in candidates:
        has_script = "yes" if candidate.listening_prompt_text else "no"
        has_audio = "yes" if audio_exists(candidate, assets_root) else "no"
        print(f"{candidate.id:<36} {has_script:<10} {has_audio:<11} {candidate.source_path}")


def print_candidate_ids(candidates: list[ListeningCandidate]) -> None:
    for candidate in candidates:
        print(candidate.id)


def apply_script(
    candidate: ListeningCandidate,
    script_text: str,
    overrides_path: Path,
) -> str:
    if candidate.source_path.name == "content_repository.json":
        overrides = load_optional_json(overrides_path)
        prompts = overrides.setdefault("prompts", {})
        prompt_override = prompts.setdefault(candidate.id, {})
        prompt_override["listeningPromptText"] = script_text
        overrides_path.write_text(
            json.dumps(overrides, indent=2, ensure_ascii=False) + "\n",
            encoding="utf-8",
        )
        return (
            f"Updated {overrides_path} for prompt '{candidate.id}'. "
            "Run the content generator again to refresh app/src/main/assets/content/content_repository.json."
        )

    data = load_json(candidate.source_path)
    if not update_item_by_id(data, candidate.id, {"listeningPromptText": script_text}):
        raise SystemExit(f"Could not find prompt '{candidate.id}' inside {candidate.source_path}")
    candidate.source_path.write_text(
        json.dumps(data, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    return f"Updated {candidate.source_path} for prompt '{candidate.id}'."


def update_item_by_id(node: Any, prompt_id: str, patch: dict[str, Any]) -> bool:
    if isinstance(node, list):
        return any(update_item_by_id(item, prompt_id, patch) for item in node)
    if not isinstance(node, dict):
        return False
    if node.get("id") == prompt_id:
        node.update(patch)
        return True
    return any(update_item_by_id(value, prompt_id, patch) for value in node.values())


def main() -> None:
    args = parse_args()
    config = load_optional_json(args.config)
    host = args.host or config_value(config, "ollamaHost") or "http://127.0.0.1:11434"
    model = args.model or config_value(config, "ollamaModel") or "gemma3:27b"

    candidates = collect_candidates(args.content_dir)
    selected_candidates = select_candidates(
        candidates,
        prompt_id=args.prompt_id,
        prompt_ids=args.prompt_ids,
        all_candidates=args.all_candidates,
        missing_script_only=args.missing_script_only,
        missing_audio_only=args.missing_audio_only,
        assets_root=args.assets_root,
        limit=args.limit,
    )
    if not selected_candidates:
        raise SystemExit("No listening candidates matched the current filters.")

    if args.list_candidates:
        print_candidates(selected_candidates, args.assets_root)
        return

    if args.list_ids:
        print_candidate_ids(selected_candidates)
        return

    if args.print_prompt and len(selected_candidates) != 1:
        raise SystemExit("--print-prompt only works when exactly one prompt is selected.")

    for candidate in selected_candidates:
        prompt = build_ollama_prompt(candidate, args.style, args.word_count)
        if args.print_prompt:
            print(prompt)
            return

        script = call_ollama(host=host, model=model, prompt=prompt)
        print(f"[{candidate.id}] {script}")

        if args.apply:
            message = apply_script(candidate, script, args.overrides)
            print(message)


if __name__ == "__main__":
    main()
