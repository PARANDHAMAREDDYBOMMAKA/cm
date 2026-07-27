import { auth } from "@/lib/auth";

const backendBaseUrl =
  process.env.BACKEND_URL ?? process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export async function backendFetch(path: string, init?: RequestInit): Promise<Response> {
  const session = await auth();
  const headers = new Headers(init?.headers);
  if (session?.accessToken) {
    headers.set("Authorization", `Bearer ${session.accessToken}`);
  }
  return fetch(`${backendBaseUrl}${path}`, { ...init, headers, cache: "no-store" });
}
