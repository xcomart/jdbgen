<#
.SYNOPSIS
    Regenerates resource/icon.ico from resource/icon.png.

.DESCRIPTION
    jpackage needs a Windows .ico for the generated launcher executable, the
    MSI's Add/Remove Programs entry and the Start menu / desktop shortcuts.
    The application itself keeps using resource/icon.png, so the .ico is a
    derived artifact: whenever icon.png changes, re-run this script.

    The produced file is a multi-resolution "PNG-in-ICO" container: every
    frame is stored as a PNG stream rather than a DIB. That form is understood
    by Windows Vista and newer (and therefore by WiX / the MSI shell), keeps
    the alpha channel intact and stays small.

    Requires Windows PowerShell (System.Drawing). No external tooling.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File packaging\make-icon.ps1
#>
[CmdletBinding()]
param(
    [string] $SourcePng,
    [string] $TargetIco,
    [int[]]  $Sizes = @(16, 24, 32, 48, 64, 128, 256)
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

# $PSScriptRoot is not reliably bound while param() defaults are evaluated under
# Windows PowerShell 5.1, so the repository-relative defaults are applied here.
$projectRoot = Split-Path -Parent $PSScriptRoot
if (-not $SourcePng) { $SourcePng = Join-Path $projectRoot 'resource\icon.png' }
if (-not $TargetIco) { $TargetIco = Join-Path $projectRoot 'resource\icon.ico' }

$SourcePng = [System.IO.Path]::GetFullPath($SourcePng)
$TargetIco = [System.IO.Path]::GetFullPath($TargetIco)

if (-not (Test-Path -LiteralPath $SourcePng)) {
    throw "Source image not found: $SourcePng"
}

Write-Host "Reading  $SourcePng"
$source = [System.Drawing.Image]::FromFile($SourcePng)
try {
    # Render the source into one PNG byte stream per requested icon size.
    $frames = @()
    foreach ($size in $Sizes) {
        if ($size -lt 1 -or $size -gt 256) {
            throw "Icon size out of range (1..256): $size"
        }

        $bitmap = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
        try {
            $graphics.CompositingMode    = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
            $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
            $graphics.InterpolationMode  = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
            $graphics.SmoothingMode      = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
            $graphics.PixelOffsetMode    = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
            $graphics.DrawImage($source, (New-Object System.Drawing.Rectangle(0, 0, $size, $size)))
        } finally {
            $graphics.Dispose()
        }

        $buffer = New-Object System.IO.MemoryStream
        try {
            $bitmap.Save($buffer, [System.Drawing.Imaging.ImageFormat]::Png)
            $frames += [pscustomobject]@{ Size = $size; Bytes = $buffer.ToArray() }
        } finally {
            $buffer.Dispose()
            $bitmap.Dispose()
        }
    }
} finally {
    $source.Dispose()
}

# ICO layout: 6 byte ICONDIR, then one 16 byte ICONDIRENTRY per frame, then the
# image payloads. A width/height byte of 0 means 256 pixels.
$output = New-Object System.IO.MemoryStream
$writer = New-Object System.IO.BinaryWriter($output)
try {
    $writer.Write([uint16]0)                # reserved
    $writer.Write([uint16]1)                # type: 1 = icon
    $writer.Write([uint16]$frames.Count)    # image count

    $offset = 6 + (16 * $frames.Count)
    foreach ($frame in $frames) {
        $dimension = if ($frame.Size -ge 256) { 0 } else { $frame.Size }
        $writer.Write([byte]$dimension)     # width
        $writer.Write([byte]$dimension)     # height
        $writer.Write([byte]0)              # palette entries (0 = truecolor)
        $writer.Write([byte]0)              # reserved
        $writer.Write([uint16]1)            # colour planes
        $writer.Write([uint16]32)           # bits per pixel
        $writer.Write([uint32]$frame.Bytes.Length)
        $writer.Write([uint32]$offset)
        $offset += $frame.Bytes.Length
    }

    foreach ($frame in $frames) {
        $writer.Write($frame.Bytes)
    }

    $writer.Flush()
    [System.IO.File]::WriteAllBytes($TargetIco, $output.ToArray())
} finally {
    $writer.Dispose()
    $output.Dispose()
}

$written = Get-Item -LiteralPath $TargetIco
Write-Host ("Wrote    {0} ({1} frames: {2}, {3:N0} bytes)" -f `
    $written.FullName, $frames.Count, ($Sizes -join ', '), $written.Length)
