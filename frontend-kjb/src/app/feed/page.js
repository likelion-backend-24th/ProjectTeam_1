import { AppShell, PageHeader } from "@/components/AppShell";

export default function FeedPage() {
  return (
    <AppShell header={<PageHeader title="피드" />}>
      <div className="flex flex-1 items-center justify-center text-sm text-ink-muted">준비 중이에요.</div>
    </AppShell>
  );
}