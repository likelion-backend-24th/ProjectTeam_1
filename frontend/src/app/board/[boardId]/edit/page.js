"use client";

import { useParams } from "next/navigation";
import { RequireAuth } from "@/components/RequireAuth";
import { BoardForm } from "@/components/BoardForm";

export default function EditBoardPage() {
  const { boardId } = useParams();
  return (
    <RequireAuth>
      <BoardForm boardId={Number(boardId)} />
    </RequireAuth>
  );
}
