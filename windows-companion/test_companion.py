import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import Mock, patch
import websockets
import companion
from companion import discovery_payload, handler, load_companion_id, resolve_app_executable, valid_message


class ProtocolValidationTests(unittest.TestCase):
    def test_shared_protocol_matches_companion_ports_and_actions(self):
        protocol_path = Path(__file__).parent.parent / "shared-protocol" / "protocol.json"
        protocol = json.loads(protocol_path.read_text(encoding="utf-8"))
        self.assertEqual(protocol["protocol_version"], 1)
        self.assertEqual(protocol["websocket_port"], companion.PORT)
        self.assertEqual(protocol["discovery_port"], companion.DISCOVERY_PORT)
        self.assertEqual(
            set(protocol["allowed_actions"]["launch_app"]),
            set(companion.APP_ALIASES),
        )

    def test_discovery_advertisement_includes_stable_companion_identity(self):
        with tempfile.TemporaryDirectory() as directory:
            app_dir = Path(directory)
            id_file = app_dir / "companion_id"
            with (
                patch.object(companion, "APP_DIR", app_dir),
                patch.object(companion, "COMPANION_ID_FILE", id_file),
                patch("companion.socket.gethostname", return_value="Test PC"),
            ):
                first = discovery_payload()
                second = discovery_payload()
                self.assertEqual(first["type"], "streamdeck_pc")
                self.assertEqual(first["name"], "Test PC")
                self.assertEqual(first["port"], companion.PORT)
                self.assertEqual(first["protocol_version"], companion.PROTOCOL_VERSION)
                self.assertEqual(first["companion_id"], second["companion_id"])
                self.assertEqual(first["companion_id"], load_companion_id())
                self.assertTrue(id_file.exists())

    def test_accepts_allowlisted_shortcuts_and_controls(self):
        self.assertTrue(valid_message({"type": "keyboard", "key": "CTRL+SHIFT+S"}))
        self.assertTrue(valid_message({"type": "launch_app", "key": "premiere_pro"}))
        self.assertTrue(valid_message({"type": "system", "key": "screenshot"}))

    def test_rejects_shells_and_unrecognized_actions(self):
        self.assertFalse(valid_message({"type": "shell", "command": "whoami"}))
        self.assertFalse(valid_message({"type": "launch_app", "key": "C:\\evil.exe"}))
        self.assertFalse(valid_message({"type": "system", "key": "shutdown"}))
        self.assertFalse(valid_message({"type": "keyboard", "key": "CTRL+;"}))
        self.assertTrue(valid_message({"type": "open_url", "key": "https://example.com"}))
        self.assertFalse(valid_message({"type": "open_url", "key": "file:///C:/Windows/win.ini"}))
        self.assertFalse(valid_message({"type": "open_url", "key": "https://user:pass@example.com"}))

    def test_rejects_malformed_payloads(self):
        self.assertFalse(valid_message(None))
        self.assertFalse(valid_message([]))
        self.assertFalse(valid_message({"type": "keyboard", "key": 7}))

    def test_resolves_installed_application_from_standard_directory(self):
        with tempfile.TemporaryDirectory() as directory:
            install = Path(directory) / "Adobe" / "Adobe Premiere Pro 2025"
            install.mkdir(parents=True)
            executable = install / "Adobe Premiere Pro.exe"
            executable.touch()
            with (
                patch("companion.shutil.which", return_value=None),
                patch.dict("companion.os.environ", {"PROGRAMFILES": directory}, clear=True),
            ):
                self.assertEqual(resolve_app_executable("premiere_pro"), executable)


class WebSocketPairingTests(unittest.IsolatedAsyncioTestCase):
    async def test_pair_authenticate_and_receive_shortcut(self):
        with tempfile.TemporaryDirectory() as directory:
            app_dir = Path(directory)
            devices_file = app_dir / "devices.json"
            action = Mock()
            with (
                patch.object(companion, "APP_DIR", app_dir),
                patch.object(companion, "DEVICES_FILE", devices_file),
                patch("builtins.input", return_value="y"),
                patch.object(companion, "execute_action", action),
            ):
                async with websockets.serve(handler, "127.0.0.1", 0) as server:
                    port = server.sockets[0].getsockname()[1]
                    async with websockets.connect(f"ws://127.0.0.1:{port}") as client:
                        await client.send(json.dumps({"type": "pair_request", "device": "Test phone"}))
                        paired = json.loads(await client.recv())
                        self.assertEqual(paired["type"], "paired")
                        self.assertTrue(devices_file.exists())

                        await client.send(json.dumps({
                            "type": "authenticate",
                            "token": paired["token"],
                        }))
                        self.assertEqual(json.loads(await client.recv())["type"], "authenticated")
                        await client.send(json.dumps({
                            "type": "keyboard",
                            "key": "CTRL+S",
                            "request_id": "test-command-1",
                        }))
                        self.assertEqual(json.loads(await client.recv()), {
                            "type": "command_result",
                            "ok": True,
                            "request_id": "test-command-1",
                        })
                        action.assert_called_once_with({
                            "type": "keyboard",
                            "key": "CTRL+S",
                            "request_id": "test-command-1",
                        })


if __name__ == "__main__":
    unittest.main()
