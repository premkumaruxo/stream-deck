# Android release setup

This guide separates **local development** from **production releases**. The
current app identity and version are:

- Application ID: `com.streamdeck.control` (keep this unchanged for updates)
- Version name: `1.0.0`
- Version code: `1`

The tracked root `gradle.properties` contains the current version values.
Gradle also accepts `-PSTREAMDECK_VERSION_NAME=...` and
`-PSTREAMDECK_VERSION_CODE=...` for one-off builds.

## Local development

Build and install the debug APK during development:

```powershell
.\gradlew.bat :android-app:assembleDebug
```

The debug APK is signed with Android's development key. It is not a production
release and should not be uploaded as an update. Update checks remain disabled
until real GitHub details are configured in
`src/main/java/com/streamdeck/control/update/UpdateConfig.kt`.

## Production release

### A. Create the release keystore

Use the JDK 17 `keytool` and save the file outside the project directory:

```powershell
keytool -genkeypair -v -keystore "$HOME\streamdeck-release.jks" `
  -alias streamdeck-release -keyalg RSA -keysize 4096 -validity 10000
```

`keytool` prompts for passwords and certificate details. Keep the keystore and
passwords in a secure backup. Losing the key prevents updates from installing
over releases signed with it. Never commit or send the keystore to the website.

### B. Configure Gradle signing credentials locally

The Android Gradle configuration reads these four names from environment
variables or Gradle properties:

```text
STREAMDECK_RELEASE_STORE_FILE
STREAMDECK_RELEASE_STORE_PASSWORD
STREAMDECK_RELEASE_KEY_ALIAS
STREAMDECK_RELEASE_KEY_PASSWORD
```

Recommended for a local Windows machine: put them in the user-level file
`%USERPROFILE%\.gradle\gradle.properties` (create the `.gradle` directory if
needed). Use the actual local path, alias, and passwords; never put real
passwords in the project-level `gradle.properties`.

```properties
STREAMDECK_RELEASE_STORE_FILE=C:/Users/your-name/streamdeck-release.jks
STREAMDECK_RELEASE_STORE_PASSWORD=replace-locally
STREAMDECK_RELEASE_KEY_ALIAS=streamdeck-release
STREAMDECK_RELEASE_KEY_PASSWORD=replace-locally
```

Alternatively, set the same names as environment variables in a protected
local or CI environment. Do not print them in logs. The repository ignores
keystore files and common local signing/secrets property files. If credentials
are absent, Gradle can still build an **unsigned** release for configuration
checks; it is not installable/publishable as a production update. Partial
signing configuration fails the build.

### C. Build and verify the signed release APK

Before each release, update the version values in the root
`gradle.properties`. Then build:

```powershell
.\gradlew.bat :android-app:signingReport
.\gradlew.bat :android-app:assembleRelease
```

The signed output is normally
`android-app\build\outputs\apk\release\android-app-release.apk`. Without
credentials Gradle produces `android-app-release-unsigned.apk`; do not publish
it. Verify the certificate before publishing:

```powershell
apksigner verify --verbose --print-certs .\android-app\build\outputs\apk\release\android-app-release.apk
```

Use the same release keystore for every update. Android verifies that the new
APK has the same package and signing identity; the updater also requires a
higher numeric `versionCode`.

### D. Create a GitHub Release

Create a public release in the repository configured in `UpdateConfig.kt`.
Use a tag matching the version, such as `v1.1.0`.

### E. Upload the APK

Upload the **signed** APK as the exact asset name:

```text
StreamDeck-Android.apk
```

The updater accepts a direct GitHub Release asset URL, for example the URL
shape below. Replace every placeholder with the real owner, repository, tag,
and asset name; do not use the GitHub repository page URL:

```text
https://github.com/GITHUB_USERNAME/GITHUB_REPOSITORY/releases/download/v1.1.0/StreamDeck-Android.apk
```

### F. Update the public metadata

The root `latest.json` is the canonical public metadata file. Replace its
`GITHUB_RELEASE_APK_URL` placeholder with the direct asset URL and commit the
file to the configured repository's `main` branch. The Android app reads the
`android` object; release notes are optional and can contain multiple entries.

For example, after a real `v1.1.0` release, replace the placeholders using the
values actually built and uploaded:

```json
{
  "android": {
    "version": "1.1.0",
    "versionCode": 2,
    "downloadUrl": "https://github.com/GITHUB_USERNAME/GITHUB_REPOSITORY/releases/download/v1.1.0/StreamDeck-Android.apk",
    "releaseNotes": [
      "Summarize the changes in this release"
    ],
    "sha256": "",
    "mandatory": false
  }
}
```

Keep `sha256` empty only when checksum verification is intentionally not
configured. The app will explain this and still verify the APK package,
version, and signing certificate.

Generate the APK checksum:

```powershell
Get-FileHash .\android-app\build\outputs\apk\release\android-app-release.apk -Algorithm SHA256
```

Copy the 64-character hash into `android.sha256`. If the checksum is left empty,
the app clearly reports that checksum verification is not configured; it still
checks the package ID, version code, and signing certificate. Do not invent a
hash.

### G. Increment version values

Update both values in the root `gradle.properties` for every production
release. For example:

| Version name | Version code |
| --- | ---: |
| `1.0.0` | `1` |
| `1.1.0` | `2` |
| `1.2.0` | `3` |

Never reuse or lower a published version code. The current values are not
automatically changed by this setup.

### H. Test the Update Center

1. Replace `GITHUB_USERNAME` and `GITHUB_REPOSITORY` in `UpdateConfig.kt`.
   Until then, the metadata endpoint is `null` and automatic network checks
   stay disabled.
2. Publish the release APK and matching root `latest.json` to a public GitHub
   repository.
3. Build/install the signed release on an Android 10+ device. A debug install
   cannot accept an APK signed by the production key.
4. Open **Settings → App Update → Check for Updates**. Confirm the current and
   latest version codes, release notes, download progress, and installation
   handoff.
5. If SHA-256 is present, test that the correct APK verifies. Android's package
   installer still asks the user to confirm installation; on Android 8+ the
   user may first need to allow **Install unknown apps** for Stream Deck.
6. Confirm the app updates in place and retains profiles, buttons, settings,
   pairing, and Room data.

The update endpoint is
`https://raw.githubusercontent.com/GITHUB_USERNAME/GITHUB_REPOSITORY/main/latest.json`.
No GitHub token is required for public metadata/releases. Do not add tokens,
keystores, signing keys, or passwords to app source, website files, or Git.

