@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0enable_startup.ps1"
if errorlevel 1 (
    echo Could not enable automatic startup.
    exit /b 1
)
echo Stream Deck Companion will start when you sign in to Windows.
