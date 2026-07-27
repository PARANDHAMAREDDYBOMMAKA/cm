import type { LucideIcon } from "lucide-react";

type Tone = "brand" | "success" | "warning" | "danger";

type StatCardProps = {
  label: string;
  value: string;
  hint: string;
  icon: LucideIcon;
  tone?: Tone;
};

const toneClasses: Record<Tone, string> = {
  brand: "bg-brand-soft text-brand",
  success: "bg-success-soft text-success",
  warning: "bg-warning-soft text-warning",
  danger: "bg-danger-soft text-danger",
};

export default function StatCard({ label, value, hint, icon: Icon, tone = "brand" }: StatCardProps) {
  return (
    <div className="rounded-xl border border-border bg-surface p-5">
      <div className="flex items-center justify-between">
        <span className="text-sm text-muted">{label}</span>
        <span className={`flex size-8 items-center justify-center rounded-lg ${toneClasses[tone]}`}>
          <Icon className="size-4" />
        </span>
      </div>
      <p className="mt-4 text-3xl font-semibold tracking-tight tabular-nums">{value}</p>
      <p className="mt-1 text-xs text-subtle">{hint}</p>
    </div>
  );
}
