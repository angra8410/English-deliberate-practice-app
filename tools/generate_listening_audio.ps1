param(
    [string]$ContentDirectory = "app/src/main/assets/content",
    [string]$AssetsRoot = "app/src/main/assets",
    [ValidateSet("auto", "piper", "sapi")]
    [string]$Engine = "auto",
    [string]$ConfigPath = "tools/local_audio_pipeline.config.json",
    [string]$VoiceName,
    [string]$PiperExecutable,
    [string]$PiperModel,
    [string]$PiperConfig,
    [int]$PiperSpeakerId = -1,
    [string[]]$PromptIds,
    [switch]$Overwrite,
    [switch]$UsePromptFallback,
    [switch]$MissingOnly,
    [switch]$ListCandidates,
    [switch]$ListVoices,
    [switch]$ShowConfig
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-PipelineConfig {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        return [pscustomobject]@{}
    }

    return Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json
}

function Get-ConfigValue {
    param(
        $Config,
        [string]$PropertyName
    )

    if ($null -eq $Config) {
        return $null
    }

    $property = $Config.PSObject.Properties[$PropertyName]
    if ($null -eq $property) {
        return $null
    }

    return $property.Value
}

function First-NonBlank {
    param([object[]]$Values)

    foreach ($value in $Values) {
        if ($value -is [string] -and -not [string]::IsNullOrWhiteSpace($value)) {
            return $value.Trim()
        }
    }

    return $null
}

