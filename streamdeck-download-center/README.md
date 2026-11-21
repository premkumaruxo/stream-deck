# Stream Deck Download Center

This directory is reserved for the separate Download Center website. Keep all
website source and assets inside `streamdeck-download-center/`; do not move
website code into `android-app/` or `windows-companion/`.

This checkout did not contain a website implementation or a prior
`latest.json` release feed. A clearly marked placeholder feed now exists at the
repository root; no website framework or Windows release version was invented.

## Local preview

Serve this directory with any static server:

```powershell
python -m http.server 4173
```

Then open `http://127.0.0.1:4173/`. The page loads `latest.json` from the same
directory, so keep the website copy synchronized with the repository-root
release feed before publishing.

## Publishing

Upload the complete `streamdeck-download-center/` directory to a static HTTPS
host such as GitHub Pages, Netlify, or Cloudflare Pages. No build step or
application runtime is required. Production download URLs in `latest.json`
must use HTTPS and point directly to public release assets.

## Public release metadata

The repository-root `latest.json` is the canonical release feed. The Android
app reads its `android` object. Once GitHub owner/repository placeholders are
replaced, the public raw URL is:

```text
https://raw.githubusercontent.com/GITHUB_USERNAME/GITHUB_REPOSITORY/main/latest.json
```

The website should read this JSON for displayed release versions, notes, and
download links rather than hard-coding them. The Android download link must
point directly to the GitHub Release asset named `StreamDeck-Android.apk`.
Repository pages are not APK download URLs.

The current Windows companion exposes `PROTOCOL_VERSION` for its network
protocol, not a Windows application release version. Do not change or present
that protocol number as a downloadable Windows version. If Windows release
metadata already exists in a future deployment, retain its existing fields
when updating the shared feed; this checkout contains no Windows release feed
to migrate.

## Separation and security

- Keep site source files in this directory.
- Keep release metadata in the repository-root `latest.json` so the Android
  app and website can consume the same public source.
- Do not put signing keys, keystores, passwords, or GitHub tokens in website
  source or public assets.
- See [`../android-app/RELEASE_SETUP.md`](../android-app/RELEASE_SETUP.md) for
  local signing, Android release, and Update Center testing instructions.

