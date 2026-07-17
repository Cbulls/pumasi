"use client";

import { FormEvent, Suspense, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { useCurrentUser } from "@/context/CurrentUserContext";

function LoginForm() {
  const router = useRouter();
  const params = useSearchParams();
  const next = params.get("next") || "/home";
  const { requestMagicLink, loginWithMagicToken, ready, token, demoAuth, switchUser, users } =
    useCurrentUser();
  const [email, setEmail] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [echoed, setEchoed] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  useEffect(() => {
    if (ready && token) router.replace(next);
  }, [ready, token, router, next]);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setPending(true);
    setError(null);
    setMessage(null);
    setEchoed(null);
    try {
      const res = await requestMagicLink(email, displayName || undefined);
      setMessage(res.message);
      if (res.echoedToken) setEchoed(res.echoedToken);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setPending(false);
    }
  };

  const verifyEcho = async () => {
    if (!echoed) return;
    setPending(true);
    setError(null);
    try {
      await loginWithMagicToken(echoed);
      router.replace(next);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setPending(false);
    }
  };

  return (
    <main className="mx-auto flex min-h-[70vh] max-w-md flex-col justify-center px-4 py-10">
      <h1 className="font-display text-2xl font-extrabold text-slate-900">로그인</h1>
      <p className="mt-2 text-sm text-slate-500">
        이메일로 로그인 링크를 받습니다. 신규 이메일이면 자동 가입되며 가입 보너스 크레딧이
        지급됩니다.
      </p>

      <form onSubmit={onSubmit} className="mt-6 space-y-3">
        <label className="block text-sm">
          <span className="text-slate-600">이메일</span>
          <input
            type="email"
            required
            className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-brand"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="you@example.com"
          />
        </label>
        <label className="block text-sm">
          <span className="text-slate-600">표시 이름 (신규만)</span>
          <input
            type="text"
            className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-brand"
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
            placeholder="홍길동"
          />
        </label>
        <button type="submit" className="btn-primary w-full" disabled={pending || !ready}>
          {pending ? "요청 중…" : "매직링크 받기"}
        </button>
      </form>

      {message && (
        <p className="mt-4 rounded-lg bg-emerald-50 p-3 text-sm text-emerald-800">{message}</p>
      )}
      {echoed && (
        <div className="mt-3 space-y-2 rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm">
          <p className="font-semibold text-amber-900">개발 모드 · 토큰 에코</p>
          <p className="break-all text-xs text-amber-800">{echoed}</p>
          <button type="button" className="btn-primary w-full" disabled={pending} onClick={verifyEcho}>
            이 링크로 로그인
          </button>
        </div>
      )}
      {error && <p className="mt-3 rounded-lg bg-red-50 p-3 text-sm text-red-600">{error}</p>}

      {demoAuth && (
        <div className="mt-8 border-t border-slate-200 pt-6">
          <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">데모 계정</p>
          <div className="mt-2 flex flex-col gap-2">
            {users.map((u) => (
              <button
                key={u.id}
                type="button"
                className="btn-ghost text-left"
                onClick={() => void switchUser(u.id).then(() => router.replace(next))}
              >
                {u.label}
              </button>
            ))}
          </div>
        </div>
      )}

      <p className="mt-8 text-center text-sm text-slate-500">
        <Link href="/" className="text-brand hover:underline">
          랜딩으로
        </Link>
      </p>
    </main>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={<p className="p-6 text-sm text-slate-500">로딩…</p>}>
      <LoginForm />
    </Suspense>
  );
}
