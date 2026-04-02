#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
from copy import deepcopy
from pathlib import Path
from typing import Any


NUMBER_WORDS = {
    "one": 1,
    "two": 2,
    "three": 3,
    "four": 4,
    "five": 5,
    "six": 6,
    "seven": 7,
    "eight": 8,
    "nine": 9,
    "ten": 10,
}

DEFAULT_INSTRUCTIONS = {
    "open_text": "Write a clear, focused response using the target language from this chapter.",
    "fill_in_blank": "Complete each item with the most natural word or phrase.",
    "multiple_choice": "Choose the best answer and pay attention to meaning and usage.",
    "speak_response": "Answer aloud and use the chapter language as naturally as possible.",
    "listen_and_summarize": "Listen carefully and summarize the main point with one supporting detail.",
    "read_and_summarize": "Read the prompt and summarize the key idea accurately.",
    "error_correction": "Rewrite the text with the errors corrected.",
    "sentence_transformation": "Rewrite the sentence while preserving the original meaning.",
}

DEFAULT_MINIMUM_WORD_COUNT = {
    "list": 10,
    "sentence_drill": 18,
    "rewrite": 20,
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Generate an enriched content repository asset from a raw book catalog JSON "
            "and optional prompt-level metadata overrides."
        )
    )
    parser.add_argument(
        "--input",
        type=Path,
        default=Path("tools/content_repository.raw.json"),
        help="Path to the raw book catalog JSON.",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("app/src/main/assets/content/content_repository.json"),
        help="Path to write the enriched content repository JSON.",
    )
    parser.add_argument(
        "--overrides",
        type=Path,
        default=Path("tools/content_metadata_overrides.json"),
        help="Optional prompt override file. Missing files are ignored.",
    )
    parser.add_argument(
        "--check",
        action="store_true",
        help="Fail if the generated output differs from the current output file instead of writing it.",
    )
    return parser.parse_args()


def load_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def load_overrides(path: Path) -> dict[str, Any]:
    if not path.exists():
        return {}
    return load_json(path)


def merge_prompt_metadata(
    prompt: dict[str, Any],
    chapter: dict[str, Any],
    book: dict[str, Any],
    override: dict[str, Any],
) -> dict[str, Any]:
    enriched = deepcopy(prompt)
    scoring_profile_override = enriched.get("scoringProfile") or override.get("scoringProfile")
    scoring_profile = scoring_profile_override or infer_scoring_profile(prompt)

    enriched["instructions"] = first_non_blank(
        enriched.get("instructions"),
        override.get("instructions"),
        infer_instructions(prompt, chapter),
    )
    starter_text = first_non_blank(
        enriched.get("starterText"),
        override.get("starterText"),
        infer_starter_text(chapter, scoring_profile),
    )
    if starter_text:
        enriched["starterText"] = starter_text

    audio_asset = first_non_blank(
        enriched.get("audioAsset"),
        override.get("audioAsset"),
    )
    if audio_asset:
        enriched["audioAsset"] = audio_asset

    enriched["modelAnswer"] = first_non_blank(
        enriched.get("modelAnswer"),
        override.get("modelAnswer"),
        infer_model_answer(prompt, chapter, book, scoring_profile),
    )

    expected_keywords = dedupe_strings(
        [
            *enriched.get("expectedKeywords", []),
            *override.get("expectedKeywords", []),
        ]
    ) or infer_expected_keywords(prompt, chapter)
    if expected_keywords:
        enriched["expectedKeywords"] = expected_keywords

    if scoring_profile_override or scoring_profile != "default":
        enriched["scoringProfile"] = scoring_profile
    else:
        enriched.pop("scoringProfile", None)

    minimum_word_count = (
        positive_int(enriched.get("minimumWordCount"))
        or positive_int(override.get("minimumWordCount"))
        or infer_minimum_word_count(prompt, scoring_profile)
    )
    if minimum_word_count is not None:
        enriched["minimumWordCount"] = minimum_word_count

    minimum_response_items = (
        positive_int(enriched.get("minimumResponseItems"))
        or positive_int(override.get("minimumResponseItems"))
        or infer_minimum_response_items(prompt, scoring_profile)
    )
    if minimum_response_items is not None:
        enriched["minimumResponseItems"] = minimum_response_items

    minimum_keyword_matches = (
        positive_int(enriched.get("minimumKeywordMatches"))
        or positive_int(override.get("minimumKeywordMatches"))
        or infer_minimum_keyword_matches(prompt)
    )
    if minimum_keyword_matches is not None:
        enriched["minimumKeywordMatches"] = minimum_keyword_matches

    requires_tone_reference = (
        explicit_boolean(enriched.get("requiresToneReference"))
        if "requiresToneReference" in enriched
        else explicit_boolean(override.get("requiresToneReference"))
    )
    if requires_tone_reference is None:
        requires_tone_reference = infer_tone_requirement(prompt)
    if requires_tone_reference is not None:
        enriched["requiresToneReference"] = requires_tone_reference

    requires_contrast_marker = (
        explicit_boolean(enriched.get("requiresContrastMarker"))
        if "requiresContrastMarker" in enriched
        else explicit_boolean(override.get("requiresContrastMarker"))
    )
    if requires_contrast_marker is None:
        requires_contrast_marker = infer_contrast_requirement(prompt)
    if requires_contrast_marker is not None:
        enriched["requiresContrastMarker"] = requires_contrast_marker

    return order_prompt_fields(enriched)


