export async function tg(env: { BOT_TOKEN: string }, method: string, payload: unknown) {
  const res = await fetch(`https://api.telegram.org/bot${env.BOT_TOKEN}/${method}`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(payload),
  });
  return (await res.json()) as { ok: boolean; description?: string };
}

export function firstChat(env: { OPERATOR_CHAT: string }): string {
  return env.OPERATOR_CHAT.split(",")[0]!.trim();
}
