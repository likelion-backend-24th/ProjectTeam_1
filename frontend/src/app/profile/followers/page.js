"use client";

import { RequireAuth } from "@/components/RequireAuth";
import { FollowListView } from "@/components/FollowListView";

export default function FollowersPage() {
  return (
    <RequireAuth>
      <FollowListView mode="followers" />
    </RequireAuth>
  );
}
