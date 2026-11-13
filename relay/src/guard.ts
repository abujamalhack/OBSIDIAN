const enc = new TextEncoder();

export async function hmac(secret: string, msg: string): Promise<string> {
  const key = await crypto.subtle.importKey(
    "raw", enc.encode(secret), { name: "HMAC", hash: "SHA-256" }, false, ["sign"]
  );
  const sig = await crypto.subtle.sign("HMAC", key, enc.encode(msg));
  return btoa(String.fromCharCode(...new Uint8Array(sig)))
    .replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

export async function ctEqual(a: string, b: string): Promise<boolean> {
  if (a.length !== b.length) return false;
  let r = 0;
  for (let i = 0; i < a.length; i++) r |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return r === 0;
}

export async function deriveBearer(apiKey: string, device: string): Promise<string> {
  return hmac(apiKey, `obsidian/bearer/v1/${device}`);
}

export async function authDevice(
  req: Request, keys: Record<string, string>, device: string
): Promise<boolean> {
  const apiKey = keys[device];
  if (!apiKey) return false;
  const got = (req.headers.get("Authorization") ?? "").replace(/^Bearer\s+/i, "");
  return ctEqual(got, await deriveBearer(apiKey, device));
}

export async function authOperator(req: Request, token: string): Promise<boolean> {
  const got = (req.headers.get("Authorization") ?? "").replace(/^Bearer\s+/i, "");
  return ctEqual(got, token);
}

export function withinSkew(t: number): boolean {
  return Math.abs(Date.now() / 1000 - t) < 300;
}

/** Advisory only — the device's persisted in_seq is authoritative. */
export async function checkSeq(kv: KVNamespace, device: string, seq: number): Promise<boolean> {
  const key = `seq:${device}`;
  const prev = parseInt((await kv.get(key)) ?? "0", 10);
  if (seq <= prev) return false;
  await kv.put(key, String(seq), { expirationTtl: 2592000 });
  return true;
}
