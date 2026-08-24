"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { AppShell, PageHeader } from "@/components/AppShell";
import { RequireAuth } from "@/components/RequireAuth";
import { BackIcon } from "@/components/icons";
import { useAuthStore } from "@/store/authStore";
import { useToastStore } from "@/store/toastStore";
import { ApiError } from "@/lib/api/client";

function formatBrn(raw) {
  const digits = raw.replace(/\D/g, "").slice(0, 10);
  if (digits.length <= 3) return digits;
  if (digits.length <= 5) return `${digits.slice(0, 3)}-${digits.slice(3)}`;
  return `${digits.slice(0, 3)}-${digits.slice(3, 5)}-${digits.slice(5)}`;
}

function Field({ label, required, children }) {
  return (
    <div className="flex flex-col gap-2">
      <span className="text-sm font-semibold text-ink">
        {label}
        {required && <span className="ml-0.5 text-free">*</span>}
      </span>
      {children}
    </div>
  );
}

function HostApplyView() {
  const router = useRouter();
  const promoteToHost = useAuthStore((s) => s.promoteToHost);
  const showToast = useToastStore((s) => s.showToast);

  const [businessName, setBusinessName] = useState("");
  const [businessRegistrationNumber, setBusinessRegistrationNumber] = useState("");
  const [businessAddress, setBusinessAddress] = useState("");
  const [lookupStatus, setLookupStatus] = useState("idle"); // idle | success | fail
  const [isSubmitting, setIsSubmitting] = useState(false);

  function handleLookup() {
    const digitCount = businessRegistrationNumber.replace(/\D/g, "").length;
    setLookupStatus(digitCount === 10 ? "success" : "fail");
  }

  const canSubmit =
    lookupStatus === "success" && businessName.trim() && businessAddress.trim() && !isSubmitting;

  async function handleSubmit() {
    if (!canSubmit) return;
    setIsSubmitting(true);
    try {
      await promoteToHost({
        businessName: businessName.trim(),
        businessRegistrationNumber,
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
      <div className="flex flex-col gap-6">
        <p className="text-[15px] font-bold text-ink">사업자 정보</p>

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
          <div className="flex gap-2">
            <input
              type="text"
              value={businessRegistrationNumber}
              onChange={(e) => {
                setBusinessRegistrationNumber(formatBrn(e.target.value));
                setLookupStatus("idle");
              }}
              placeholder="000-00-00000"
              maxLength={12}
              className="flex-1 rounded-xl bg-surface px-4 py-3.5 text-[15px] outline-none"
            />
            <button
              type="button"
              onClick={handleLookup}
              disabled={businessRegistrationNumber.replace(/\D/g, "").length === 0}
              className="shrink-0 cursor-pointer rounded-xl bg-free px-4 text-[14px] font-semibold whitespace-nowrap text-white disabled:opacity-40"
            >
              사업자 조회
            </button>
          </div>
          {lookupStatus === "fail" && (
            <p className="text-xs text-danger">사업자등록번호 10자리를 입력해주세요.</p>
          )}
          {lookupStatus === "success" && <p className="text-xs font-medium text-free">✓ 확인됐어요.</p>}
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
          type="button"
          onClick={handleSubmit}
          disabled={!canSubmit}
          className="mt-1 h-[52px] w-full cursor-pointer rounded-xl bg-free text-[15px] font-bold text-white disabled:opacity-40"
        >
          {isSubmitting ? "전환 중..." : "호스트로 변경하기"}
        </button>
      </div>
    </AppShell>
  );
}

export default function HostApplyPage() {
  return (
    <RequireAuth>
      <HostApplyView />
    </RequireAuth>
  );
}