function Resolve-FirstCommandOrPath {
    param([object[]]$Values)

    foreach ($candidate in $Values) {
        if (-not ($candidate -is [string]) -or [string]::IsNullOrWhiteSpace($candidate)) {
            continue
        }

        $value = $candidate.Trim()
        $looksLikePath = $value.Contains("\") -or $value.Contains("/") -or $value.Contains(":")
        if ($looksLikePath) {
            if (Test-Path -LiteralPath $value) {
                return (Resolve-Path -LiteralPath $value).Path
            }
            continue
        }

        $command = Get-Command $value -ErrorAction SilentlyContinue
        if ($null -ne $command) {
            return $command.Source
        }
    }

    return $null
}

function Resolve-FirstExistingPath {
    param([object[]]$Values)

    foreach ($candidate in $Values) {
        if (-not ($candidate -is [string]) -or [string]::IsNullOrWhiteSpace($candidate)) {
            continue
        }

        $value = $candidate.Trim()
        if (Test-Path -LiteralPath $value) {
            return (Resolve-Path -LiteralPath $value).Path
        }
    }

    return $null
}

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

function Get-ResolvedAudioSettings {
    param(
        [string]$RequestedEngine,
        [string]$ConfigPath,
        [string]$VoiceName,
        [string]$PiperExecutable,
        [string]$PiperModel,
        [string]$PiperConfig,
        [int]$PiperSpeakerId,
        [switch]$AllowIncomplete
    )

    $config = Get-PipelineConfig -Path $ConfigPath
    $configuredPreferredEngine = First-NonBlank @(Get-ConfigValue -Config $config -PropertyName "preferredEngine")
    $effectiveRequestedEngine = if ($RequestedEngine -eq "auto" -and $configuredPreferredEngine) {
        $configuredPreferredEngine.ToLowerInvariant()
    } else {
        $RequestedEngine
    }

    $resolvedPiperExecutable = Resolve-FirstCommandOrPath @(
        $PiperExecutable,
        (Get-ConfigValue -Config $config -PropertyName "piperExecutable"),
        "piper"
    )
    $resolvedPiperModel = Resolve-FirstExistingPath @(
        $PiperModel,
        (Get-ConfigValue -Config $config -PropertyName "piperModel")
    )
    $resolvedPiperConfig = Resolve-FirstExistingPath @(
        $PiperConfig,
        (Get-ConfigValue -Config $config -PropertyName "piperConfig")
    )
    $resolvedPiperSpeakerId = if ($PiperSpeakerId -ge 0) {
        $PiperSpeakerId
    } else {
        $configuredSpeaker = Get-ConfigValue -Config $config -PropertyName "piperSpeakerId"
        if ($configuredSpeaker -is [int]) { $configuredSpeaker } else { -1 }
    }
    $resolvedVoiceName = First-NonBlank @($VoiceName)

    $hasPiper = $null -ne $resolvedPiperExecutable -and -not [string]::IsNullOrWhiteSpace($resolvedPiperModel)
    $resolvedEngine = switch ($effectiveRequestedEngine) {
        "piper" {
            if (-not $AllowIncomplete -and $null -eq $resolvedPiperExecutable) {
                throw "Piper was requested, but no Piper executable was found. Install piper-tts or set piperExecutable in tools/local_audio_pipeline.config.json."
            }
            if (-not $AllowIncomplete -and [string]::IsNullOrWhiteSpace($resolvedPiperModel)) {
                throw "Piper was requested, but no Piper voice model .onnx file was found. Download a voice such as en_US-lessac-medium and set piperModel in tools/local_audio_pipeline.config.json."
            }
            "piper"
        }
        "sapi" { "sapi" }
        default {
            if ($hasPiper) { "piper" } else { "sapi" }
        }
    }

    return [pscustomobject]@{
        Engine = $resolvedEngine
        VoiceName = $resolvedVoiceName
        PiperExecutable = $resolvedPiperExecutable
        PiperModel = $resolvedPiperModel
        PiperConfig = $resolvedPiperConfig
        PiperSpeakerId = $resolvedPiperSpeakerId
        ConfigPath = $ConfigPath
        ConfigLoaded = Test-Path -LiteralPath $ConfigPath
    }
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

    $titleProperty = $Node.PSObject.Properties["title"]
    $title = if ($null -ne $titleProperty) { $titleProperty.Value } else { $null }

    $promptProperty = $Node.PSObject.Properties["prompt"]
    $promptText = if ($null -ne $promptProperty) { $promptProperty.Value } else { $null }

    $listeningPromptProperty = $Node.PSObject.Properties["listeningPromptText"]
    $scriptText = if ($null -ne $listeningPromptProperty) { $listeningPromptProperty.Value } else { $null }
    if ([string]::IsNullOrWhiteSpace($scriptText) -and $UsePromptFallback) {
        $scriptText = $promptText
    }
    if ([string]::IsNullOrWhiteSpace($scriptText)) {
        return
    }

    $Candidates.Add([pscustomobject]@{
        Id = $id
        Title = [string]$title
        Prompt = [string]$promptText
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

function Write-WaveFileWithSapi {
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

function Write-WaveFileWithPiper {
    param(
        [string]$OutputPath,
        [string]$Text,
        $Settings
    )

    if (-not (Test-Path -LiteralPath $Settings.PiperExecutable)) {
        throw "Piper executable was not found at $($Settings.PiperExecutable)"
    }
    if (-not (Test-Path -LiteralPath $Settings.PiperModel)) {
        throw "Piper model was not found at $($Settings.PiperModel)"
    }
    if ($Settings.PiperConfig -and -not (Test-Path -LiteralPath $Settings.PiperConfig)) {
        throw "Piper config was not found at $($Settings.PiperConfig)"
    }

    $outputDirectory = Split-Path -Parent $OutputPath
    if (-not (Test-Path -LiteralPath $outputDirectory)) {
        New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
    }

    $tempInput = [System.IO.Path]::GetTempFileName()
    try {
        [System.IO.File]::WriteAllText($tempInput, $Text, [System.Text.Encoding]::UTF8)
        $arguments = @(
            "--model", $Settings.PiperModel,
            "--output_file", $OutputPath
        )
        if ($Settings.PiperConfig) {
            $arguments += @("--config", $Settings.PiperConfig)
        }
        if ($Settings.PiperSpeakerId -ge 0) {
            $arguments += @("--speaker", [string]$Settings.PiperSpeakerId)
        }

        $piperInput = Get-Content -LiteralPath $tempInput -Raw
        $piperInput | & $Settings.PiperExecutable @arguments
        if ($LASTEXITCODE -ne 0) {
            throw "Piper exited with code $LASTEXITCODE while writing $OutputPath"
        }
    } finally {
        if (Test-Path -LiteralPath $tempInput) {
            Remove-Item -LiteralPath $tempInput -Force
        }
    }
}

function Write-WaveFileFromText {
    param(
        [string]$OutputPath,
        [string]$Text,
        $Settings
    )

    switch ($Settings.Engine) {
        "piper" { Write-WaveFileWithPiper -OutputPath $OutputPath -Text $Text -Settings $Settings }
        "sapi" { Write-WaveFileWithSapi -OutputPath $OutputPath -Text $Text -RequestedVoiceName $Settings.VoiceName }
        default { throw "Unsupported audio engine: $($Settings.Engine)" }
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
            Title = $primary.Title
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

$settings = Get-ResolvedAudioSettings `
    -RequestedEngine $Engine `
    -ConfigPath $ConfigPath `
    -VoiceName $VoiceName `
    -PiperExecutable $PiperExecutable `
    -PiperModel $PiperModel `
    -PiperConfig $PiperConfig `
    -PiperSpeakerId $PiperSpeakerId `
    -AllowIncomplete:$ShowConfig

if ($ShowConfig) {
    $settings | Format-List
    exit 0
}

if ($ListVoices) {
    if ($settings.Engine -eq "piper") {
        Write-Host "Piper uses model files rather than installed Windows desktop voices."
        Write-Host "Resolved Piper executable: $($settings.PiperExecutable)"
        Write-Host "Resolved Piper model: $($settings.PiperModel)"
        exit 0
    }

    Get-SapiVoiceDescriptions
    exit 0
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
        Select-Object Id, Title, AudioAsset, Exists, SourcePath |
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

    Write-WaveFileFromText -OutputPath $outputPath -Text $entry.ScriptPreview -Settings $settings
    Write-Host "Generated $outputPath from $($entry.Id) using $($settings.Engine)"
}
