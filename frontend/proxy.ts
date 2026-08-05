import { NextResponse } from "next/server";

const backendBaseUrl =
  process.env.BACKEND_URL ?? process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

const TIMEOUT_MS = 2000;
const TTL_MS = 60_000;
const RETRY_MS = 10_000;

type Stack = { server: string; poweredBy: string; runtime: string };

let cached: Stack | null = null;
let cachedAt = 0;
let inFlight: Promise<Stack | null> | null = null;

async function load(): Promise<Stack | null> {
  try {
    const response = await fetch(`${backendBaseUrl}/api/stack`, {
      cache: "no-store",
      signal: AbortSignal.timeout(TIMEOUT_MS),
    });
    if (!response.ok) {
      return null;
    }
    const body = await response.json();
    if (!body?.server || !body?.poweredBy || !body?.runtime) {
      return null;
    }
    return { server: body.server, poweredBy: body.poweredBy, runtime: body.runtime };
  } catch {
    return null;
  }
}

async function stack(): Promise<Stack | null> {
  const age = Date.now() - cachedAt;
  if (cached && age < TTL_MS) {
    return cached;
  }
  if (!cached && age < RETRY_MS) {
    return null;
  }
  if (!inFlight) {
    inFlight = load()
      .then((resolved) => {
        cachedAt = Date.now();
        if (resolved) {
          cached = resolved;
        }
        return resolved;
      })
      .finally(() => {
        inFlight = null;
      });
  }
  return inFlight;
}

export async function proxy() {
  const response = NextResponse.next();
  const resolved = await stack();
  if (resolved) {
    response.headers.set("Server", resolved.server);
    response.headers.set("X-Powered-By", resolved.poweredBy);
    response.headers.set("X-Runtime", resolved.runtime);
  }
  response.headers.set("X-Content-Type-Options", "nosniff");
  response.headers.set("Referrer-Policy", "strict-origin-when-cross-origin");
  response.headers.set("X-Frame-Options", "DENY");
  return response;
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico).*)"],
};