def enrich_catalog(catalog: dict[str, Any], overrides: dict[str, Any]) -> dict[str, Any]:
    prompt_overrides = overrides.get("prompts", {})
    enriched_catalog = deepcopy(catalog)
    for book in enriched_catalog.get("books", []):
        for chapter in book.get("chapters", []):
            chapter["practicePrompts"] = [
                merge_prompt_metadata(
                    prompt=prompt,
                    chapter=chapter,
                    book=book,
                    override=prompt_overrides.get(prompt.get("id", ""), {}),
                )
                for prompt in chapter.get("practicePrompts", [])
            ]
    return enriched_catalog


def order_prompt_fields(prompt: dict[str, Any]) -> dict[str, Any]:
    preferred_order = [
        "id",
        "type",
        "targetSkill",
        "prompt",
        "instructions",
        "starterText",
        "audioAsset",
        "modelAnswer",
        "expectedKeywords",
        "scoringProfile",
        "minimumWordCount",
        "minimumResponseItems",
        "minimumKeywordMatches",
        "requiresToneReference",
        "requiresContrastMarker",
    ]
    ordered_prompt: dict[str, Any] = {}
    for key in preferred_order:
        if key in prompt:
            ordered_prompt[key] = prompt[key]
    for key, value in prompt.items():
        if key not in ordered_prompt:
            ordered_prompt[key] = value
    return ordered_prompt


def infer_instructions(prompt: dict[str, Any], chapter: dict[str, Any]) -> str:
    prompt_text = prompt.get("prompt", "")
    scoring_profile = infer_scoring_profile(prompt)
    if scoring_profile == "list":
        return (
            "List the required expressions using authentic language from this chapter "
            "instead of generic business vocabulary."
        )
    if scoring_profile == "sentence_drill":
        return (
            "Write full sentence pairs that clearly show the target contrast or pattern "
            "from this chapter."
        )
    if scoring_profile == "rewrite":
        return (
            "Rewrite each item using the more natural form and keep the target grammar "
            "or vocabulary accurate."
        )
    return DEFAULT_INSTRUCTIONS.get(prompt.get("type", "").lower(), DEFAULT_INSTRUCTIONS["open_text"])


def infer_starter_text(chapter: dict[str, Any], scoring_profile: str) -> str | None:
    if scoring_profile == "list":
        return None
    examples = chapter.get("examples", [])
    if examples:
        return examples[0].get("english")
    points = chapter.get("points", [])
    if points:
        return points[0]
    return None


def infer_model_answer(
    prompt: dict[str, Any],
    chapter: dict[str, Any],
    book: dict[str, Any],
    scoring_profile: str,
) -> str:
    examples = [item.get("english", "").strip() for item in chapter.get("examples", []) if item.get("english")]
    points = [item.strip() for item in chapter.get("points", []) if item and item.strip()]
    summary = chapter.get("summary", "").strip()
    if scoring_profile == "list":
        candidates = examples + keyword_phrases(points)
        return "; ".join(dedupe_strings(candidates)[:5]) or "List five precise expressions from the chapter."
    if scoring_profile == "sentence_drill":
        candidates = examples[:4]
        if len(candidates) >= 2:
            return " ".join(candidates)
    if scoring_profile == "rewrite":
        candidates = examples + points
        return " ".join(dedupe_strings(candidates)[:5]) or "Rewrite the sentences using the more natural form."
    sentences = examples[:2] + points[:2]
    if sentences:
        return " ".join(sentences)
    if summary:
        return summary
    return f"Adapted from {book.get('title', 'the source text')}."


