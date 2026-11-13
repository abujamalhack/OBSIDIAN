# Installation

## Prerequisites

- Linux/macOS host with `adb` (Android Platform Tools 34+)
- JDK 17
- Android SDK 35 + build-tools 35.0.0
- Node.js 20 (relay + tooling)
- Python 3.11 (enrollment)
- A Cloudflare account (free tier)
- A Telegram account

## 1. Bot

1. Message `@BotFather` → `/newbot` → name it → copy the token.
2. `/setprivacy` → Disable (so the bot sees all group messages if you use one).
3. Do **not** call `setWebhook`. The device polls `getUpdates` directly;
   a webhook would cause HTTP 409 and break the channel.
4. Send any message to your new bot, then open
   `https://api.telegram.org/bot<TOKEN>/getUpdates` and copy
   `result[0].message.chat.id`. That is `OPERATOR_CHAT`.

## 2. Relay

```bash
cd relay
npm install
npx wrangler login
npx wrangler kv namespace create Q
# paste the returned id into wrangler.toml
