"""Paired local-network WebSocket agent for Stream Deck Android."""

from __future__ import annotations

import asyncio
import json
import ipaddress
import urllib.parse
import os
import secrets
import socket
import subprocess
import sys
import threading
import time
import shutil
import uuid
from pathlib import Path
from typing import Any

import websockets
from websockets.server import WebSocketServerProtocol

PORT = int(os.environ.get("STREAMDECK_PORT", "8765"))
DISCOVERY_PORT = 39394
APP_DIR = Path(os.environ.get("APPDATA", str(Path.home()))) / "StreamDeckCompanion"
DEVICES_FILE = APP_DIR / "devices.json"
COMPANION_ID_FILE = APP_DIR / "companion_id"
PROTOCOL_VERSION = 1
ALLOWED_SHORTCUT_KEYS = {
    "CTRL", "ALT", "SHIFT", "WIN", "ENTER", "SPACE", "TAB", "ESC", "DELETE",
    "BACKSPACE", "HOME", "END", "UP", "DOWN", "LEFT", "RIGHT",
    *(f"F{i}" for i in range(1, 13)),
    *"ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789",
}

APP_ALIASES = {
    "premiere_pro": ("Adobe Premiere Pro", "Adobe Premiere Pro.exe"),
    "photoshop": ("Adobe Photoshop", "Photoshop.exe"),
    "after_effects": ("Adobe After Effects", "AfterFX.exe"),
    "resolve": ("DaVinci Resolve", "Resolve.exe"),
    "obs": ("OBS Studio", "obs64.exe"),
    "chrome": ("Google Chrome", "chrome.exe"),
    "file_explorer": ("File Explorer", "explorer.exe"),
}

APP_PATH_PATTERNS = {
    "premiere_pro": (
        ("PROGRAMFILES", "Adobe", "Adobe Premiere Pro*", "Adobe Premiere Pro.exe"),
        ("PROGRAMFILES(X86)", "Adobe", "Adobe Premiere Pro*", "Adobe Premiere Pro.exe"),
    ),
    "photoshop": (
        ("PROGRAMFILES", "Adobe", "Adobe Photoshop*", "Photoshop.exe"),
        ("PROGRAMFILES(X86)", "Adobe", "Adobe Photoshop*", "Photoshop.exe"),
    ),
    "after_effects": (
        ("PROGRAMFILES", "Adobe", "Adobe After Effects*", "Support Files", "AfterFX.exe"),
        ("PROGRAMFILES(X86)", "Adobe", "Adobe After Effects*", "Support Files", "AfterFX.exe"),
    ),
    "resolve": (
        ("PROGRAMFILES", "Blackmagic Design", "DaVinci Resolve", "Resolve.exe"),
        ("PROGRAMFILES(X86)", "Blackmagic Design", "DaVinci Resolve", "Resolve.exe"),
    ),
    "obs": (
        ("PROGRAMFILES", "obs-studio", "bin", "64bit", "obs64.exe"),
        ("PROGRAMFILES(X86)", "obs-studio", "bin", "64bit", "obs64.exe"),
        ("LOCALAPPDATA", "Programs", "obs-studio", "bin", "64bit", "obs64.exe"),
    ),
    "chrome": (
        ("PROGRAMFILES", "Google", "Chrome", "Application", "chrome.exe"),
        ("PROGRAMFILES(X86)", "Google", "Chrome", "Application", "chrome.exe"),
        ("LOCALAPPDATA", "Google", "Chrome", "Application", "chrome.exe"),
    ),
}


def set_windows_console_icon() -> None:
    if sys.platform != "win32":
        return
    icon_path = Path(__file__).resolve().parent / "assets" / "streamdeck.ico"
    if not icon_path.is_file():
        return
    try:
        import ctypes

        hwnd = ctypes.windll.kernel32.GetConsoleWindow()
        if not hwnd:
            return
        icon = ctypes.windll.user32.LoadImageW(
            None,
            str(icon_path),
            1,
            0,
            0,
            0x10,
        )
        if icon:
            ctypes.windll.user32.SendMessageW(hwnd, 0x80, 0, icon)
            ctypes.windll.user32.SendMessageW(hwnd, 0x80, 1, icon)
    except (AttributeError, OSError):
        pass


def resolve_app_executable(alias: str) -> Path:
    if alias not in APP_ALIASES:
        raise ValueError("Unsupported application")
    executable = APP_ALIASES[alias][1]
    located = shutil.which(executable)
    if located:
        return Path(located)
    for pattern in APP_PATH_PATTERNS.get(alias, ()):
        base = os.environ.get(pattern[0])
        if not base:
            continue
        root = Path(base)
        path_pattern = Path(*pattern[1:])
        candidate = next(root.glob(str(path_pattern)), None)
        if candidate is not None and candidate.is_file():
            return candidate
    raise FileNotFoundError(f"{APP_ALIASES[alias][0]} was not found in standard install locations")


