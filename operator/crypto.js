"use strict";

const B64U = {
  enc(bytes) {
    let s = "";
    const arr = new Uint8Array(bytes);
    for (let i = 0; i < arr.length; i++) s += String.fromCharCode(arr[i]);
    return btoa(s).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
  },
  dec(str) {
    const pad = str.replace(/-/g, "+").replace(/_/g, "/");
    const raw = atob(pad + "=".repeat((4 - (pad.length % 4)) % 4));
    const out = new Uint8Array(raw.length);
    for (let i = 0; i < raw.length; i++) out[i] = raw.charCodeAt(i);
    return out;
  },
};

const ENC = new TextEncoder();
const DEC = new TextDecoder();

async function importPriv(raw) {
  return crypto.subtle.importKey("raw", raw, { name: "X25519" }, true, ["deriveBits"]);
}

async function importPub(raw) {
  return crypto.subtle.importKey("raw", raw, { name: "X25519" }, true, []);
}

async function shared(aPriv, bPub) {
  const bits = await crypto.subtle.deriveBits(
    { name: "X25519", public: bPub }, aPriv, 256
  );
  return new Uint8Array(bits);
}

async function ikmFor(priv, peerPub, ephPub) {
  const a = await shared(priv, peerPub);
  const b = await shared(priv, ephPub);
  const zero = (x) => x.every((v) => v === 0);
  if (zero(a) || zero(b)) throw new Error("low-order point");
  const out = new Uint8Array(64);
  out.set(a, 0);
  out.set(b, 32);
  return out;
}

async function hkdf(ikm, salt, info) {
  const key = await crypto.subtle.importKey("raw", ikm, "HKDF", false, ["deriveBits"]);
  const bits = await crypto.subtle.deriveBits(
    { name: "HKDF", hash: "SHA-256", salt, info }, key, 256
  );
  return new Uint8Array(bits);
}

function header(device, seq, ts, ephPub, nonce) {
  const parts = [
    ENC.encode("OBS2"), Uint8Array.of(1), ENC.encode(device),
    ENC.encode(String(seq)), ENC.encode(String(ts)), ephPub, nonce,
  ];
  const total = parts.reduce((n, p) => n + p.length, 0);
  const out = new Uint8Array(total);
  let off = 0;
  for (const p of parts) { out.set(p, off); off += p.length; }
  return out;
}

async function keyFor(ikm, hdr, device) {
  const salt = new Uint8Array(await crypto.subtle.digest("SHA-256", hdr));
  return hkdf(ikm, salt, ENC.encode("obsidian/v2/" + device));
}

async function seal(priv, devicePubRaw, device, seq, plain) {
  const peerPub = await importPub(devicePubRaw);
  const eph = await crypto.subtle.generateKey({ name: "X25519" }, true, ["deriveBits"]);
  const ephPub = new Uint8Array(await crypto.subtle.exportKey("raw", eph.publicKey));

  const ts = Math.floor(Date.now() / 1000);
  const nonce = crypto.getRandomValues(new Uint8Array(12));
  const hdr = header(device, seq, ts, ephPub, nonce);

  const key = await keyFor(await ikmFor(priv, peerPub, eph.publicKey), hdr, device);
  const aes = await crypto.subtle.importKey("raw", key, "AES-GCM", false, ["encrypt"]);
  const ct = new Uint8Array(await crypto.subtle.encrypt(
    { name: "AES-GCM", iv: nonce, additionalData: hdr }, aes, ENC.encode(plain)
  ));

  return JSON.stringify({
    v: 1, d: device, s: seq, t: ts, k: "msg",
    e: B64U.enc(ephPub), n: B64U.enc(nonce), c: B64U.enc(ct),
  });
}

async function open(priv, devicePubRaw, envelope) {
  const o = JSON.parse(envelope);
  if (o.v !== 1) throw new Error("version");
  const peerPub = await importPub(devicePubRaw);
  const ephPub = B64U.dec(o.e);
  const nonce = B64U.dec(o.n);
  const ephKey = await importPub(ephPub);
  const hdr = header(o.d, o.s, o.t, ephPub, nonce);

  const key = await keyFor(await ikmFor(priv, peerPub, ephKey), hdr, o.d);
  const aes = await crypto.subtle.importKey("raw", key, "AES-GCM", false, ["decrypt"]);
  const pt = await crypto.subtle.decrypt(
    { name: "AES-GCM", iv: nonce, additionalData: hdr }, aes, B64U.dec(o.c)
  );
  return DEC.decode(pt);
}

window.OBS = { B64U, importPriv, seal, open };
