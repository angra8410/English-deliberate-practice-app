# Content Generator

Use `tools/generate_content_repository.py` to turn a checked-in raw book catalog into the app asset at `app/src/main/assets/content/content_repository.json`.

## Usage

```powershell
python tools/generate_content_repository.py `
  --input tools/content_repository.raw.json `
  --output app/src/main/assets/content/content_repository.json `
  --overrides tools/content_metadata_overrides.json
```

## Drift check

```powershell
python tools/generate_content_repository.py `
  --input tools/content_repository.raw.json `
  --output app/src/main/assets/content/content_repository.json `
  --overrides tools/content_metadata_overrides.json `
  --check
```

## Generator tests

```powershell
python -m unittest discover -s tools -p "test_*.py"
```

## What the generator does

- reads raw book and chapter data from `tools/content_repository.raw.json`
- infers prompt-level evaluation metadata when missing
- applies prompt-specific overrides from `tools/content_metadata_overrides.json`
- writes a stable, pretty-printed JSON file for the Android asset pipeline

## Prompt metadata supported

- `instructions`
- `starterText`
- `modelAnswer`
- `expectedKeywords`
- `scoringProfile`
- `minimumWordCount`
- `minimumResponseItems`
- `minimumKeywordMatches`
- `requiresToneReference`
- `requiresContrastMarker`
- `audioAsset`
- `listeningPromptText`

## Override format

```json
{
  "prompts": {
    "applying-for-a-job-prompt-2": {
      "instructions": "List five phrases that commonly appear in job advertisements.",
      "expectedKeywords": [
        "competitive salary",
        "career prospects"
      ],
      "scoringProfile": "list",
      "minimumResponseItems": 5
    },
    "interview-panel-summary-prompt-1": {
      "audioAsset": "audio/c1_interview_panel_summary.wav",
      "listeningPromptText": "The panel agrees that the candidate should move to the next stage because her preparation and communication were stronger than those of the other applicants. However, one interviewer still wants a clearer example of how she handles multiple deadlines under pressure."
    }
  }
}
```

## Recommended workflow

1. Generate or update `tools/content_repository.raw.json` from your notes.
2. Keep high-quality prompt-specific metadata in `tools/content_metadata_overrides.json`.
3. Run the content generator to refresh `app/src/main/assets/content/content_repository.json`.
4. Run the generator again with `--check` when you want to verify the asset has not drifted.
5. Run `python -m unittest discover -s tools -p "test_*.py"` to verify generator inference and drift behavior.
6. Run `:app:testDebugUnitTest` to confirm the parser and asset smoke tests still pass.

## Listening audio workflow

- Keep listening prompt scripts in `listeningPromptText` so the app can fall back to spoken prompt playback and local tooling can generate bundled audio from the same source data.
- Use repo-relative audio asset paths under `app/src/main/assets/audio`, for example `audio/c1_interview_panel_summary.wav`.
- Prefer `.wav` for generated local audio. The included PowerShell generator writes wave files without any external services.
