import type { NextConfig } from "next";

const backendUrl =
  process.env.BACKEND_URL ?? process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

async function backendServerHeader(): Promise<string | null> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 2000);
  try {
    const response = await fetch(`${backendUrl}/api/ping`, {
      cache: "no-store",
      signal: controller.signal,
    });
    return response.headers.get("server");
  } catch {
    return null;
  } finally {
    clearTimeout(timeout);
  }
}

const nextConfig: NextConfig = {
  async headers() {
    const server = await backendServerHeader();
    if (!server) {
      return [];
    }
    return [
      {
        source: "/:path*",
        headers: [{ key: "Server", value: server }],
      },
    ];
  },
};

export default nextConfig;
