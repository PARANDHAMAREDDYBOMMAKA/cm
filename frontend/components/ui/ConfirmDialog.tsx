"use client";

import { Loader2 } from "lucide-react";
import Modal from "@/components/ui/Modal";

type Props = {
  open: boolean;
  title: string;
  description: string;
  confirmLabel: string;
  busyLabel?: string;
  tone?: "danger" | "brand";
  busy?: boolean;
  error?: string | null;
  onConfirm: () => void;
  onCancel: () => void;
};

export default function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel,
  busyLabel,
  tone = "danger",
  busy = false,
  error,
  onConfirm,
  onCancel,
}: Props) {
  const confirmClass =
    tone === "danger"
      ? "bg-danger text-white hover:opacity-90"
      : "bg-brand text-white hover:bg-brand-hover";

  return (
    <Modal
      open={open}
      onClose={() => {
        if (!busy) {
          onCancel();
        }
      }}
      title={title}
      description={description}
      size="sm"
      footer={
        <>
          <button
            type="button"
            onClick={onCancel}
            disabled={busy}
            className="rounded-lg px-3.5 py-2 text-sm font-medium text-secondary transition-colors hover:bg-canvas disabled:opacity-60"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={busy}
            className={`inline-flex items-center gap-2 rounded-lg px-3.5 py-2 text-sm font-medium transition-opacity disabled:opacity-60 ${confirmClass}`}
          >
            {busy ? <Loader2 className="size-4 animate-spin" /> : null}
            {busy ? (busyLabel ?? "Working…") : confirmLabel}
          </button>
        </>
      }
    >
      {error ? (
        <p role="alert" className="rounded-lg bg-danger-soft px-3 py-2 text-sm text-danger">
          {error}
        </p>
      ) : null}
    </Modal>
  );
}
