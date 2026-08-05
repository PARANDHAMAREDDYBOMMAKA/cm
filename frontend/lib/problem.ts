export type Problem = {
  title?: string;
  detail?: string;
  status?: number;
  errors?: Record<string, string>;
};

export async function problemMessage(response: Response, fallback: string): Promise<string> {
  if (response.status === 401 || response.status === 403) {
    return "You are not allowed to do that. Your session may have expired — try signing in again.";
  }
  if (response.status === 429) {
    return "Too many requests. Wait a moment and try again.";
  }
  try {
    const body: Problem = await response.clone().json();
    if (body.errors && typeof body.errors === "object") {
      const fields = Object.entries(body.errors).map(([field, message]) => `${field}: ${message}`);
      if (fields.length > 0) {
        return fields.join("; ");
      }
    }
    if (typeof body.detail === "string" && body.detail.trim().length > 0) {
      return body.detail;
    }
    if (typeof body.title === "string" && body.title.trim().length > 0) {
      return body.title;
    }
  } catch {
    return `${fallback} (${response.status}).`;
  }
  return `${fallback} (${response.status}).`;
}

export function networkMessage(fallback: string): string {
  return `${fallback} The backend could not be reached.`;
}
