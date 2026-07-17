"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";
import { useCredit, useCreditLedger, usePurchaseCredits } from "@/lib/hooks";
import { useCurrentUser } from "@/context/CurrentUserContext";

const REASON_KO: Record<string, string> = {
  SIGNUP_BONUS: "가입 보너스",
  GENESIS: "초기 지급",
  PURCHASE: "충전",
  ESCROW_DEPOSIT: "게시 예치",
  ESCROW_REFUND: "예치 환불",
  EARN_RESPONSE: "응답 보상",
  BURN: "소각",
};

export default function MePage() {
  const { userId, token, logout, demoAuth } = useCurrentUser();
  const credit = useCredit();
  const ledger = useCreditLedger();
  const purchase = usePurchaseCredits();
  const profile = useQuery({
    queryKey: ["profile", userId],
    queryFn: () =>
      apiFetch<{ userId: string; displayName: string; email: string }>(`/pmsi/auth/profile`, {
        token,
      }),
    enabled: !!token,
  });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-extrabold">내 정보</h1>
        <p className="text-sm text-slate-500">계정 · 크레딧 지갑 · 원장</p>
      </div>

      <section className="card space-y-2">
        <h2 className="font-bold">계정</h2>
        <p className="text-sm">
          <span className="text-slate-500">표시 이름</span>{" "}
          <strong>{profile.data?.displayName || "—"}</strong>
        </p>
        <p className="text-sm">
          <span className="text-slate-500">이메일</span>{" "}
          <strong>{profile.data?.email || "(데모 계정)"}</strong>
        </p>
        <p className="text-xs text-slate-400">ID: {userId}</p>
        <div className="flex flex-wrap gap-2 pt-2">
          <Link href="/login" className="btn-ghost text-xs">
            다른 계정으로 로그인
          </Link>
          {!demoAuth && (
            <button type="button" className="btn-ghost text-xs" onClick={() => void logout()}>
              로그아웃
            </button>
          )}
        </div>
      </section>

      <section className="card space-y-3">
        <h2 className="font-bold">크레딧 지갑</h2>
        <div className="flex gap-4 text-sm">
          <div>
            <p className="text-slate-500">가용</p>
            <p className="text-2xl font-extrabold text-brand">{credit.data?.available ?? "—"}</p>
          </div>
          <div>
            <p className="text-slate-500">예치</p>
            <p className="text-2xl font-extrabold">{credit.data?.escrow ?? "—"}</p>
          </div>
        </div>
        <p className="text-xs text-slate-500">
          게시 시 가용 크레딧이 부족하면 충전이 필요합니다. (베타: Fake 충전)
        </p>
        <button
          type="button"
          className="btn-primary"
          disabled={purchase.isPending}
          onClick={() => purchase.mutate(100)}
        >
          {purchase.isPending ? "충전 중…" : "+100 충전"}
        </button>
      </section>

      <section className="space-y-3">
        <h2 className="font-bold">최근 내역</h2>
        {ledger.isLoading && <p className="text-slate-500">불러오는 중…</p>}
        {ledger.data?.items?.length === 0 && (
          <p className="text-sm text-slate-500">아직 원장 기록이 없습니다.</p>
        )}
        <ul className="divide-y divide-slate-100 rounded-xl border border-slate-200">
          {ledger.data?.items?.map((row) => (
            <li key={row.id} className="flex items-center justify-between px-3 py-2 text-sm">
              <div>
                <p className="font-semibold">{REASON_KO[row.reason] ?? row.reason}</p>
                <p className="text-xs text-slate-400">{row.createdAt}</p>
              </div>
              <span
                className={`font-bold ${row.delta >= 0 ? "text-emerald-600" : "text-red-600"}`}
              >
                {row.delta >= 0 ? `+${row.delta}` : row.delta}
              </span>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}
