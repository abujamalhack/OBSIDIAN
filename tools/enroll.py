#!/usr/bin/env python3
import argparse
import base64
import hashlib
import hmac
import json
import secrets
import urllib.parse


def b64u(raw: bytes) -> str:
    return base64.urlsafe_b64encode(raw).rstrip(b"=").decode()


def bearer(api_key: str, device: str) -> str:
    msg = f"obsidian/bearer/v1/{device}".encode()
    return b64u(hmac.new(api_key.encode(), msg, hashlib.sha256).digest())


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--relay", required=True)
    ap.add_argument("--oppub", required=True)
    ap.add_argument("--chat", required=True)
    ap.add_argument("--label", default="")
    args = ap.parse_args()

    device = secrets.token_hex(4)
    api_key = b64u(secrets.token_bytes(32))
    derived = bearer(api_key, device)

    params = {
        "relay": args.relay.rstrip("/"),
        "dev": device,
        "key": api_key,
        "oppub": args.oppub,
        "chat": args.chat,
    }

    print(f"deviceId : {device}")
    print(f"apiKey   : {api_key}")
    print(f"bearer   : {derived}")
    print()
    print("add to wrangler.toml [vars], then redeploy:")
    print(f'  DEVICE_KEYS = \'{{"{device}":"{api_key}"}}\'')
    print(f'  DEVICE_PUB  = \'{{"{device}":"{args.oppub}"}}\'')
    print()
    print("enrollment URI:")
    print("obsidian://enroll?" + urllib.parse.urlencode(params))
    print()
    print("adb:")
    print('  adb shell am start -d "' + "obsidian://enroll?" + urllib.parse.urlencode(params) + '"')
    print()
    print("console session entry (paste into devtools):")
    print(json.dumps({device: {"pub": args.oppub, "label": args.label or device}}))


if __name__ == "__main__":
    main()
