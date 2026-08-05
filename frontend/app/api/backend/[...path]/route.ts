import { auth } from "@/lib/auth";

const backendBaseUrl =
  process.env.BACKEND_URL ?? process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

const FORWARDED_RESPONSE_HEADERS = [
  "content-type",
  "content-disposition",
  "content-length",
  "content-security-policy",
  "x-content-type-options",
];

function isAllowed(segments: string[]): boolean {
  if (segments.length < 2 || segments[0] !== "api") {
    return false;
  }
  return !segments.some((segment) => segment === ".." || segment === "." || segment.includes("\\"));
}

async function proxy(
  request: Request,
  context: { params: Promise<{ path: string[] }> },
): Promise<Response> {
  const { path } = await context.params;
  if (!isAllowed(path)) {
    return new Response(JSON.stringify({ title: "Not found", status: 404 }), {
      status: 404,
      headers: { "content-type": "application/problem+json" },
    });
  }

  const session = await auth();
  const search = new URL(request.url).search;
  const target = `${backendBaseUrl}/${path.map(encodeURIComponent).join("/")}${search}`;

  const headers = new Headers();
  if (session?.accessToken) {
    headers.set("Authorization", `Bearer ${session.accessToken}`);
  }
  const contentType = request.headers.get("content-type");
  if (contentType) {
    headers.set("content-type", contentType);
  }
  const fingerprint = request.headers.get("x-device-fingerprint");
  if (fingerprint) {
    headers.set("X-Device-Fingerprint", fingerprint);
  }

  const hasBody = request.method !== "GET" && request.method !== "HEAD";
  const response = await fetch(target, {
    method: request.method,
    headers,
    body: hasBody ? await request.arrayBuffer() : undefined,
    cache: "no-store",
  });

  const responseHeaders = new Headers();
  for (const name of FORWARDED_RESPONSE_HEADERS) {
    const value = response.headers.get(name);
    if (value) {
      responseHeaders.set(name, value);
    }
  }
  responseHeaders.set("x-content-type-options", "nosniff");

  return new Response(response.body, { status: response.status, headers: responseHeaders });
}

export { proxy as GET, proxy as POST, proxy as PUT, proxy as PATCH, proxy as DELETE };
