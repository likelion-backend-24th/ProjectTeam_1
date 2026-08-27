"use client";

import { RequireAuth } from "@/components/RequireAuth";
import { FollowListView } from "@/components/FollowListView";

export default function FollowingPage() {
  return (
    <RequireAuth>
      <FollowListView mode="following" />
    </RequireAuth>
  );
}
