"use client";

import { useEffect, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useAuthStore } from "@/store/authStore";

function OAuthCallbackContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const socialLogin = useAuthStore((s) => s.socialLogin);

  useEffect(() => {
    const accessToken = searchParams.get("accessToken");
    const refreshToken = searchParams.get("refreshToken");

    if (accessToken) {
      socialLogin(accessToken, refreshToken)
        .then(() => {
          // 로그인 성공 시 메인 화면으로 이동
          router.replace("/");
        })
        .catch(() => {
          // 실패 시 로그인 페이지로 튕겨내기
          router.replace("/login?error=social_failed");
        });
    } else {
      router.replace("/login");
    }
  }, [searchParams, socialLogin, router]);

  return (
    <div className="flex h-screen items-center justify-center text-sm text-ink-muted">
      로그인 처리 중입니다... 잠시만 기다려주세요!
    </div>
  );
}

export default function OAuthCallbackPage() {
  return (
    <Suspense fallback={<div className="flex h-screen items-center justify-center text-sm text-ink-muted">로딩 중...</div>}>
      <OAuthCallbackContent />
    </Suspense>
  );
}