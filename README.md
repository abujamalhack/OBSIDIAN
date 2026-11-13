# OBSIDIAN

Advanced Android red-team framework with an encrypted, operator-agnostic
Telegram command channel. Device-owner provisioning, silent capture,
full audit chain, differential misconfiguration hunting.

## What it actually does

- Device-owner provisioning: silent, permanent grants for camera, mic,
  background location, SMS. No dialogs, no revocation.
- Silent capture: camera (front/rear/burst/stream/clip), microphone,
  GPS once/track/history, MediaProjection frames.
- Collection: SMS OTPs, notifications, contacts, call log, calendar,
  screen tree via accessibility, cookies/storage via headless WebView.
- Knox surface enumeration through reflection — builds without a partner key.
- Differential misconfiguration hunt with NVD/OSV dedup before anything
  is labelled a candidate.
- End-to-end encryption: X25519 + HKDF-SHA256 + AES-256-GCM, header bound
  as AAD. Telegram and Cloudflare see ciphertext only.
- Hash-chained audit log with `verify`.

## What it cannot do

- MediaProjection consent: one tap per session. Universal, no fix.
- `FLAG_SECURE` windows capture black.
- Other apps' private storage without root.
- System CA store without Magisk.
- MIUI/EMUI/ColorOS autostart without a user tap.

## Quickstart

1. `docs/00-INSTALL.md` — full procedure.
2. `docs/01-CONFIGURATION.md` — every secret and where it goes.
3. `docs/06-RUNBOOK.md` — commands.

## Stack

Kotlin 2.0.21 / AGP 8.7.3 / Gradle 8.11.1 / minSdk 29 / targetSdk 35.
Cloudflare Workers + KV. WebCrypto operator console. No paid tiers.

## Licence

MIT.
