# Content Generator

Use `tools/generate_content_repository.py` to turn a raw or partially enriched book catalog into the app asset at `app/src/main/assets/content/content_repository.json`.

## Usage

```powershell
python tools/generate_content_repository.py `
  --input app/src/main/assets/content/content_repository.json `
  --output app/src/main/assets/content/content_repository.json `
  --overrides tools/content_metadata_overrides.json
```

## What the generator does

- keeps existing prompt ids, chapters, and book metadata
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
    }
  }
}
```

## Recommended workflow

1. Generate your raw book catalog JSON from your notes.
2. Run the content generator with the raw catalog as `--input`.
3. Keep high-quality prompt-specific metadata in `tools/content_metadata_overrides.json`.
4. Re-run the generator whenever the raw catalog changes.
5. Run `:app:testDebugUnitTest` to confirm the parser and asset smoke tests still pass.
