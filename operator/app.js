"use strict";

const S = {
  priv: null,
  privRaw: null,
  sessions: {},
  active: null,
  relay: "",
  opToken: "",
  since: 0,
};

const $ = (id) => document.getElementById(id);

function log(text, cls = "") {
  const el = document.createElement("div");
  el.className = cls ? `line-${cls}` : "";
  el.textContent = text;
  $("log").appendChild(el);
  $("log").scrollTop = $("log").scrollHeight;
}

function setStatus(text, cls = "") {
  $("status").textContent = text;
  $("status").style.color = cls === "bad" ? "var(--bad)" : cls === "ok" ? "var(--ok)" : "var(--dim)";
}

function persist() {
  localStorage.priv = S.privRaw ? OBS.B64U.enc(S.privRaw) : "";
  localStorage.sessions = JSON.stringify(S.sessions);
  localStorage.active = S.active ?? "";
  localStorage.relay = S.relay;
  localStorage.opToken = S.opToken;
}

function restore() {
  S.relay = localStorage.relay ?? "";
  S.opToken = localStorage.opToken ?? "";
  S.active = localStorage.active || null;
  try { S.sessions = JSON.parse(localStorage.sessions ?? "{}"); } catch { S.sessions = {}; }
  $("relay").value = S.relay;
  $("optoken").value = S.opToken;
  if (localStorage.priv) loadPriv(OBS.B64U.dec(localStorage.priv));
}

async function loadPriv(raw) {
  S.privRaw = raw;
  S.priv = await OBS.importPriv(raw);
  const pub = await crypto.subtle.exportKey("raw", await pubOf(raw));
  $("pubout").value = OBS.B64U.enc(new Uint8Array(pub));
  setStatus("identity loaded", "ok");
  render();
}

async function pubOf(raw) {
  const priv = await crypto.subtle.importKey("raw", raw, { name: "X25519" }, true, ["deriveBits"]);
  // WebCrypto has no direct private→public derivation; re-import as a pair is
  // not possible from raw, so the public key is stored alongside on generate.
  return { priv };
}

function render() {
  $("active").textContent = S.active ? `active → ${S.active}` : "no active session";
  const list = $("sessionlist");
  list.innerHTML = "";
  for (const [id, meta] of Object.entries(S.sessions)) {
    const li = document.createElement("li");
    li.textContent = `${id} ${meta.label ?? ""}${id === S.active ? " *" : ""}`;
    if (id === S.active) li.className = "active";
    li.onclick = () => { S.active = id; persist(); render(); log(`/use ${id}`, "ok"); };
    list.appendChild(li);
  }
}

async function api(path, init = {}) {
  const res = await fetch(S.relay + path, {
    ...init,
    headers: {
      ...(init.headers ?? {}),
      Authorization: `Bearer ${S.opToken}`,
      "content-type": "application/json",
    },
  });
  if (!res.ok) throw new Error(`${res.status} ${await res.text()}`);
  return res.json();
}

async function send(cmd) {
  if (!S.active) return log("no active session", "err");
  const meta = S.sessions[S.active];
  if (!meta?.pub) return log("session has no public key", "err");

  const seq = parseInt(localStorage[`seq.${S.active}`] ?? "0", 10) + 1;
  localStorage[`seq.${S.active}`] = String(seq);

  const env = await OBS.seal(S.priv, OBS.B64U.dec(meta.pub), S.active, seq, cmd);
  log(`> ${cmd}`, "send");
  const r = await api(`/op/${S.active}/send`, { method: "POST", body: JSON.stringify({ text: env }) });
  if (!r.ok) log(`! ${r.description}`, "err");
}

async function pull() {
  if (!S.active) return log("no active session", "err");
  const meta = S.sessions[S.active];
  const data = await api(`/op/${S.active}/inbox?since=${S.since}`);
  for (const env of data.items) {
    try {
      const text = await OBS.open(S.priv, OBS.B64U.dec(meta.pub), env);
      log(`< ${text}`, "recv");
      const t = JSON.parse(env).t;
      if (t > S.since) S.since = t;
    } catch (e) {
      log(`! decrypt ${e.message}`, "err");
    }
  }
  if (data.items.length) log(`(${data.items.length} item(s))`, "ok");
}

async function refresh() {
  const data = await api("/op/sessions");
  for (const s of data.sessions) {
    S.sessions[s.id] = { pub: s.pub, label: s.id };
  }
  persist();
  render();
  log(`sessions: ${data.sessions.length}`, "ok");
}

async function buildReport() {
  const lines = [...$("log").children].map((d) => d.textContent);
  const md = ["# OBSIDIAN console transcript", "", "```", ...lines, "```"].join("\n");
  $("out").textContent = md;
  await navigator.clipboard?.writeText(md).catch(() => {});
  log("report built and copied", "ok");
}

function wire() {
  $("gen").onclick = async () => {
    const pair = await crypto.subtle.generateKey({ name: "X25519" }, true, ["deriveBits"]);
    const raw = new Uint8Array(await crypto.subtle.exportKey("raw", pair.privateKey));
    const pub = new Uint8Array(await crypto.subtle.exportKey("raw", pair.publicKey));
    await loadPriv(raw);
    $("pubout").value = OBS.B64U.enc(pub);
    log("identity generated — export the public key into enroll.py", "ok");
  };

  $("export").onclick = () => {
    if (!S.privRaw) return log("no identity", "err");
    $("pubout").value = OBS.B64U.enc(S.privRaw);
    $("pubout").select();
    log("private key shown — store it offline", "ok");
  };

  $("import").onclick = async () => {
    const v = prompt("paste private key (base64url)");
    if (v) await loadPriv(OBS.B64U.dec(v.trim()));
  };

  $("savecfg").onclick = () => {
    S.relay = $("relay").value.trim().replace(/\/$/, "");
    S.opToken = $("optoken").value.trim();
    persist();
    log("config saved", "ok");
  };

  $("refresh").onclick = () => refresh().catch((e) => log(`! ${e.message}`, "err"));
  $("pull").onclick = () => pull().catch((e) => log(`! ${e.message}`, "err"));
  $("report").onclick = () => buildReport().catch((e) => log(`! ${e.message}`, "err"));

  $("sendform").onsubmit = (ev) => {
    ev.preventDefault();
    const cmd = $("cmd").value.trim();
    if (!cmd) return;
    $("cmd").value = "";
    send(cmd).catch((e) => log(`! ${e.message}`, "err"));
  };
}

restore();
wire();
render();
