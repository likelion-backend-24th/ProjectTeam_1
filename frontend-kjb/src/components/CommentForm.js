"use client";

import { useState } from "react";
import { SendIcon } from "./icons";

export function CommentForm({ placeholder = "따뜻한 댓글을 남겨주세요...", onSubmit }) {
  const [value, setValue] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    const trimmed = value.trim();
    if (!trimmed || isSubmitting) return;

    setIsSubmitting(true);
    try {
      await onSubmit(trimmed);
      setValue("");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form
      className="sticky bottom-0 flex items-center gap-2 border-t border-border bg-white px-4 py-3"
      onSubmit={handleSubmit}
    >
      <input
        type="text"
        placeholder={placeholder}
        value={value}
        onChange={(e) => setValue(e.target.value)}
        disabled={isSubmitting}
        className="flex-1 rounded-full bg-surface px-4 py-3 text-sm outline-none"
      />
      <button
        type="submit"
        disabled={!value.trim() || isSubmitting}
        aria-label="등록"
        className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-accent text-white disabled:opacity-40"
      >
        <SendIcon size={16} />
      </button>
    </form>
  );
}
