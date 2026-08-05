import { cache } from "react";
import { auth } from "@/lib/auth";

const backendBaseUrl =
  process.env.BACKEND_URL ?? process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export const currentSession = cache(async () => auth());

export async function backendFetch(path: string, init?: RequestInit): Promise<Response> {
  const session = await currentSession();
  const headers = new Headers(init?.headers);
  if (session?.accessToken) {
    headers.set("Authorization", `Bearer ${session.accessToken}`);
  }
  return fetch(`${backendBaseUrl}${path}`, { ...init, headers, cache: "no-store" });
}

export type LoadFailure = "session" | "backend";

export async function classifyFailure(response: Response): Promise<LoadFailure> {
  if (response.status === 401 || response.status === 403) {
    return "session";
  }
  const session = await currentSession();
  return session?.error ? "session" : "backend";
}
