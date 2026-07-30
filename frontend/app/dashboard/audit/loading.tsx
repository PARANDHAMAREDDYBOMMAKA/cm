export default function Loading() {
  return (
    <div className="mx-auto max-w-5xl animate-pulse">
      <div className="h-8 w-44 rounded-lg bg-border" />
      <div className="mt-2 h-4 w-80 rounded bg-border" />
      <div className="mt-6 h-20 rounded-xl border border-border bg-surface" />
      <div className="mt-3 overflow-hidden rounded-xl border border-border bg-surface">
        {Array.from({ length: 6 }).map((_, index) => (
          <div key={index} className="border-b border-border px-5 py-4 last:border-0">
            <div className="h-4 w-56 rounded bg-canvas" />
            <div className="mt-2 h-3 w-80 rounded bg-canvas" />
          </div>
        ))}
      </div>
    </div>
  );
}