def load_devices() -> dict[str, str]:
    try:
        data = json.loads(DEVICES_FILE.read_text(encoding="utf-8"))
        if not isinstance(data, dict) or not all(
            isinstance(name, str) and isinstance(token, str)
            for name, token in data.items()
        ):
            raise ValueError("Invalid paired-device database")
        return data
    except FileNotFoundError:
        return {}


def save_devices(devices: dict[str, str]) -> None:
    APP_DIR.mkdir(parents=True, exist_ok=True)
    temporary = DEVICES_FILE.with_suffix(".tmp")
    temporary.write_text(json.dumps(devices, indent=2), encoding="utf-8")
    temporary.replace(DEVICES_FILE)


def load_companion_id() -> str:
    try:
        value = COMPANION_ID_FILE.read_text(encoding="utf-8").strip()
        return str(uuid.UUID(value))
    except (FileNotFoundError, OSError, ValueError):
        APP_DIR.mkdir(parents=True, exist_ok=True)
        value = str(uuid.uuid4())
        temporary = COMPANION_ID_FILE.with_suffix(".tmp")
        temporary.write_text(value, encoding="utf-8")
        temporary.replace(COMPANION_ID_FILE)
        return value


def discovery_payload() -> dict[str, Any]:
    return {
        "type": "streamdeck_pc",
        "name": socket.gethostname(),
        "port": PORT,
        "companion_id": load_companion_id(),
        "protocol_version": PROTOCOL_VERSION,
    }


def valid_message(message: Any) -> bool:
    if not isinstance(message, dict):
        return False
    kind = message.get("type")
    if kind == "pair_request":
        return isinstance(message.get("device", "Android device"), str)
    if kind == "authenticate":
        return isinstance(message.get("token"), str) and len(message["token"]) >= 32
    if kind == "keyboard":
        key = message.get("key")
        if not isinstance(key, str):
            return False
        parts = key.upper().split("+")
        return 1 <= len(parts) <= 5 and all(
            part in ALLOWED_SHORTCUT_KEYS for part in parts
        )
    if kind == "mouse":
        return message.get("key") in {
            "left_click", "right_click", "double_click", "middle_click",
            "scroll_up", "scroll_down",
        }
    if kind == "media":
        return message.get("key") in {
            "play_pause", "previous", "next", "volume_up", "volume_down", "mute",
        }
    if kind == "system":
        return message.get("key") in {
            "screenshot", "lock", "task_manager", "open_settings", "file_explorer",
        }
    if kind == "launch_app":
        return message.get("key") in APP_ALIASES
    if kind == "open_url":
        value = message.get("key")
        if not isinstance(value, str) or len(value) > 2048:
            return False
        parsed = urllib.parse.urlparse(value)
        return parsed.scheme in {"http", "https"} and bool(parsed.hostname) and not parsed.username and not parsed.password
    return False


def execute_action(message: dict[str, Any]) -> None:
    import pyautogui

    kind = message["type"]
    key = message.get("key", "").lower()
    if kind == "keyboard":
        parts = message["key"].lower().split("+")
        pyautogui.hotkey(*parts)
    elif kind == "mouse":
        actions = {
            "left_click": pyautogui.click,
            "right_click": pyautogui.rightClick,
            "double_click": lambda: pyautogui.doubleClick(),
            "middle_click": lambda: pyautogui.middleClick(),
            "scroll_up": lambda: pyautogui.scroll(3),
            "scroll_down": lambda: pyautogui.scroll(-3),
        }
        actions[key]()
    elif kind == "media":
        keys = {
            "play_pause": 0xB3, "previous": 0xB1, "next": 0xB0,
            "volume_up": 0xAF, "volume_down": 0xAE, "mute": 0xAD,
        }
        import ctypes

        keybd_event = ctypes.windll.user32.keybd_event
        keybd_event(keys[key], 0, 0, 0)
        keybd_event(keys[key], 0, 0x0002, 0)
    elif kind == "launch_app":
        label, _ = APP_ALIASES[key]
        if key == "file_explorer":
            os.startfile("explorer.exe")  # Windows shell resolves the installed app.
        else:
            subprocess.Popen([str(resolve_app_executable(key))], shell=False)
        print(f"Launched {label}")
    elif kind == "open_url":
        import webbrowser

        webbrowser.open(message["key"], new=2)
    elif kind == "system":
        if key == "lock":
            ctypes = __import__("ctypes")
            ctypes.windll.user32.LockWorkStation()
        elif key == "screenshot":
            from datetime import datetime

            folder = Path.home() / "Pictures" / "Screenshots"
            folder.mkdir(parents=True, exist_ok=True)
            pyautogui.screenshot().save(folder / f"StreamDeck-{datetime.now():%Y%m%d-%H%M%S}.png")
        elif key == "task_manager":
            subprocess.Popen(["taskmgr.exe"], shell=False)
        elif key == "open_settings":
            os.startfile("ms-settings:")
        elif key == "file_explorer":
            os.startfile("explorer.exe")


