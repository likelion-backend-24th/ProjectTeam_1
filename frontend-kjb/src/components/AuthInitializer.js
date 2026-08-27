"use client";

import { useEffect } from "react";
import { useAuthStore } from "@/store/authStore";

export function AuthInitializer() {
  useEffect(() => {
    useAuthStore.getState().init();
  }, []);

  return null;
}
