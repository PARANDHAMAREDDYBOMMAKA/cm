"use client";

import { useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { Loader2, UploadCloud } from "lucide-react";
import { proxyUrl } from "@/lib/api";

export default function UploadPanel({ claimId }: { claimId: string }) {
  const router = useRouter();
  const inputRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);
  const [dragging, setDragging] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const upload = async (file: File) => {
    setUploading(true);
    setError(null);
    try {
      const form = new FormData();
      form.append("file", file);
      const response = await fetch(proxyUrl(`/api/claims/${claimId}/documents`), {
        method: "POST",
        body: form,
      });
      if (response.ok) {
        router.refresh();
      } else if (response.status === 503) {
        setError("Object storage (R2) is not configured yet.");
      } else {
        setError(`Upload failed (${response.status}).`);
      }
    } catch {
      setError("Upload failed. Is the backend running?");
    } finally {
      setUploading(false);
      if (inputRef.current) {
        inputRef.current.value = "";
      }
    }
  };

  return (
    <div
      onDragOver={(event) => {
        event.preventDefault();
        setDragging(true);
      }}
      onDragLeave={() => setDragging(false)}
      onDrop={(event) => {
        event.preventDefault();
        setDragging(false);
        const file = event.dataTransfer.files?.[0];
        if (file) {
          upload(file);
        }
      }}
      className={`flex flex-col items-center rounded-xl border border-dashed p-8 text-center transition-colors ${
        dragging ? "border-brand bg-brand-soft" : "border-border-strong bg-surface"
      }`}
    >
      <input
        ref={inputRef}
        type="file"
        className="hidden"
        onChange={(event) => {
          const file = event.target.files?.[0];
          if (file) {
            upload(file);
          }
        }}
      />
      <span className="flex size-11 items-center justify-center rounded-xl bg-brand-soft text-brand">
        {uploading ? <Loader2 className="size-5 animate-spin" /> : <UploadCloud className="size-5" />}
      </span>
      <p className="mt-3 text-sm text-secondary">
        Drag &amp; drop a document here, or
      </p>
      <button
        type="button"
        onClick={() => inputRef.current?.click()}
        disabled={uploading}
        className="mt-3 inline-flex items-center gap-2 rounded-lg bg-brand px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-brand-hover disabled:opacity-60"
      >
        <UploadCloud className="size-4" />
        {uploading ? "Uploading…" : "Choose file"}
      </button>
      <p className="mt-2 text-xs text-subtle">PDF or image, up to 25 MB</p>
      {error ? <p className="mt-3 text-xs text-danger">{error}</p> : null}
    </div>
  );
}
