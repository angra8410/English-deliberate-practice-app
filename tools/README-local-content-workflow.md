# Local Content And Audio Workflow

This app is set up for a local-only workflow. Content stays in the repo, and bundled listening audio stays under `app/src/main/assets/audio`.

## 1. Add or update material

You have two supported content paths:

- built-in activity packs:
  - edit `app/src/main/assets/content/activities_b2.json`
  - edit `app/src/main/assets/content/activities_c1.json`
- generated book catalog:
  - edit `tools/content_repository.raw.json`
  - keep prompt-specific metadata in `tools/content_metadata_overrides.json`
  - regenerate `app/src/main/assets/content/content_repository.json`

## 2. Author listening tasks for local audio

For any listening item you want to bundle locally, keep both of these fields together:

- `audioAsset`: repo-relative asset path such as `audio/c1_interview_panel_summary.wav`
- `listeningPromptText`: the exact spoken script that should be synthesized into the bundled file

Recommended rules:

- prefer `.wav` for locally generated files
- keep the spoken script natural and close to real listening input, not just the task instruction
- keep asset paths under `app/src/main/assets/audio`

Example:

```json
{
  "id": "act_c1_listen_001",
  "exerciseType": "LISTEN_AND_SUMMARIZE",
  "prompt": "Listen to the audio and summarize the speaker's main claim and two implications.",
  "audioAsset": "audio/c1_listen_001.wav",
  "listeningPromptText": "The speaker argues that remote-first companies should not evaluate collaboration only by meeting frequency. She accepts that frequent check-ins can prevent misalignment, but says they often replace deeper written coordination and independent problem-solving."
}
```

## 3. Regenerate the book catalog asset when needed

```powershell
python tools/generate_content_repository.py `
  --input tools/content_repository.raw.json `
  --output app/src/main/assets/content/content_repository.json `
  --overrides tools/content_metadata_overrides.json
```

## 4. Generate bundled listening audio locally

List installed Windows voices:

```powershell
powershell -ExecutionPolicy Bypass -File tools/generate_listening_audio.ps1 -ListVoices
```

Generate missing `.wav` files from all content JSON:

```powershell
powershell -ExecutionPolicy Bypass -File tools/generate_listening_audio.ps1
```

Regenerate everything with a specific voice:

```powershell
powershell -ExecutionPolicy Bypass -File tools/generate_listening_audio.ps1 `
  -VoiceName "Microsoft Zira Desktop" `
  -Overwrite
```

Generate only one prompt:

```powershell
powershell -ExecutionPolicy Bypass -File tools/generate_listening_audio.ps1 `
  -PromptIds act_c1_listen_001 `
  -Overwrite
```

Notes:

- the script scans `app/src/main/assets/content/*.json`
- it groups entries by `audioAsset`
- it skips existing files unless `-Overwrite` is set
- it only writes `.wav` files
- it fails if two prompts point at the same `audioAsset` but use different spoken scripts

## 5. Verify before committing

```powershell
python -m unittest discover -s tools -p "test_*.py"
```

Then run the Android unit tests you normally use for parser and asset validation.
