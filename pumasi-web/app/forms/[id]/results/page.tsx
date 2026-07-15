"use client";

import { useState } from "react";
import Link from "next/link";
import { useForm, useResults } from "@/lib/hooks";
import { ApiError } from "@/lib/api";
import ChartCard from "@/components/ChartCard";
import GateBlur from "@/components/GateBlur";
import ResponsesTable from "@/components/ResponsesTable";

type Tab = "summary" | "individual";

export default function ResultsPage({ params }: { params: { id: string } }) {
  const formId = params.id;
  const [tab, setTab] = useState<Tab>("summary");
  const { data: form } = useForm(formId);
  const { data: items, isLoading, isError, error } = useResults(formId);

  // 소유자가 아니면 백엔드가 403 → 게이트 블러
  if (isError && error instanceof ApiError && error.status === 403) {
    return (
      <div className="space-y-4">
        <h1 className="text-xl font-extrabold">결과 대시보드</h1>
        <GateBlur message="본인이 만든 설문의 결과만 열람할 수 있습니다. 상단에서 제작자 계정(u-owner)으로 전환해 보세요." />
      </div>
    );
  }

  const totalResponses = items ? Math.max(0, ...items.map((i) => i.respondentCount)) : 0;

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between gap-2">
        <div>
          <h1 className="text-xl font-extrabold">{form?.title ?? "결과 대시보드"}</h1>
          <p className="text-sm text-slate-500">
            요약은 성실 응답(pass)만 집계합니다. 결과 열람은 무료입니다.
          </p>
        </div>
        <Link href="/" className="btn-ghost">내 설문</Link>
      </div>

      {/* 탭 */}
      <div className="flex gap-1 rounded-xl bg-slate-100 p-1 w-fit">
        {([
          { id: "summary", label: "요약" },
          { id: "individual", label: "개별 응답" },
        ] as { id: Tab; label: string }[]).map((t) => (
          <button
            key={t.id}
            onClick={() => setTab(t.id)}
            className={`rounded-lg px-4 py-1.5 text-sm font-semibold transition ${
              tab === t.id ? "bg-white text-brand shadow-sm" : "text-slate-500 hover:text-slate-700"
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {isLoading && <p className="text-slate-500">집계 불러오는 중…</p>}
      {isError && !(error instanceof ApiError && error.status === 403) && (
        <p className="rounded-lg bg-red-50 p-3 text-sm text-red-600">{(error as Error).message}</p>
      )}

      {tab === "summary" && items && (
        <>
          <div className="flex flex-wrap gap-3">
            <div className="card flex-1">
              <p className="text-xs text-slate-500">최다 응답 문항 기준 응답 수</p>
              <p className="text-2xl font-extrabold text-brand">{totalResponses}</p>
            </div>
            <div className="card flex-1">
              <p className="text-xs text-slate-500">문항 수</p>
              <p className="text-2xl font-extrabold">{items.length}</p>
            </div>
          </div>

          {items.length === 0 && (
            <div className="card text-center text-slate-500">질문이 없습니다.</div>
          )}

          <div className="grid gap-4 sm:grid-cols-2">
            {items.map((item) => (
              <ChartCard key={item.questionId} item={item} />
            ))}
          </div>
        </>
      )}

      {tab === "individual" && (
        <ResponsesTable formId={formId} active={tab === "individual"} />
      )}
    </div>
  );
}