async def handler(websocket: WebSocketServerProtocol, _path: str) -> None:
    address = websocket.remote_address[0] if websocket.remote_address else "unknown"
    try:
        peer = ipaddress.ip_address(address.split("%", 1)[0])
        if not (peer.is_private or peer.is_loopback):
            await websocket.close(code=1008, reason="Invalid peer")
            return
        devices = load_devices()
        authenticated = False
        async for raw in websocket:
            try:
                message = json.loads(raw)
            except (json.JSONDecodeError, TypeError):
                await websocket.send(json.dumps({"type": "error", "message": "Invalid JSON"}))
                continue
            if not isinstance(message, dict):
                await websocket.send(json.dumps({"type": "error", "message": "Invalid message"}))
                continue
            if message.get("type") == "authenticate":
                token = message.get("token")
                authenticated = isinstance(token, str) and secrets.compare_digest(
                    token, next((saved for saved in devices.values() if secrets.compare_digest(saved, token)), "")
                )
                await websocket.send(json.dumps(
                    {"type": "authenticated"} if authenticated
                    else {"type": "error", "message": "Pairing token rejected"}
                ))
                continue
            if not authenticated and message.get("type") == "pair_request":
                device_name = str(message.get("device", "Android device"))[:64]
                print(f"\nPairing request from {device_name} at {address}.")
                decision = await asyncio.to_thread(input, "Allow this device? [y/N] ")
                if decision.strip().lower() not in {"y", "yes"}:
                    await websocket.send(json.dumps({"type": "error", "message": "Pairing declined"}))
                    continue
                token = secrets.token_urlsafe(32)
                devices[device_name] = token
                save_devices(devices)
                authenticated = True
                await websocket.send(json.dumps({"type": "paired", "token": token}))
                print(f"Paired {device_name}. Keep its token private.")
                continue
            if not authenticated:
                await websocket.send(json.dumps({"type": "error", "message": "Pairing required"}))
                continue
            if not valid_message(message):
                await websocket.send(json.dumps({
                    "type": "error",
                    "message": "Unsupported or invalid action",
                    "request_id": message.get("request_id"),
                }))
                continue
            request_id = message.get("request_id")
            try:
                await asyncio.to_thread(execute_action, message)
                await websocket.send(json.dumps({
                    "type": "command_result",
                    "ok": True,
                    "request_id": request_id,
                }))
            except Exception as exc:
                print(f"Action failed: {type(exc).__name__}: {exc}")
                await websocket.send(json.dumps({
                    "type": "error",
                    "message": "PC action failed",
                    "request_id": request_id,
                }))
    except websockets.ConnectionClosed:
        pass
    except Exception as exc:
        print(f"Client handler error: {type(exc).__name__}: {exc}")


def discovery_broadcaster() -> None:
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
        payload = json.dumps(discovery_payload()).encode()
        while True:
            try:
                sock.sendto(payload, ("255.255.255.255", DISCOVERY_PORT))
            except OSError as exc:
                print(f"Discovery broadcast failed: {exc}")
            time.sleep(3)
    except OSError as exc:
        print(f"Discovery unavailable: {exc}")


async def main() -> None:
    if sys.platform != "win32":
        print("Warning: this companion is intended for Windows.")
    else:
        os.system("title Stream Deck Companion")
        set_windows_console_icon()
    if not 1 <= PORT <= 65535:
        raise ValueError("STREAMDECK_PORT must be between 1 and 65535")
    threading.Thread(target=discovery_broadcaster, daemon=True).start()
    print(f"Stream Deck companion listening on ws://0.0.0.0:{PORT}")
    print("Pairing requires local confirmation. Arbitrary shell commands are disabled.")
    async with websockets.serve(handler, "0.0.0.0", PORT, ping_interval=20, ping_timeout=20):
        await asyncio.Future()


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\nCompanion stopped.")
