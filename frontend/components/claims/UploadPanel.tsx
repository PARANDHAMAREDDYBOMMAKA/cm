"use client";

import { useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { AlertTriangle, Loader2, UploadCloud } from "lucide-react";
import { proxyUrl } from "@/lib/api";
import { deviceFingerprint } from "@/lib/fingerprint";
import { networkMessage, problemMessage } from "@/lib/problem";
import { formatBytes } from "@/lib/claim";
import { useToast } from "@/components/ui/Toast";

const ACCEPTED_TYPES = [
  "application/pdf",
  "image/png",
  "image/jpeg",
  "image/webp",
  "image/tiff",
] as const;

const ACCEPT_ATTRIBUTE = [...ACCEPTED_TYPES, ".pdf", ".png", ".jpg", ".jpeg", ".webp", ".tif", ".tiff"].join(",");

const MAX_BYTES = 25 * 1024 * 1024;

function rejectionReason(file: File): string | null {
  if (file.size === 0) {
    return `${file.name} is empty.`;
  }
  if (file.size > MAX_BYTES) {
    return `${file.name} is ${formatBytes(file.size)}; the limit is ${formatBytes(MAX_BYTES)}.`;
  }
  if (file.type && !ACCEPTED_TYPES.includes(file.type as (typeof ACCEPTED_TYPES)[number])) {
    return `${file.name} is a ${file.type} file. Only PDF and image documents are accepted.`;
  }
  return null;
}

export default function UploadPanel({ claimId }: { claimId: string }) {
  const router = useRouter();
  const { notify } = useToast();
  const inputRef = useRef<HTMLInputElement>(null);
  const [progress, setProgress] = useState<{ done: number; total: number } | null>(null);
  const [dragging, setDragging] = useState(false);
  const [errors, setErrors] = useState<string[]>([]);

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
      return `${file.name}: ${await problemMessage(response, "upload failed")}`;
    } catch {
      return `${file.name}: ${networkMessage("upload failed.")}`;
    }
  };

  const upload = async (files: File[]) => {
    if (files.length === 0) {
      return;
    }

    const rejected: string[] = [];
    const accepted: File[] = [];
    for (const file of files) {
      const reason = rejectionReason(file);
      if (reason) {
        rejected.push(reason);
      } else {
        accepted.push(file);
      }
    }

    setErrors(rejected);
    if (inputRef.current) {
      inputRef.current.value = "";
    }
    if (accepted.length === 0) {
      notify({
        tone: "error",
        title: "Nothing uploaded",
        description: rejected[0],
      });
      return;
    }

    setProgress({ done: 0, total: accepted.length });
    const fingerprint = await deviceFingerprint();
    const failures = [...rejected];
    let uploaded = 0;

    for (const [index, file] of accepted.entries()) {
      const failure = await uploadOne(file, fingerprint);
      if (failure) {
        failures.push(failure);
      } else {
        uploaded += 1;
      }
      setProgress({ done: index + 1, total: accepted.length });
    }

    setProgress(null);
    setErrors(failures);

    if (uploaded > 0) {
      notify({
        tone: "success",
        title: `${uploaded} document${uploaded === 1 ? "" : "s"} uploaded`,
        description: "Reading the document now — the page will update when it is done.",
      });
      router.refresh();
    }
    if (failures.length > 0) {
      notify({
        tone: "error",
        title: `${failures.length} file${failures.length === 1 ? "" : "s"} could not be uploaded`,
        description: failures[0],
      });
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
        accept={ACCEPT_ATTRIBUTE}
        className="hidden"
        onChange={(event) => upload(Array.from(event.target.files ?? []))}
      />
      <span className="flex size-11 items-center justify-center rounded-xl bg-brand-soft text-brand">
        {uploading ? <Loader2 className="size-5 animate-spin" /> : <UploadCloud className="size-5" />}
      </span>
      <p className="mt-3 text-sm text-secondary">Drag &amp; drop documents here, or</p>
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
      <p className="mt-2 text-xs text-subtle">PDF, PNG, JPEG, WebP or TIFF · up to {formatBytes(MAX_BYTES)} each</p>

      {errors.length > 0 ? (
        <ul role="alert" className="mt-3 w-full space-y-1 text-left">
          {errors.map((message) => (
            <li key={message} className="flex items-start gap-2 text-xs text-danger">
              <AlertTriangle className="mt-0.5 size-3.5 shrink-0" />
              <span className="break-words">{message}</span>
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}
