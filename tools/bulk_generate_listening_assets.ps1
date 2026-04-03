param(
    [string]$ConfigPath = "tools/local_audio_pipeline.config.json",
    [string[]]$PromptIds,
    [switch]$AllCandidates,
    [switch]$MissingScriptOnly,
    [switch]$MissingAudioOnly,
    [int]$Limit,
    [switch]$SkipScriptGeneration,
    [switch]$SkipAudioGeneration,
    [ValidateSet("natural", "exam", "dialogue")]
    [string]$Style = "natural",
    [int]$WordCount = 70,
    [ValidateSet("auto", "piper", "sapi")]
    [string]$Engine = "auto",
    [switch]$Overwrite,
    [switch]$ListOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (-not $PromptIds -and -not $AllCandidates) {
    throw "Specify -PromptIds or -AllCandidates."
}

$targetArguments = @()
if ($PromptIds) {
    $targetArguments += "--prompt-ids"
    $targetArguments += $PromptIds
} elseif ($AllCandidates) {
    $targetArguments += "--all-candidates"
}
$sharedFilterArguments = @()
if ($MissingScriptOnly) {
    $sharedFilterArguments += "--missing-script-only"
}
if ($MissingAudioOnly) {
    $sharedFilterArguments += "--missing-audio-only"
}
if ($Limit -gt 0) {
    $sharedFilterArguments += @("--limit", [string]$Limit)
}

$resolvedPromptIds = @()
if ($PromptIds) {
    $resolvedPromptIds = @($PromptIds)
} else {
    $resolvedPromptIds = @(
        & python tools/generate_listening_script_with_ollama.py `
            $targetArguments `
            $sharedFilterArguments `
            --list-ids `
            --config $ConfigPath
    )
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}

if ($resolvedPromptIds.Count -eq 0) {
    throw "No listening candidates matched the current filters."
}

if ($ListOnly) {
    & python tools/generate_listening_script_with_ollama.py --prompt-ids $resolvedPromptIds --list-candidates --config $ConfigPath
    exit $LASTEXITCODE
}

if (-not $SkipScriptGeneration) {
    & python tools/generate_listening_script_with_ollama.py --prompt-ids $resolvedPromptIds --style $Style --word-count $WordCount --apply --config $ConfigPath
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}

if (-not $SkipAudioGeneration) {
    $audioScriptPath = Join-Path $PSScriptRoot "generate_listening_audio.ps1"
    $audioArguments = @{
        ConfigPath = $ConfigPath
        Engine = $Engine
        PromptIds = $resolvedPromptIds
    }
    if ($Overwrite) {
        $audioArguments["Overwrite"] = $true
    }

    & $audioScriptPath @audioArguments
    exit $LASTEXITCODE
}
