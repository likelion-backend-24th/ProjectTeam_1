import { create } from "zustand";

let nextId = 1;
const DURATION_MS = 2500;

export const useToastStore = create((set) => ({
  toasts: [],

  showToast: (message, type = "success") => {
    const id = nextId++;
    set((state) => ({ toasts: [...state.toasts, { id, message, type }] }));
    setTimeout(() => {
      set((state) => ({ toasts: state.toasts.filter((t) => t.id !== id) }));
    }, DURATION_MS);
  },

  dismissToast: (id) => {
    set((state) => ({ toasts: state.toasts.filter((t) => t.id !== id) }));
  },
}));
