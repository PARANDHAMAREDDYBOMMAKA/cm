type StatCardProps = {
  label: string;
  value: string;
  hint: string;
  accent?: "neutral" | "emerald" | "amber" | "red";
};

const accentClasses: Record<NonNullable<StatCardProps["accent"]>, string> = {
  neutral: "text-zinc-900 dark:text-zinc-100",
  emerald: "text-emerald-600 dark:text-emerald-400",
  amber: "text-amber-600 dark:text-amber-400",
  red: "text-red-600 dark:text-red-400",
};

export default function StatCard({ label, value, hint, accent = "neutral" }: StatCardProps) {
  return (
    <div className="rounded-xl border border-zinc-200 bg-white p-5 dark:border-zinc-800 dark:bg-zinc-900">
      <p className="text-sm text-zinc-500 dark:text-zinc-400">{label}</p>
      <p className={`mt-2 text-3xl font-semibold tracking-tight ${accentClasses[accent]}`}>{value}</p>
      <p className="mt-1 text-xs text-zinc-400 dark:text-zinc-500">{hint}</p>
    </div>
  );
}
