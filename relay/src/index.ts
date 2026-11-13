import type { Env } from "./env";
import { authDevice, authOperator, checkSeq, withinSkew } from "./guard";
import { readInbox, storeResult } from "./queue";
import { firstChat, tg } from "./telegram";

function json(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "content-type": "application/json" },
  });
}

function parseKeys(raw: string): Record<string, string> {
  try {
    const v = JSON.parse(raw);
    return typeof v === "object" && v !== null ? (v as Record<string, string>) : {};
  } catch {
    return {};
  }
}

export default {
  async fetch(req: Request, env: Env): Promise<Response> {
    const url = new URL(req.url);
    const path = url.pathname;

    if (path === "/health") return new Response("ok");

    // ---------- operator ----------
    if (path.startsWith("/op/")) {
      if (!(await authOperator(req, env.OP_TOKEN))) return json({ error: "auth" }, 401);
      return handleOperator(req, env, url);
    }

    // ---------- agent ----------
    const resultMatch = path.match(/^\/agent\/([0-9a-f]{8})\/result$/);
    if (resultMatch && req.method === "POST") {
      return handleResult(req, env, resultMatch[1]!);
    }

    // ---------- telegram webhook (optional mirror) ----------
    if (path === "/tg" && req.method === "POST") {
      return handleWebhook(req, env);
    }

    return json({ error: "not found" }, 404);
  },
};

async function handleOperator(req: Request, env: Env, url: URL): Promise<Response> {
  if (url.pathname === "/op/sessions" && req.method === "GET") {
    const keys = parseKeys(env.DEVICE_KEYS);
    const pubs = parseKeys(env.DEVICE_PUB);
    const sessions = Object.keys(keys).map((id) => ({
      id,
      pub: pubs[id] ?? null,
    }));
    return json({ sessions });
  }

  const sendMatch = url.pathname.match(/^\/op\/([0-9a-f]{8})\/send$/);
  if (sendMatch && req.method === "POST") {
    const body = (await req.json()) as { text?: string };
    if (!body.text) return json({ error: "text required" }, 400);
    const r = await tg(env, "sendMessage", {
      chat_id: firstChat(env),
      text: `OBS2.${body.text}`,
      disable_web_page_preview: true,
    });
    return json({ ok: r.ok, description: r.description }, r.ok ? 200 : 502);
  }

  const inboxMatch = url.pathname.match(/^\/op\/([0-9a-f]{8})\/inbox$/);
  if (inboxMatch && req.method === "GET") {
    const since = parseInt(url.searchParams.get("since") ?? "0", 10);
    const items = await readInbox(env.Q, inboxMatch[1]!, since);
    return json({ items, now: Date.now() });
  }

  return json({ error: "not found" }, 404);
}

async function handleResult(req: Request, env: Env, device: string): Promise<Response> {
  if (!(await authDevice(req, parseKeys(env.DEVICE_KEYS), device))) {
    return json({ error: "auth" }, 401);
  }

  const raw = await req.text();
  const max = parseInt(env.MAX_BODY, 10);
  if (raw.length > max) return json({ error: "too large" }, 413);

  let parsed: { v: number; d: string; s: number; t: number; k: string; c: string };
  try {
    parsed = JSON.parse(raw);
  } catch {
    return json({ error: "bad json" }, 400);
  }

  if (parsed.d !== device) return json({ error: "device mismatch" }, 400);
  if (!withinSkew(parsed.t)) return json({ error: "stale" }, 409);
  if (!(await checkSeq(env.Q, device, parsed.s))) return json({ error: "replay" }, 409);

  await storeResult(env.Q, device, raw);

  const chat = firstChat(env);
  if (parsed.k === "manifest") {
    await tg(env, "sendDocument", {
      chat_id: chat,
      document: new Blob([raw], { type: "application/octet-stream" }),
      caption: `manifest ${device} seq=${parsed.s}`,
    });
  } else {
    await tg(env, "sendMessage", { chat_id: chat, text: `R:${device}:${parsed.s}` });
  }

  return json({ ok: true });
}

async function handleWebhook(req: Request, env: Env): Promise<Response> {
  const body = (await req.json()) as {
    message?: { message_id: number; text?: string; from?: { is_bot?: boolean } };
  };
  const msg = body.message;
  if (!msg?.text || msg.from?.is_bot) return json({ ok: true });
  await tg(env, "sendMessage", {
    chat_id: firstChat(env),
    text: `↩ ${msg.text.slice(0, 200)}`,
    disable_web_page_preview: true,
  });
  return json({ ok: true });
                                          }
