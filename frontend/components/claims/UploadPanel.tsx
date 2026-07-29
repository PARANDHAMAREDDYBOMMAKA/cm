"use client";

import { useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { Loader2, UploadCloud } from "lucide-react";
import { proxyUrl } from "@/lib/api";
import { deviceFingerprint } from "@/lib/fingerprint";

export default function UploadPanel({ claimId }: { claimId: string }) {
  const router = useRouter();
  const inputRef = useRef<HTMLInputElement>(null);
  const [progress, setProgress] = useState<{ done: number; total: number } | null>(null);
  const [dragging, setDragging] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const uploading = progress !== null;

  const uploadOne = async (file: File, fingerprint: string | null): Promise<string | null> => {
    const form = new FormData();
    form.append("file", file);
    try {
      const response = await fetch(proxyUrl(`/api/claims/${claimId}/documents`), {
        method: "POST",
        headers: fingerprint ? { "X-Device-Fingerprint": fingerprint } : undefined,
        body: form,
      });
      if (response.ok) {
        return null;
      }
      if (response.status === 503) {
        return "Object storage (R2) is not configured yet.";
      }
      return `${file.name} failed (${response.status}).`;
    } catch {
      return `${file.name} failed. Is the backend running?`;
    }
  };

  const upload = async (files: File[]) => {
    if (files.length === 0) {
      return;
    }
    setError(null);
    setProgress({ done: 0, total: files.length });

    const fingerprint = await deviceFingerprint();
    const failures: string[] = [];
    for (const [index, file] of files.entries()) {
      const failure = await uploadOne(file, fingerprint);
      if (failure) {
        failures.push(failure);
      }
      setProgress({ done: index + 1, total: files.length });
    }

    setProgress(null);
    if (inputRef.current) {
      inputRef.current.value = "";
    }
    if (failures.length > 0) {
      setError(failures.join(" "));
    }
    if (failures.length < files.length) {
      router.refresh();
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
        upload(Array.from(event.dataTransfer.files ?? []));
      }}
      className={`flex flex-col items-center rounded-xl border border-dashed p-8 text-center transition-colors ${
        dragging ? "border-brand bg-brand-soft" : "border-border-strong bg-surface"
      }`}
    >
      <input
        ref={inputRef}
        type="file"
        multiple
        className="hidden"
        onChange={(event) => upload(Array.from(event.target.files ?? []))}
      />
      <span className="flex size-11 items-center justify-center rounded-xl bg-brand-soft text-brand">
        {uploading ? <Loader2 className="size-5 animate-spin" /> : <UploadCloud className="size-5" />}
      </span>
      <p className="mt-3 text-sm text-secondary">
        Drag &amp; drop documents here, or
      </p>
      <button
        type="button"
        onClick={() => inputRef.current?.click()}
        disabled={uploading}
        className="mt-3 inline-flex items-center gap-2 rounded-lg bg-brand px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-brand-hover disabled:opacity-60"
      >
        <UploadCloud className="size-4" />
        {progress
          ? `Uploading ${Math.min(progress.done + 1, progress.total)} of ${progress.total}…`
          : "Choose files"}
      </button>
      <p className="mt-2 text-xs text-subtle">PDF or images, up to 25 MB each</p>
      {error ? <p className="mt-3 text-xs text-danger">{error}</p> : null}
    </div>
  );
}
