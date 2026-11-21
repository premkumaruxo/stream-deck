@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0disable_startup.ps1"
if errorlevel 1 (
    echo Could not disable automatic startup.
    exit /b 1
)
echo Stream Deck Companion will no longer start automatically.
