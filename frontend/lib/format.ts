const currencyFormatter = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  maximumFractionDigits: 0,
});

export function formatCurrency(value: string | number): string {
  const amount = typeof value === "string" ? Number(value) : value;
  if (!Number.isFinite(amount)) {
    return currencyFormatter.format(0);
  }
  return currencyFormatter.format(amount);
}

export function formatMinutes(minutes: number | null): string {
  if (minutes === null || !Number.isFinite(minutes)) {
    return "—";
  }
  const rounded = Math.round(minutes);
  if (rounded < 60) {
    return `${rounded}m`;
  }
  const hours = Math.floor(rounded / 60);
  const remainder = rounded % 60;
  return remainder === 0 ? `${hours}h` : `${hours}h ${remainder}m`;
}

export function formatRelativeAge(iso: string): string {
  const then = new Date(iso).getTime();
  if (Number.isNaN(then)) {
    return "—";
  }
  const diffMs = Date.now() - then;
  const diffMinutes = Math.round(diffMs / 60000);
  if (diffMinutes < 1) {
    return "just now";
  }
  if (diffMinutes < 60) {
    return `${diffMinutes}m ago`;
  }
  const diffHours = Math.round(diffMinutes / 60);
  if (diffHours < 24) {
    return `${diffHours}h ago`;
  }
  const diffDays = Math.round(diffHours / 24);
  return `${diffDays}d ago`;
}
