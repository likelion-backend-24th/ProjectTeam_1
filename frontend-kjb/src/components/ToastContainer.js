"use client";

import { useToastStore } from "@/store/toastStore";

export function ToastContainer() {
  const toasts = useToastStore((s) => s.toasts);
  const dismissToast = useToastStore((s) => s.dismissToast);

  if (toasts.length === 0) return null;

  return (
    <div className="pointer-events-none fixed inset-x-0 bottom-20 z-50 flex justify-center px-4">
      <div className="flex w-full max-w-[440px] flex-col items-center gap-2">
        {toasts.map((t) => (
          <button
            key={t.id}
            type="button"
            onClick={() => dismissToast(t.id)}
            className={`pointer-events-auto w-full rounded-xl px-4 py-3 text-center text-sm font-medium text-white shadow-lg ${
              t.type === "error" ? "bg-red-500" : "bg-ink"
            }`}
          >
            {t.message}
          </button>
        ))}
      </div>
    </div>
  );
}