def infer_expected_keywords(prompt: dict[str, Any], chapter: dict[str, Any]) -> list[str]:
    prompt_text = prompt.get("prompt", "")
    scoring_profile = infer_scoring_profile(prompt)
    tags = [tag.replace("-", " ") for tag in chapter.get("tags", [])]
    points = chapter.get("points", [])
    if scoring_profile == "list":
        phrases = keyword_phrases(points)
        return dedupe_strings(phrases + tags)[:5]
    return dedupe_strings(tags[:4] + extract_keywords(points))[:6]


def infer_scoring_profile(prompt: dict[str, Any]) -> str:
    prompt_type = prompt.get("type", "").lower()
    prompt_text = prompt.get("prompt", "").lower()
    if prompt_type in {"error_correction", "sentence_transformation"} or prompt_text.startswith("rewrite"):
        return "rewrite"
    if "sentence pair" in prompt_text:
        return "sentence_drill"
    if prompt_text.startswith("list "):
        return "list"
    return "default"


def infer_minimum_word_count(prompt: dict[str, Any], scoring_profile: str) -> int | None:
    if scoring_profile in DEFAULT_MINIMUM_WORD_COUNT:
        return DEFAULT_MINIMUM_WORD_COUNT[scoring_profile]
    return None


def infer_minimum_response_items(prompt: dict[str, Any], scoring_profile: str) -> int | None:
    if scoring_profile == "default":
        return None
    numeric_hint = find_numeric_hint(prompt.get("prompt", ""))
    return numeric_hint


def infer_minimum_keyword_matches(prompt: dict[str, Any]) -> int | None:
    prompt_type = prompt.get("type", "").lower()
    if prompt_type in {"read_and_summarize", "listen_and_summarize"}:
        return 2
    return None


def infer_tone_requirement(prompt: dict[str, Any]) -> bool | None:
    if prompt.get("type", "").lower() == "read_and_summarize":
        return True
    return None


def infer_contrast_requirement(prompt: dict[str, Any]) -> bool | None:
    if prompt.get("type", "").lower() == "listen_and_summarize":
        return True
    return None


def find_numeric_hint(text: str) -> int | None:
    digit_match = re.search(r"\b(\d+)\b", text)
    if digit_match:
        return int(digit_match.group(1))
    lowered = text.lower()
    for word, value in NUMBER_WORDS.items():
        if re.search(rf"\b{word}\b", lowered):
            return value
    return None


def keyword_phrases(points: list[str]) -> list[str]:
    phrases: list[str] = []
    for point in points:
        clauses = re.split(r"[.;:]", point)
        for clause in clauses:
            cleaned = clause.strip()
            if 8 <= len(cleaned) <= 80:
                phrases.append(cleaned)
    return phrases


def extract_keywords(points: list[str]) -> list[str]:
    keywords: list[str] = []
    for point in points:
        keywords.extend(
            [
                token.lower()
                for token in re.split(r"[^A-Za-z]+", point)
                if len(token) >= 4
            ][:2]
        )
    return keywords


def dedupe_strings(values: list[str]) -> list[str]:
    seen: set[str] = set()
    result: list[str] = []
    for value in values:
        cleaned = value.strip()
        if not cleaned:
            continue
        normalized = cleaned.lower()
        if normalized in seen:
            continue
        seen.add(normalized)
        result.append(cleaned)
    return result


def positive_int(value: Any) -> int | None:
    if isinstance(value, int) and value > 0:
        return value
    return None


def explicit_boolean(value: Any) -> bool | None:
    if isinstance(value, bool):
        return value
    return None


def first_non_blank(*values: Any) -> str | None:
    for value in values:
        if isinstance(value, str) and value.strip():
            return value.strip()
    return None


def main() -> None:
    args = parse_args()
    catalog = load_json(args.input)
    overrides = load_overrides(args.overrides)
    enriched_catalog = enrich_catalog(catalog, overrides)
    rendered_output = json.dumps(enriched_catalog, indent=2, ensure_ascii=False) + "\n"
    if args.check:
        current_output = args.output.read_text(encoding="utf-8") if args.output.exists() else ""
        if current_output != rendered_output:
            raise SystemExit(
                f"Generated output for {args.output} is out of date. "
                f"Run the generator without --check to refresh it."
            )
        return

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(rendered_output, encoding="utf-8")


if __name__ == "__main__":
    main()
