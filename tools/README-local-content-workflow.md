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

## 3. Scaffold a new built-in listening activity

Use the helper when you want to add a new B2 or C1 listening item to the built-in activity packs without hand-editing the JSON shape:

```powershell
python tools/create_listening_activity.py `
  --level C1 `
  --title "Vendor handover call" `
  --prompt "Listen to the handover call and summarize the final agreement and one risk." `
  --evaluation-targets agreement risk deadline `
  --tags handover operations `
  --minimum-word-count 60 `
  --requires-contrast-marker
```

What it does:

- appends a new `LISTEN_AND_SUMMARIZE` item to `activities_b2.json` or `activities_c1.json`
- auto-picks the level's listening unit id
- derives a clean activity id if you do not pass `--id`
- derives `audioAsset` as `audio/<activity_id>.wav` if you do not pass `--audio-asset`
- prints the next local commands to generate the script with Ollama and the `.wav` with Piper

Use `--dry-run` if you want to preview the JSON first:

```powershell
python tools/create_listening_activity.py `
  --level B2 `
  --title "Team stand-up recap" `
  --prompt "Listen to the recap and summarize the team's final decision." `
  --dry-run
```

You can also provide the spoken script up front:

```powershell
python tools/create_listening_activity.py `
  --level B2 `
  --title "Team stand-up recap" `
  --prompt "Listen to the recap and summarize the team's final decision." `
  --listening-prompt-text "Okay, so the team agrees to move the release by two days..."
```

If you leave out `--listening-prompt-text`, the helper creates the activity without a script and tells you to run the Ollama script next.

## 4. Regenerate the book catalog asset when needed

```powershell
python tools/generate_content_repository.py `
  --input tools/content_repository.raw.json `
  --output app/src/main/assets/content/content_repository.json `
  --overrides tools/content_metadata_overrides.json
```

## 5. Create a local machine config

Copy the example file and fill in the local paths that only exist on your machine:

```powershell
Copy-Item tools/local_audio_pipeline.config.example.json tools/local_audio_pipeline.config.json
```

Recommended fields:

- `ollamaModel`: local model name such as `gemma3:27b`
- `ollamaHost`: usually `http://127.0.0.1:11434`
- `piperExecutable`: full path to `piper.exe`
- `piperModel`: full path to the Piper `.onnx` voice model
- `piperConfig`: full path to the matching `.onnx.json` file

The real config file is gitignored because it is machine-specific.

## 6. Generate or improve the listening script with Ollama

Preview the prompt that will be sent to Ollama:

```powershell
python tools/generate_listening_script_with_ollama.py `
  --prompt-id listening-b2-summary `
  --print-prompt
```

Generate a new TTS-friendly script:

```powershell
python tools/generate_listening_script_with_ollama.py `
  --prompt-id listening-b2-summary
```

Generate and apply it back to the correct source JSON:

```powershell
python tools/generate_listening_script_with_ollama.py `
  --prompt-id listening-b2-summary `
  --apply
```

Generate scripts for multiple prompt ids in one run:

```powershell
python tools/generate_listening_script_with_ollama.py `
  --prompt-ids listening-b2-summary act_c1_listen_001 `
  --apply
```

List only the candidates that still need a generated script:

```powershell
python tools/generate_listening_script_with_ollama.py `
  --all-candidates `
  --missing-script-only `
  --list-candidates
```

Notes:

- built-in asset prompts are updated in their source file such as `activities_b2.json`
- generated catalog prompts are written into `tools/content_metadata_overrides.json`
- after applying a generated catalog script, run the content generator again

## 7. Generate bundled listening audio locally

List installed Windows voices:

```powershell
powershell -ExecutionPolicy Bypass -File tools/generate_listening_audio.ps1 -ListVoices
```

Show which engine and config the script will use:

```powershell
powershell -ExecutionPolicy Bypass -File tools/generate_listening_audio.ps1 -ShowConfig
```

List all listening candidates so you can see the exact prompt ids before generating:

```powershell
powershell -ExecutionPolicy Bypass -File tools/generate_listening_audio.ps1 -ListCandidates
```

List only the candidates that are still missing a bundled `.wav`:

```powershell
powershell -ExecutionPolicy Bypass -File tools/generate_listening_audio.ps1 `
  -ListCandidates `
  -MissingOnly
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

Force Piper when it is configured:

```powershell
powershell -ExecutionPolicy Bypass -File tools/generate_listening_audio.ps1 `
  -Engine piper `
  -PromptIds listening-b2-summary `
  -Overwrite
```

Or stay with Windows SAPI:

```powershell
powershell -ExecutionPolicy Bypass -File tools/generate_listening_audio.ps1 `
  -Engine sapi `
  -PromptIds listening-b2-summary `
  -Overwrite
```

## 8. Run the full bulk pipeline

Generate scripts and audio for multiple prompt ids:

```powershell
powershell -ExecutionPolicy Bypass -File tools/bulk_generate_listening_assets.ps1 `
  -PromptIds listening-b2-summary act_c1_listen_001 `
  -Engine piper `
  -Overwrite
```

Or target every listening candidate that is still missing audio:

```powershell
powershell -ExecutionPolicy Bypass -File tools/bulk_generate_listening_assets.ps1 `
  -AllCandidates `
  -MissingAudioOnly `
  -Engine piper `
  -Overwrite
```

Or list what would be processed without changing files:

```powershell
powershell -ExecutionPolicy Bypass -File tools/bulk_generate_listening_assets.ps1 `
  -AllCandidates `
  -MissingScriptOnly `
  -ListOnly
```

Or process only the first few filtered candidates while you tune prompts and voices:

```powershell
powershell -ExecutionPolicy Bypass -File tools/bulk_generate_listening_assets.ps1 `
  -AllCandidates `
  -Limit 3 `
  -Engine piper `
  -Overwrite
```

Notes:

- the script scans `app/src/main/assets/content/*.json`
- it prefers Piper automatically when the local config points to a Piper executable and model
- it falls back to Windows SAPI when Piper is not configured
- it groups entries by `audioAsset`
- it can list prompt ids before generating anything
- it can cap a run with `-Limit` so you can work in small batches
- it skips existing files unless `-Overwrite` is set
- use `-MissingOnly` when you only want the still-unbundled listening items
- it only writes `.wav` files
- it fails if two prompts point at the same `audioAsset` but use different spoken scripts

## 9. Verify before committing

```powershell
python -m unittest discover -s tools -p "test_*.py"
```

Then run the Android unit tests you normally use for parser and asset validation.
