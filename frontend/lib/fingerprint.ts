import FingerprintJS from "@fingerprintjs/fingerprintjs";

let pending: Promise<string | null> | undefined;

export function deviceFingerprint(): Promise<string | null> {
  if (typeof window === "undefined") {
    return Promise.resolve(null);
  }
  if (!pending) {
    pending = FingerprintJS.load()
      .then((agent) => agent.get())
      .then((result) => result.visitorId)
      .catch(() => null);
  }
  return pending;
}
