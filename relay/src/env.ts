export interface Env {
  Q: KVNamespace;
  BOT_TOKEN: string;
  OP_TOKEN: string;
  OPERATOR_CHAT: string;
  DEVICE_KEYS: string;
  DEVICE_PUB: string;
  MAX_BODY: string;
}

export interface OpCtx {
  device: string;
  chat: string;
}
