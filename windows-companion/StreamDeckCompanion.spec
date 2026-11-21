# PyInstaller specification for optional EXE packaging.
# This does not replace the existing run.bat development workflow.
from pathlib import Path

base = Path(SPECPATH)

a = Analysis(
    [str(base / "companion.py")],
    pathex=[str(base)],
    binaries=[],
    datas=[(str(base / "assets" / "streamdeck.ico"), "assets")],
    hiddenimports=[],
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=[],
    noarchive=False,
)
pyz = PYZ(a.pure)
exe = EXE(
    pyz,
    a.scripts,
    a.binaries,
    a.datas,
    [],
    name="StreamDeck-Windows-Companion",
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=True,
    console=True,
    icon=str(base / "assets" / "streamdeck.ico"),
)
