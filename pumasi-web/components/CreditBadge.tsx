"use client";

import { useCredit } from "@/lib/hooks";

/** 상단 헤더의 크레딧 배지 (가용/예치). */
export default function CreditBadge() {
  const { data, isLoading, isError } = useCredit();

  if (isLoading) {
    return <span className="badge bg-slate-100 text-slate-500">크레딧 …</span>;
  }
  if (isError || !data) {
    return <span className="badge bg-slate-100 text-slate-400">크레딧 -</span>;
  }
  return (
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
  );
}
