import { NextResponse, type NextRequest } from "next/server";

const backendBaseUrl =
  process.env.BACKEND_URL ?? process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

const TIMEOUT_MS = 2000;

type Stack = { server: string; poweredBy: string; runtime: string };

async function stack(): Promise<Stack | null> {
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

export async function middleware(request: NextRequest) {
  const response = NextResponse.next();
  const resolved = await stack();
  if (resolved) {
    response.headers.set("Server", resolved.server);
    response.headers.set("X-Powered-By", resolved.poweredBy);
    response.headers.set("X-Runtime", resolved.runtime);
  }
  return response;
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico).*)"],
};
