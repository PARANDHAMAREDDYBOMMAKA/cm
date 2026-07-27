export default function Loading() {
  return (
    <div className="mx-auto max-w-6xl animate-pulse">
      <div className="h-8 w-40 rounded-lg bg-border" />
      <div className="mt-2 h-4 w-72 rounded bg-border" />
      <div className="mt-6 overflow-hidden rounded-xl border border-border bg-surface">
        {Array.from({ length: 5 }).map((_, index) => (
          <div key={index} className="flex items-center gap-4 border-b border-border px-5 py-4 last:border-0">
            <div className="h-4 w-32 rounded bg-canvas" />
            <div className="h-5 w-20 rounded-full bg-canvas" />
            <div className="ml-auto h-4 w-24 rounded bg-canvas" />
          </div>
        ))}
      </div>
    </div>
  );
}
