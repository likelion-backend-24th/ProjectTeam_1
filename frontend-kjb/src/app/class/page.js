import { AppShell, PageHeader } from "@/components/AppShell";

export default function ClassPage() {
  return (
    <AppShell header={<PageHeader title="클래스" />}>
      <div className="flex flex-1 items-center justify-center text-sm text-ink-muted">준비 중이에요.</div>
    </AppShell>
  );
}