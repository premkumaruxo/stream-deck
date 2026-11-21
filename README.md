# Stream Deck — PC Control Studio

A native Android control deck and a paired Windows WebSocket companion. Saved profiles and buttons remain on-device and the deck remains usable while the PC is offline.

## Android screens and capabilities

- **Home:** PC connection status, active profile, live quick actions, and shortcuts to deck, profiles, layouts, and controls.
- **Deck:** portrait/landscape responsive grid with persisted per-profile layouts and button spans, press feedback, edit mode, long-press actions, drag-to-reorder, duplicate, delete, and image-backed icons.
- **Button designer:** live preview and persistent title, subtitle, image/emoji, colors, border, radius, shadow, typography, grid span, haptic/animation, confirmation, and action settings.
- **Profiles:** create, rename, duplicate, select, and delete independently stored decks.
- **Layouts:** 2×2 through 8×5 presets and custom 2–12 row/column grids.
- **Add Control / Controls:** keyboard, mouse, application, media, system, web, custom-shortcut, and macro shortcuts are added to the active profile and execute through the paired companion.
- **Macros:** persist ordered allowlisted actions and waits, edit/reorder/remove steps, and add saved macros as deck buttons.
- **Settings:** pair/manage the PC connection and import/export JSON backups.
- **App Update:** check GitHub release metadata, review release notes, download
  and verify a release APK, then hand installation to Android.

Room schema migration preserves existing profile/button data while adding the appearance and span fields. Pairing tokens remain encrypted with Android Keystore AES-GCM. The companion accepts only allowlisted actions and HTTP(S) URLs.

Still outside this implementation: plugins, automatic profile switching based on the active Windows application, custom executable paths, arbitrary shell commands, Windows Wi-Fi/Bluetooth/brightness/audio-device controls, and a persistent Android background service.

## Requirements

- Android Studio or the included Gradle 8.9 wrapper, JDK 17, Android SDK Platform 35, and Android SDK Build Tools 34.0.0.
- Pinned Android build versions: Android Gradle Plugin 8.7.3; Kotlin and Compose compiler plugin 2.0.21; KSP 2.0.21-1.0.28; Compose BOM 2024.12.01. AGP 8.7 requires Gradle 8.9 and JDK 17, and supports API 35.
- Android 10 or later.
- Windows 10/11, Python 3.10+, and Android/PC on the same trusted local network.
- Allow the companion through Windows Firewall on **Private networks only**.

## Android setup

Open the project root in Android Studio and sync Gradle, then run the `android-app` configuration on an Android 10+ device. Set `JAVA_HOME` to JDK 17 and install Android SDK Platform 35 and Build Tools 34.0.0 in SDK Manager. The checked-in wrapper downloads the pinned Gradle 8.9 distribution and verifies its SHA-256 checksum. Build the debug APK from the project root with:

```powershell
.\gradlew.bat :android-app:assembleDebug
```

The APK is at `android-app\build\outputs\apk\debug\android-app-debug.apk`. The first run creates a persistent Video Editing profile with example shortcut and application buttons. Profiles, layouts, buttons, and macros are stored in Room.

## Android updates

The Android updater reads the root `latest.json`. Set `GITHUB_USERNAME` and
`GITHUB_REPOSITORY` in `UpdateConfig.kt` before publishing; until then update
checks remain disabled and the JSON download URL is only a placeholder.
Release signing, version increments, GitHub Release assets, and Update Center
testing are documented in [android-app/RELEASE_SETUP.md](./android-app/RELEASE_SETUP.md).

## Download Center website

The separate website belongs in [streamdeck-download-center](./streamdeck-download-center/README.md).
This checkout contains setup documentation there, not a website implementation.

## Windows companion setup

From the project root, start the Windows companion with:

```powershell
.\windows-companion\run.bat
```

The script creates a local `.venv`, installs the pinned Python dependencies into it, and starts the companion on port 8765. Keep the console open; a new device requires an explicit `y` confirmation in that console. Pairing tokens are stored under `%APPDATA%\StreamDeckCompanion\devices.json`.

