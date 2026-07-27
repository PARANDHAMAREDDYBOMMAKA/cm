export default function Loading() {
  return (
    <div className="mx-auto max-w-4xl animate-pulse">
      <div className="h-4 w-16 rounded bg-border" />
      <div className="mt-4 h-8 w-48 rounded-lg bg-border" />
      <div className="mt-2 h-4 w-40 rounded bg-border" />
      <div className="mt-8 h-4 w-24 rounded bg-border" />
      <div className="mt-3 space-y-2 overflow-hidden rounded-xl border border-border bg-surface p-4">
        {Array.from({ length: 3 }).map((_, index) => (
          <div key={index} className="h-12 rounded-lg bg-canvas" />
        ))}
      </div>
      <div className="mt-6 h-40 rounded-xl border border-dashed border-border-strong bg-surface" />
    </div>
  );
}
