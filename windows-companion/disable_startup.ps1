$ErrorActionPreference = "Stop"
$startupDirectory = [Environment]::GetFolderPath("Startup")
$shortcutPath = Join-Path $startupDirectory "Stream Deck Companion.lnk"

if (Test-Path -LiteralPath $shortcutPath -PathType Leaf) {
    Remove-Item -LiteralPath $shortcutPath
}

Write-Output "Startup disabled for the current Windows user."
