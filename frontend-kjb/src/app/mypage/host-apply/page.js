"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { AppShell, PageHeader } from "@/components/AppShell";
import { RequireAuth } from "@/components/RequireAuth";
import { BackIcon } from "@/components/icons";
import { useAuthStore } from "@/store/authStore";
import { useToastStore } from "@/store/toastStore";
import { ApiError } from "@/lib/api/client";

const BRN_PATTERN = /^\d{3}-\d{2}-\d{5}$/;

function HostApplyView() {
  const router = useRouter();
  const promoteToHost = useAuthStore((s) => s.promoteToHost);
  const showToast = useToastStore((s) => s.showToast);

  const [businessName, setBusinessName] = useState("");
  const [businessRegistrationNumber, setBusinessRegistrationNumber] = useState("");
  const [businessAddress, setBusinessAddress] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    if (!businessName.trim() || !businessRegistrationNumber.trim() || !businessAddress.trim()) {
      showToast("모든 항목을 입력해주세요.", "error");
      return;
    }
    if (!BRN_PATTERN.test(businessRegistrationNumber.trim())) {
      showToast("사업자등록번호 형식이 올바르지 않아요. (예: 000-00-00000)", "error");
      return;
    }

    setIsSubmitting(true);
    try {
      await promoteToHost({
        businessName: businessName.trim(),
        businessRegistrationNumber: businessRegistrationNumber.trim(),
        businessAddress: businessAddress.trim(),
      });
      showToast("호스트로 전환됐어요!");
      router.replace("/mypage");
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "호스트 전환에 실패했어요.", "error");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AppShell
      header={
        <PageHeader
          title="호스트 신청"
          left={
            <button type="button" onClick={() => router.back()} className="cursor-pointer">
              <BackIcon size={22} />
            </button>
          }
        />
      }
    >
      <form onSubmit={handleSubmit} className="flex flex-col gap-5">
        <p className="rounded-xl bg-surface px-4 py-3.5 text-[13px] text-ink-muted">
          사업자 정보를 입력하면 별도 심사 없이 바로 호스트로 전환돼요.
        </p>

        <Field label="사업자명" required>
          <input
            type="text"
            value={businessName}
            onChange={(e) => setBusinessName(e.target.value)}
            placeholder="사업자명을 입력해주세요"
            className="w-full rounded-xl bg-surface px-4 py-3.5 text-[15px] outline-none"
          />
        </Field>

        <Field label="사업자등록번호" required>
          <input
            type="text"
            value={businessRegistrationNumber}
            onChange={(e) => setBusinessRegistrationNumber(e.target.value)}
            placeholder="000-00-00000"
            className="w-full rounded-xl bg-surface px-4 py-3.5 text-[15px] outline-none"
          />
        </Field>

        <Field label="사업장 주소" required>
          <input
            type="text"
            value={businessAddress}
            onChange={(e) => setBusinessAddress(e.target.value)}
            placeholder="사업장 주소를 입력해주세요"
            className="w-full rounded-xl bg-surface px-4 py-3.5 text-[15px] outline-none"
          />
        </Field>

        <button
          type="submit"
          disabled={isSubmitting}
          className="mt-2 h-[50px] w-full rounded-xl bg-primary text-[15px] font-semibold text-white disabled:opacity-50"
        >
          {isSubmitting ? "전환 중..." : "호스트 신청하기"}
        </button>
      </form>
    </AppShell>
  );
}

function Field({ label, required, children }) {
  return (
    <div className="flex flex-col gap-2">
      <span className="text-sm font-semibold text-ink">
        {label}
        {required && <span className="ml-0.5 text-primary">*</span>}
      </span>
      {children}
    </div>
  );
}

export default function HostApplyPage() {
  return (
    <RequireAuth>
      <HostApplyView />
    </RequireAuth>
  );
}