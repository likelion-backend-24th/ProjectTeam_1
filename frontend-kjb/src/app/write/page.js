"use client";

import { RequireAuth } from "@/components/RequireAuth";
import { BoardForm } from "@/components/BoardForm";

export default function WritePage() {
  return (
    <RequireAuth>
      <BoardForm />
    </RequireAuth>
  );
}
