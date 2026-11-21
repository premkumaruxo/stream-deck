@echo off
setlocal
set "ROOT=%~dp0"
title Stream Deck Companion
if not exist "%ROOT%.venv\Scripts\python.exe" goto create_venv
goto install

:create_venv
where py >nul 2>nul
if not errorlevel 1 goto use_launcher
where python >nul 2>nul
if errorlevel 1 (
    echo Python 3.10 or later is required. Install Python and run this file again.
    exit /b 1
)
python -m venv "%ROOT%.venv"
if errorlevel 1 goto venv_error
goto install

:use_launcher
py -3 -m venv "%ROOT%.venv"
if errorlevel 1 goto venv_error

:install
"%ROOT%.venv\Scripts\python.exe" -c "import websockets, pyautogui" >nul 2>nul
if not errorlevel 1 goto start
"%ROOT%.venv\Scripts\python.exe" -m pip install --disable-pip-version-check -r "%ROOT%requirements.txt"
if errorlevel 1 exit /b 1
:start
"%ROOT%.venv\Scripts\python.exe" "%ROOT%companion.py"
exit /b %errorlevel%

:venv_error
echo Could not create the Python environment. Install Python 3.10 or later and try again.
exit /b 1
