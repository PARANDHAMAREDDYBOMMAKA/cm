import { LOW_CONFIDENCE } from "@/lib/extraction";

const TONES = {
  high: { text: "text-success", dot: "bg-success" },
  medium: { text: "text-warning", dot: "bg-warning" },
  low: { text: "text-danger", dot: "bg-danger" },
} as const;

export default function ConfidenceDot({ value, edited }: { value: number | null; edited: boolean }) {
  if (edited) {
    return (
      <span className="inline-flex items-center gap-1 text-[11px] font-medium text-success">
        <span className="size-1.5 rounded-full bg-success" />
        verified
      </span>
    );
  }
  if (value == null) {
    return null;
  }
  const tone = value >= 0.9 ? TONES.high : value >= LOW_CONFIDENCE ? TONES.medium : TONES.low;
  return (
    <span className={`inline-flex items-center gap-1 text-[11px] font-medium ${tone.text}`}>
      <span className={`size-1.5 rounded-full ${tone.dot}`} />
      {Math.round(value * 100)}%
    </span>
  );
}
