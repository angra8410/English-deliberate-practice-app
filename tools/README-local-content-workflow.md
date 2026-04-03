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

## 4. Create a local machine config

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

## 5. Generate or improve the listening script with Ollama

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

Notes:

- built-in asset prompts are updated in their source file such as `activities_b2.json`
- generated catalog prompts are written into `tools/content_metadata_overrides.json`
- after applying a generated catalog script, run the content generator again

## 6. Generate bundled listening audio locally

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

Notes:

- the script scans `app/src/main/assets/content/*.json`
- it prefers Piper automatically when the local config points to a Piper executable and model
- it falls back to Windows SAPI when Piper is not configured
- it groups entries by `audioAsset`
- it can list prompt ids before generating anything
- it skips existing files unless `-Overwrite` is set
- use `-MissingOnly` when you only want the still-unbundled listening items
- it only writes `.wav` files
- it fails if two prompts point at the same `audioAsset` but use different spoken scripts

## 7. Verify before committing

```powershell
python -m unittest discover -s tools -p "test_*.py"
```

Then run the Android unit tests you normally use for parser and asset validation.
