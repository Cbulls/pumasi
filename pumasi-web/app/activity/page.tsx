"use client";

import Link from "next/link";
import { useMyActivity, useUnlockOpportunities } from "@/lib/hooks";

const FLAG_CLS: Record<string, string> = {
  pass: "bg-emerald-100 text-emerald-700",
  hold: "bg-amber-100 text-amber-700",
  reject: "bg-red-100 text-red-700",
};

export default function ActivityPage() {
  const { data, isLoading, isError, error } = useMyActivity();
  const unlock = useUnlockOpportunities();

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-extrabold">내 활동</h1>
        <p className="text-sm text-slate-500">
          내가 응답한 설문과, 답하면 결과가 열리는 상대 설문을 모았습니다.
        </p>
      </div>

      <section className="card space-y-3">
        <div className="flex items-center justify-between gap-2">
          <h2 className="font-bold">언락 가능 설문</h2>
          <span className="badge bg-brand-light text-brand-dark">
            {unlock.data?.count ?? 0}개
          </span>
        </div>
        <p className="text-xs text-slate-500">
          상대가 내 설문에 답했습니다. 아래 설문에 응답하면 그 사람의 응답이 결과에서 열립니다.
        </p>
        {unlock.data?.items?.length === 0 && (
          <p className="text-sm text-slate-500">지금 언락을 위해 응답할 설문이 없습니다.</p>
        )}
        <ul className="space-y-2">
          {unlock.data?.items?.map((f) => (
            <li
              key={f.formId}
              className="flex items-center justify-between gap-2 rounded-lg border border-slate-100 px-3 py-2"
            >
              <div>
                <p className="font-semibold">{f.title}</p>
                <p className="text-xs text-slate-500">약 {f.costCredits}분</p>
              </div>
              <Link href={`/forms/${f.formId}/respond`} className="btn-primary text-xs">
                응답하기
              </Link>
            </li>
          ))}
        </ul>
      </section>

      <section className="space-y-3">
        <h2 className="font-bold">내가 제출한 응답</h2>
        {isLoading && <p className="text-slate-500">불러오는 중…</p>}
        {isError && (
          <p className="rounded-lg bg-red-50 p-3 text-sm text-red-600">
            {(error as Error).message}
          </p>
        )}
        {data?.items?.length === 0 && (
          <div className="card text-center text-slate-500">
            아직 응답 이력이 없습니다.{" "}
            <Link href="/feed" className="font-semibold text-brand">
              피드에서 응답하기
            </Link>
          </div>
        )}
        <ul className="space-y-2">
          {data?.items?.map((r) => (
            <li key={r.responseId} className="card flex items-start justify-between gap-3">
              <div>
                <p className="font-semibold">{r.formTitle}</p>
                <p className="mt-1 text-xs text-slate-500">
                  {r.anonLabel} · {r.submittedAt}
                </p>
              </div>
              <span className={`badge ${FLAG_CLS[r.qualityFlag] ?? "bg-slate-100"}`}>
                {r.qualityFlag}
              </span>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}
