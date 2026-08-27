"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { AppShell, PageHeader } from "@/components/AppShell";
import { BackIcon } from "@/components/icons";
import { signup } from "@/lib/api/auth";
import { ApiError } from "@/lib/api/client";

const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,20}$/;

export default function SignupPage() {
  const router = useRouter();

  const [email, setEmail] = useState("");
  const [nickname, setNickname] = useState("");
  const [name, setName] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [agreed, setAgreed] = useState(false);
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  function validate() {
    const next = {};
    if (!email.trim()) next.email = "이메일을 입력해주세요.";
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) next.email = "올바른 이메일 형식이 아니에요.";

    if (!name.trim()) next.name = "이름을 입력해주세요.";

    if (!nickname.trim()) next.nickname = "닉네임을 입력해주세요.";
    else if (nickname.trim().length < 2 || nickname.trim().length > 10)
      next.nickname = "닉네임은 2~10자로 입력해주세요.";

    if (!password) next.password = "비밀번호를 입력해주세요.";
    else if (!PASSWORD_PATTERN.test(password))
      next.password = "8~20자, 영문/숫자/특수문자를 모두 포함해주세요.";

    if (!passwordConfirm) next.passwordConfirm = "비밀번호를 다시 입력해주세요.";
    else if (password !== passwordConfirm) next.passwordConfirm = "비밀번호가 일치하지 않아요.";

    if (!agreed) next.agreed = "이용약관에 동의해주세요.";

    setErrors(next);
    return Object.keys(next).length === 0;
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setServerError(null);
    if (!validate()) return;

    setIsSubmitting(true);
    try {
      await signup({ email, password, passwordConfirm, nickname, name });
      router.push("/login?justSignedUp=1");
    } catch (err) {
      setServerError(err instanceof ApiError ? err.message : "회원가입에 실패했어요. 다시 시도해주세요.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AppShell
      showNav={false}
      header={
        <PageHeader
          title="회원가입"
          left={
            <Link href="/login" className="flex h-8 w-8 items-center justify-center rounded-lg" aria-label="뒤로가기">
              <BackIcon />
            </Link>
          }
        />
      }
    >
      <form className="flex flex-col gap-4" onSubmit={handleSubmit} noValidate>
        {serverError && <p className="rounded-lg bg-red-50 px-3.5 py-2.5 text-[13px] text-red-700">{serverError}</p>}

        <Field label="이메일" htmlFor="email" error={errors.email}>
          <input
            id="email"
            type="email"
            placeholder="email@domain.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            autoComplete="email"
            className="w-full rounded-xl bg-surface px-4 py-3.5 text-[15px] outline-none focus:bg-white focus:ring-1 focus:ring-ink"
          />
        </Field>

        <Field label="닉네임" htmlFor="nickname" error={errors.nickname}>
          <input
            id="nickname"
            type="text"
            placeholder="사용할 닉네임"
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
            className="w-full rounded-xl bg-surface px-4 py-3.5 text-[15px] outline-none focus:bg-white focus:ring-1 focus:ring-ink"
          />
        </Field>

        <Field label="이름" htmlFor="name" error={errors.name}>
          <input
            id="name"
            type="text"
            placeholder="이름..."
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="w-full rounded-xl bg-surface px-4 py-3.5 text-[15px] outline-none focus:bg-white focus:ring-1 focus:ring-ink"
          />
        </Field>

        <Field label="비밀번호" htmlFor="password" error={errors.password}>
          <input
            id="password"
            type="password"
            placeholder="8자 이상..."
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="new-password"
            className="w-full rounded-xl bg-surface px-4 py-3.5 text-[15px] outline-none focus:bg-white focus:ring-1 focus:ring-ink"
          />
        </Field>

        <Field label="비밀번호 확인" htmlFor="passwordConfirm" error={errors.passwordConfirm}>
          <input
            id="passwordConfirm"
            type="password"
            placeholder="재입력..."
            value={passwordConfirm}
            onChange={(e) => setPasswordConfirm(e.target.value)}
            autoComplete="new-password"
            className="w-full rounded-xl bg-surface px-4 py-3.5 text-[15px] outline-none focus:bg-white focus:ring-1 focus:ring-ink"
          />
        </Field>

        <div>
          <label className="flex items-center gap-2 text-sm text-ink-soft">
            <input
              type="checkbox"
              checked={agreed}
              onChange={(e) => setAgreed(e.target.checked)}
              className="h-[18px] w-[18px] accent-primary"
            />
            이용약관 동의
          </label>
          {errors.agreed && <p className="mt-1 text-[13px] text-red-500">{errors.agreed}</p>}
        </div>

        <button
          type="submit"
          disabled={isSubmitting}
          className="h-[50px] w-full rounded-xl bg-primary text-[15px] font-semibold text-white disabled:opacity-50"
        >
          {isSubmitting ? "..." : "가입하기"}
        </button>
      </form>
    </AppShell>
  );
}

function Field({ label, htmlFor, error, children }) {
  return (
    <div className="flex flex-col gap-2">
      <label className="text-sm font-semibold" htmlFor={htmlFor}>
        {label}
      </label>
      {children}
      {error && <p className="text-[13px] text-red-500">{error}</p>}
    </div>
  );
}
