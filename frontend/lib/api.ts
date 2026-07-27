export const apiBaseUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export function apiUrl(path: string): string {
  return `${apiBaseUrl}${path.startsWith("/") ? path : `/${path}`}`;
}

export function proxyUrl(path: string): string {
  return `/api/backend${path.startsWith("/") ? path : `/${path}`}`;
}
