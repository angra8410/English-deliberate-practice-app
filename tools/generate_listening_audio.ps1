param(
    [string]$ContentDirectory = "app/src/main/assets/content",
    [string]$AssetsRoot = "app/src/main/assets",
    [string]$VoiceName,
    [string[]]$PromptIds,
    [switch]$Overwrite,
    [switch]$UsePromptFallback,
    [switch]$MissingOnly,
    [switch]$ListCandidates,
    [switch]$ListVoices
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-SapiVoiceDescriptions {
    $voice = New-Object -ComObject SAPI.SpVoice
    try {
        return $voice.GetVoices() |
            ForEach-Object { $_.GetDescription() } |
            Sort-Object -Unique
    } finally {
        [void][System.Runtime.InteropServices.Marshal]::FinalReleaseComObject($voice)
    }
}

if ($ListVoices) {
    Get-SapiVoiceDescriptions
    exit 0
}

function Get-ContentDocuments {
    param([string]$Directory)

    $resolvedDirectory = Resolve-Path -LiteralPath $Directory
    Get-ChildItem -LiteralPath $resolvedDirectory -Filter *.json -File |
        Sort-Object FullName |
        ForEach-Object {
            $json = Get-Content -LiteralPath $_.FullName -Raw | ConvertFrom-Json
            [pscustomobject]@{
                Path = $_.FullName
                Json = $json
            }
        }
}

function Add-Candidate {
    param(
        [System.Collections.Generic.List[object]]$Candidates,
        [string]$SourcePath,
        $Node,
        [switch]$UsePromptFallback,
        [string[]]$PromptIds
    )

    if ($null -eq $Node) {
        return
    }

    $audioAssetProperty = $Node.PSObject.Properties["audioAsset"]
    $audioAsset = if ($null -ne $audioAssetProperty) { $audioAssetProperty.Value } else { $null }
    if ([string]::IsNullOrWhiteSpace($audioAsset)) {
        return
    }

    $idProperty = $Node.PSObject.Properties["id"]
    $id = if ($null -ne $idProperty) { $idProperty.Value } else { $null }
    if ($PromptIds -and ($id -notin $PromptIds)) {
        return
    }

    $listeningPromptProperty = $Node.PSObject.Properties["listeningPromptText"]
    $scriptText = if ($null -ne $listeningPromptProperty) { $listeningPromptProperty.Value } else { $null }
    if ([string]::IsNullOrWhiteSpace($scriptText) -and $UsePromptFallback) {
        $promptProperty = $Node.PSObject.Properties["prompt"]
        $scriptText = if ($null -ne $promptProperty) { $promptProperty.Value } else { $null }
    }
    if ([string]::IsNullOrWhiteSpace($scriptText)) {
        return
    }

    $Candidates.Add([pscustomobject]@{
        Id = $id
        AudioAsset = [string]$audioAsset
        ScriptText = ([string]$scriptText).Trim()
        SourcePath = $SourcePath
    })
}

function Collect-AudioCandidates {
    param(
        [System.Collections.Generic.List[object]]$Candidates,
        [string]$SourcePath,
        $Node,
        [switch]$UsePromptFallback,
        [string[]]$PromptIds
    )

    if ($Node -is [System.Collections.IEnumerable] -and -not ($Node -is [string])) {
        foreach ($Item in $Node) {
            Collect-AudioCandidates -Candidates $Candidates -SourcePath $SourcePath -Node $Item -UsePromptFallback:$UsePromptFallback -PromptIds $PromptIds
        }
        return
    }

    if ($Node -isnot [psobject]) {
        return
    }

    Add-Candidate -Candidates $Candidates -SourcePath $SourcePath -Node $Node -UsePromptFallback:$UsePromptFallback -PromptIds $PromptIds

    foreach ($Property in $Node.PSObject.Properties) {
        $Value = $Property.Value
        if ($Value -is [System.Collections.IEnumerable] -and -not ($Value -is [string])) {
            foreach ($Child in $Value) {
                Collect-AudioCandidates -Candidates $Candidates -SourcePath $SourcePath -Node $Child -UsePromptFallback:$UsePromptFallback -PromptIds $PromptIds
            }
        } elseif ($Value -is [psobject] -and -not ($Value -is [string])) {
            Collect-AudioCandidates -Candidates $Candidates -SourcePath $SourcePath -Node $Value -UsePromptFallback:$UsePromptFallback -PromptIds $PromptIds
        }
    }
}

function Select-SapiVoice {
    param(
        $Voice,
        [string]$RequestedVoiceName
    )

    if ([string]::IsNullOrWhiteSpace($RequestedVoiceName)) {
        return
    }

    $matchingVoice = $Voice.GetVoices() |
        Where-Object { $_.GetDescription() -eq $RequestedVoiceName } |
        Select-Object -First 1

    if ($null -eq $matchingVoice) {
        $availableVoices = Get-SapiVoiceDescriptions
        throw "Voice '$RequestedVoiceName' was not found. Available voices: $($availableVoices -join ', ')"
    }

    $Voice.Voice = $matchingVoice
}

function Write-WaveFileFromText {
    param(
        [string]$OutputPath,
        [string]$Text,
        [string]$RequestedVoiceName
    )

    $voice = New-Object -ComObject SAPI.SpVoice
    $stream = New-Object -ComObject SAPI.SpFileStream

    try {
        Select-SapiVoice -Voice $voice -RequestedVoiceName $RequestedVoiceName

        $outputDirectory = Split-Path -Parent $OutputPath
        if (-not (Test-Path -LiteralPath $outputDirectory)) {
            New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
        }

        $stream.Format.Type = 22
        $stream.Open($OutputPath, 3, $false)
        $voice.AudioOutputStream = $stream
        [void]$voice.Speak($Text)
        $stream.Close()
    } finally {
        if ($null -ne $stream) {
            [void][System.Runtime.InteropServices.Marshal]::FinalReleaseComObject($stream)
        }
        if ($null -ne $voice) {
            [void][System.Runtime.InteropServices.Marshal]::FinalReleaseComObject($voice)
        }
    }
}

function Get-AudioEntries {
    param(
        [System.Collections.Generic.List[object]]$Candidates,
        [string]$ResolvedAssetsRoot,
        [switch]$MissingOnly
    )

    $entries = foreach ($group in ($Candidates | Group-Object AudioAsset)) {
        $groupEntries = @($group.Group)
        $primary = $groupEntries[0]
        $distinctScripts = @($groupEntries | Select-Object -ExpandProperty ScriptText -Unique)
        $relativeAudioAsset = $primary.AudioAsset.Replace('/', [System.IO.Path]::DirectorySeparatorChar)
        $outputPath = Join-Path $ResolvedAssetsRoot $relativeAudioAsset
        $fileExists = Test-Path -LiteralPath $outputPath

        [pscustomobject]@{
            Id = $primary.Id
            AudioAsset = $primary.AudioAsset
            OutputPath = $outputPath
            Exists = $fileExists
            SourcePath = $primary.SourcePath
            ScriptVariantCount = $distinctScripts.Count
            ScriptPreview = if ($distinctScripts.Count -gt 0) { $distinctScripts[0] } else { "" }
            Entries = $groupEntries
        }
    }

    if ($MissingOnly) {
        return @($entries | Where-Object { -not $_.Exists })
    }

    return @($entries)
}

$candidates = New-Object 'System.Collections.Generic.List[object]'
Get-ContentDocuments -Directory $ContentDirectory | ForEach-Object {
    Collect-AudioCandidates -Candidates $candidates -SourcePath $_.Path -Node $_.Json -UsePromptFallback:$UsePromptFallback -PromptIds $PromptIds
}

if ($candidates.Count -eq 0) {
    throw "No listening audio candidates were found. Add audioAsset plus listeningPromptText to your content JSON first."
}

$resolvedAssetsRoot = (Resolve-Path -LiteralPath $AssetsRoot).Path
$audioEntries = @(Get-AudioEntries -Candidates $candidates -ResolvedAssetsRoot $resolvedAssetsRoot -MissingOnly:$MissingOnly)

if ($ListCandidates) {
    $audioEntries |
        Sort-Object Id |
        Select-Object Id, AudioAsset, Exists, SourcePath |
        Format-Table -AutoSize
    exit 0
}

if ($audioEntries.Count -eq 0) {
    throw "No listening audio candidates matched the current filters."
}

foreach ($entry in $audioEntries) {
    if ($entry.ScriptVariantCount -ne 1) {
        $sources = $entry.Entries | ForEach-Object { "$($_.Id) from $($_.SourcePath)" }
        throw "Conflicting listeningPromptText values found for $($entry.AudioAsset): $($sources -join '; ')"
    }

    $relativeAudioAsset = $entry.AudioAsset.Replace('/', [System.IO.Path]::DirectorySeparatorChar)
    $extension = [System.IO.Path]::GetExtension($relativeAudioAsset)
    if ($extension -ne ".wav") {
        Write-Warning "Skipping $($entry.Id) because $($entry.AudioAsset) is not a .wav target."
        continue
    }

    $outputPath = $entry.OutputPath
    if ((Test-Path -LiteralPath $outputPath) -and -not $Overwrite) {
        Write-Host "Skipping existing file $outputPath"
        continue
    }

    Write-WaveFileFromText -OutputPath $outputPath -Text $entry.ScriptPreview -RequestedVoiceName $VoiceName
    Write-Host "Generated $outputPath from $($entry.Id)"
}
