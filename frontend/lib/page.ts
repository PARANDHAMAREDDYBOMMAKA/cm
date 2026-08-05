export type PageResponse<T> = {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
};

export function itemsOf<T>(body: unknown): T[] {
  if (Array.isArray(body)) {
    return body as T[];
  }
  if (body && typeof body === "object" && Array.isArray((body as PageResponse<T>).items)) {
    return (body as PageResponse<T>).items;
  }
  return [];
}

export function totalOf(body: unknown, fallback: number): number {
  if (body && typeof body === "object" && typeof (body as PageResponse<unknown>).totalItems === "number") {
    return (body as PageResponse<unknown>).totalItems;
  }
  return fallback;
}

export async function readItems<T>(response: Response): Promise<T[]> {
  if (!response.ok) {
    return [];
  }
  try {
    return itemsOf<T>(await response.json());
  } catch {
    return [];
  }
}
