export async function storeResult(kv: KVNamespace, device: string, env: string): Promise<void> {
  const ts = String(Date.now()).padStart(13, "0");
  const id = crypto.randomUUID().slice(0, 8);
  await kv.put(`r:${device}:${ts}:${id}`, env, { expirationTtl: 604800 });
}

export async function readInbox(
  kv: KVNamespace, device: string, since: number
): Promise<string[]> {
  const list = await kv.list({ prefix: `r:${device}:`, limit: 200 });
  const out: string[] = [];
  for (const k of list.keys) {
    const ts = parseInt(k.name.split(":")[2] ?? "0", 10);
    if (ts <= since) continue;
    const v = await kv.get(k.name);
    if (v) out.push(v);
  }
  return out;
}
