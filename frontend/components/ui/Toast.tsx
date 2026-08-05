"use client";

import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { AlertTriangle, CheckCircle2, Info, X } from "lucide-react";

export type ToastTone = "success" | "error" | "info";

type Toast = {
  id: number;
  tone: ToastTone;
  title: string;
  description?: string;
};

type ToastContextValue = {
  notify: (toast: { tone: ToastTone; title: string; description?: string }) => void;
};

const ToastContext = createContext<ToastContextValue | null>(null);

const TONE_STYLES: Record<ToastTone, { wrapper: string; icon: typeof Info }> = {
  success: { wrapper: "border-success bg-success-soft text-success", icon: CheckCircle2 },
  error: { wrapper: "border-danger bg-danger-soft text-danger", icon: AlertTriangle },
  info: { wrapper: "border-border bg-surface text-secondary", icon: Info },
};

const DISMISS_MS = 6000;

export function useToast(): ToastContextValue {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error("useToast must be used inside a ToastProvider");
  }
  return context;
}

export default function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const nextId = useRef(0);

  const dismiss = useCallback((id: number) => {
    setToasts((current) => current.filter((toast) => toast.id !== id));
  }, []);

  const notify = useCallback(
    (toast: { tone: ToastTone; title: string; description?: string }) => {
      const id = nextId.current++;
      setToasts((current) => [...current, { ...toast, id }]);
      window.setTimeout(() => dismiss(id), DISMISS_MS);
    },
    [dismiss],
  );

  const value = useMemo(() => ({ notify }), [notify]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div
        aria-live="polite"
        aria-atomic="false"
        className="pointer-events-none fixed bottom-4 right-4 z-[60] flex w-full max-w-sm flex-col gap-2"
      >
        {toasts.map((toast) => {
          const { wrapper, icon: Icon } = TONE_STYLES[toast.tone];
          return (
            <div
              key={toast.id}
              role={toast.tone === "error" ? "alert" : "status"}
              className={`pointer-events-auto flex items-start gap-3 rounded-xl border p-4 shadow-lg ${wrapper}`}
            >
              <Icon className="mt-0.5 size-4 shrink-0" />
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium">{toast.title}</p>
                {toast.description ? (
                  <p className="mt-0.5 break-words text-xs opacity-90">{toast.description}</p>
                ) : null}
              </div>
              <button
                type="button"
                onClick={() => dismiss(toast.id)}
                aria-label="Dismiss notification"
                className="shrink-0 rounded p-0.5 opacity-70 transition-opacity hover:opacity-100"
              >
                <X className="size-4" />
              </button>
            </div>
          );
        })}
      </div>
    </ToastContext.Provider>
  );
}
