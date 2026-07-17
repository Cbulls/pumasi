"use client";

import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useCurrentUser } from "@/context/CurrentUserContext";

/** 로그인 필요 화면 가드. 토큰 없으면 /login으로. */
export default function RequireAuth({ children }: { children: React.ReactNode }) {
  const { ready, token } = useCurrentUser();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (!ready) return;
    if (!token) {
      const next = encodeURIComponent(pathname || "/home");
      router.replace(`/login?next=${next}`);
    }
  }, [ready, token, router, pathname]);

  if (!ready) {
    return <p className="p-6 text-sm text-slate-500">세션 확인 중…</p>;
  }
  if (!token) {
    return <p className="p-6 text-sm text-slate-500">로그인 페이지로 이동 중…</p>;
  }
  return <>{children}</>;
}