The companion binds to the local network to accept WebSocket connections and broadcasts its hostname, port, stable companion ID, and protocol version for discovery. Android listens for the existing UDP discovery broadcast and automatically connects/authenticates with the saved pairing token; manual IP entry remains available. Windows may show a firewall prompt; allow private-network access only. Set a different port with `STREAMDECK_PORT` before starting the companion.

To start the companion automatically after the current Windows user signs in, run `windows-companion\enable_startup.bat`. This creates a Startup-folder shortcut for the existing launcher and does not require administrator privileges. Run `windows-companion\disable_startup.bat` to remove it. Pair a phone once from the normal visible `run.bat` console before enabling startup; first-time pairing still requires local confirmation. The startup launcher opens minimized. `run.bat` remains the manual fallback.

## Connect and pair

On the same network, open the app and it will search for the companion automatically. On first connection, approve the Android device in the Windows companion console. The app receives and stores the per-device token encrypted with an Android Keystore AES-GCM key. Use **Settings → PC connection → Forget PC** to clear the saved endpoint and token; manual IP entry remains available.

Discovery advertisements use UDP port 39394 and the existing WebSocket protocol uses port 8765. Automatic reconnect retries with exponential backoff and restarts discovery when the active Android network changes or the app returns to the foreground. Configure the IP manually if the router or Windows Firewall blocks UDP broadcast. A USB cable alone is not a network route; USB connectivity is attempted only when Android exposes a matching USB/RNDIS network interface.

## Profiles, buttons, and controls

- **Profiles** creates, renames, duplicates, selects, and removes local profiles; each profile owns its layout and buttons.
- Tap a deck button to execute its configured action. **Edit** mode and long-press expose edit, resize, move, duplicate, and delete; long-press-drag reorders buttons.
- **Add control** provides common shortcuts and actions, or use **Design button** for a custom button.
- **Macros** saves an ordered action/delay sequence that can be added to a deck.
- **Layouts** immediately changes the active profile's grid.
- **Controls** exposes allowlisted keyboard, mouse, media, and Windows system actions.
- Supported launch aliases: Premiere Pro, Photoshop, After Effects, DaVinci Resolve, OBS, Chrome, and File Explorer. The companion resolves standard Windows install directories or PATH and returns a command error if an app isn't found.
- Export and import use Android's document picker. Restore adds imported profiles instead of overwriting existing data.

## Security and platform limitations

The companion prompts for local confirmation before pairing, issues random device tokens, authenticates paired connections, and validates every action against a strict allowlist. It never accepts shell commands or arbitrary executable paths. Shutdown/restart, Wi-Fi/Bluetooth toggles, PC brightness controls, and custom shell execution are intentionally not exposed. The local WebSocket transport is not encrypted; use it only on a trusted private LAN. For untrusted networks, use a VPN.

Android cannot independently control the Windows PC: actions require the paired companion. The app does not request administrator privileges. Firewall/network isolation can prevent discovery or connection; manual IP entry is provided.

## Troubleshooting

- **Disconnected:** Confirm both devices are on the same LAN, verify that the companion is running, and allow inbound TCP 8765 and UDP broadcast 39394 on the Windows private-network firewall.
- **Pairing declined:** Reconnect with an empty token and approve the request at the companion console.
- **Token rejected:** Clear the app token and pair again; paired-device tokens are in `%APPDATA%\StreamDeckCompanion\devices.json`.
- **Shortcut or app did not run:** Check that the target window is interactive and the application is installed. Some protected applications reject simulated input.
- **Dependency/build errors:** Use JDK 17, Android SDK Platform 35, Android SDK Build Tools 34.0.0, the included Gradle 8.9 wrapper, and Python 3.10+.

Run the companion's protocol validation and local WebSocket pairing/command round-trip tests after installing its requirements:

```powershell
cd windows-companion
.venv\Scripts\python.exe -m unittest -v
```
