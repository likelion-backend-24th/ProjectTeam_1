"use client";

import { Suspense, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { AppShell } from "@/components/AppShell";
import { useAuthStore } from "@/store/authStore";
import { ApiError } from "@/lib/api/client";

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const login = useAuthStore((s) => s.login);

  const from = searchParams.get("from");
  const justSignedUp = searchParams.get("justSignedUp") === "1";

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError(null);

    if (!email.trim() || !password) {
      setError("이메일과 비밀번호를 입력해주세요.");
      return;
    }

    setIsSubmitting(true);
    try {
      await login({ email, password });
      router.replace(from || "/");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "로그인에 실패했어요. 다시 시도해주세요.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AppShell showNav={false}>
      <div className="flex flex-col gap-7 pt-3">
        <div className="flex flex-col gap-2 text-center">
          <p className="text-center text-[22px] font-extrabold">🌱 도시 귀농 프로젝트</p>
          <h1 className="text-[19px] font-bold">로그인</h1>
          <p className="text-sm text-ink-soft">이메일과 비밀번호를 입력해주세요</p>
        </div>

        {justSignedUp && (
          <p className="rounded-lg bg-red-50 px-3.5 py-2.5 text-[13px] text-red-700">
            가입이 완료됐어요. 로그인해주세요!
          </p>
        )}
        {error && <p className="rounded-lg bg-red-50 px-3.5 py-2.5 text-[13px] text-red-700">{error}</p>}

        <form className="flex flex-col gap-4" onSubmit={handleSubmit} noValidate>
          <input
            type="email"
            placeholder="email@domain.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            autoComplete="email"
            className="w-full rounded-xl bg-surface px-4 py-3.5 text-[15px] outline-none focus:bg-white focus:ring-1 focus:ring-ink"
          />
          <input
            type="password"
            placeholder="비밀번호"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            className="w-full rounded-xl bg-surface px-4 py-3.5 text-[15px] outline-none focus:bg-white focus:ring-1 focus:ring-ink"
          />

          <button
            type="submit"
            disabled={isSubmitting}
            className="h-[50px] w-full rounded-xl bg-primary text-[15px] font-semibold text-white disabled:opacity-50"
          >
            {isSubmitting ? "..." : "계속"}
          </button>
        </form>

        {/* 소셜 로그인 영역 추가 */}
        <div className="flex flex-col gap-3">
          <div className="relative flex py-2 items-center">
            <div className="flex-grow border-t border-surface"></div>
            <span className="flex-shrink mx-4 text-xs text-ink-soft">또는 소셜 로그인</span>
            <div className="flex-grow border-t border-surface"></div>
          </div>

          <a
            href="http://54.86.192.52:8080/oauth2/authorization/google"
            className="flex h-[50px] w-full items-center justify-center gap-2 rounded-xl border border-surface bg-white text-[15px] font-semibold text-ink hover:bg-surface transition-colors"
          >
            <span>구글로 시작하기</span>
          </a>

          <a
            href="http://54.86.192.52:8080/oauth2/authorization/kakao"
            className="flex h-[50px] w-full items-center justify-center gap-2 rounded-xl bg-[#FEE500] text-[15px] font-semibold text-[#191919] hover:opacity-90 transition-colors"
          >
            <span>카카오로 시작하기</span>
          </a>

          <a
            href="http://54.86.192.52:8080/oauth2/authorization/naver"
            className="flex h-[50px] w-full items-center justify-center gap-2 rounded-xl bg-[#03C75A] text-[15px] font-semibold text-white hover:opacity-90 transition-colors"
          >
            <span>네이버로 시작하기</span>
          </a>
        </div>

        <div className="flex justify-center gap-1.5 text-sm text-ink-soft">
          <span>아직 계정이 없으신가요?</span>
          <Link className="font-bold text-ink" href="/signup">
            회원가입
          </Link>
        </div>
      </div>
    </AppShell>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={null}>
      <LoginForm />
    </Suspense>
  );
}