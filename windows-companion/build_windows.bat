@echo off
setlocal
set "ROOT=%~dp0"
"%ROOT%.venv\Scripts\python.exe" -m pip show pyinstaller >nul 2>nul
if errorlevel 1 "%ROOT%.venv\Scripts\python.exe" -m pip install pyinstaller
if errorlevel 1 exit /b 1
"%ROOT%.venv\Scripts\python.exe" -m PyInstaller --noconfirm --clean "%ROOT%StreamDeckCompanion.spec"
exit /b %errorlevel%
