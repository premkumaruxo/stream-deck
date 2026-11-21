$ErrorActionPreference = "Stop"
$startupDirectory = [Environment]::GetFolderPath("Startup")
$shortcutPath = Join-Path $startupDirectory "Stream Deck Companion.lnk"
$companionScript = Join-Path $PSScriptRoot "run.bat"

if (-not (Test-Path -LiteralPath $companionScript -PathType Leaf)) {
    throw "The companion launcher was not found."
}

$shell = New-Object -ComObject WScript.Shell
$shortcut = $shell.CreateShortcut($shortcutPath)
$shortcut.TargetPath = $companionScript
$shortcut.WorkingDirectory = $PSScriptRoot
$shortcut.WindowStyle = 7
$shortcut.Description = "Start the Stream Deck Windows Companion at sign-in"
$shortcut.Save()

Write-Output "Startup enabled for the current Windows user."
