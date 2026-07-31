export default function Loading() {
  return (
    <div className="mx-auto max-w-4xl animate-pulse">
      <div className="h-8 w-32 rounded-lg bg-border" />
      <div className="mt-2 h-4 w-80 rounded bg-border" />
      <div className="mt-6 h-24 rounded-xl border border-border bg-surface" />
      <div className="mt-6 overflow-hidden rounded-xl border border-border bg-surface">
        {Array.from({ length: 5 }).map((_, index) => (
          <div key={index} className="flex items-center gap-4 border-b border-border px-5 py-4 last:border-0">
            <div className="h-4 w-44 rounded bg-canvas" />
            <div className="ml-auto h-5 w-24 rounded-full bg-canvas" />
          </div>
        ))}
      </div>
    </div>
  );
}
