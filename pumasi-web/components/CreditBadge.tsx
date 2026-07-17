"use client";

import { useCredit, usePurchaseCredits } from "@/lib/hooks";

const PURCHASE_AMOUNT = 100;

/** 상단 헤더의 크레딧 배지 (가용/예치) + 베타 충전 버튼. */
export default function CreditBadge() {
  const { data, isLoading, isError } = useCredit();
  const purchase = usePurchaseCredits();

  if (isLoading) {
    return <span className="badge bg-slate-100 text-slate-500">크레딧 …</span>;
  }
  if (isError || !data) {
    return <span className="badge bg-slate-100 text-slate-400">크레딧 -</span>;
  }
  return (
    <span className="flex items-center gap-1">
      <span
        className="badge gap-1 bg-brand-light text-brand-dark"
        title={`가용 ${data.available} / 예치 ${data.escrow}`}
      >
        <span aria-hidden>◈</span>
        {data.available}
        {data.escrow > 0 && (
          <span className="ml-1 text-brand/60">(예치 {data.escrow})</span>
        )}
      </span>
      <button
        type="button"
        className="rounded-lg border border-slate-300 px-2 py-1 text-xs font-semibold text-slate-600 hover:border-brand hover:text-brand disabled:opacity-50"
        title={`베타: ${PURCHASE_AMOUNT} 크레딧 충전(가짜 결제)`}
        disabled={purchase.isPending}
        onClick={() => purchase.mutate(PURCHASE_AMOUNT)}
      >
        {purchase.isPending ? "충전 중…" : "+충전"}
      </button>
    </span>
  );
}
