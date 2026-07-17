"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useForm, useFunnel, useResults, useSections } from "@/lib/hooks";
import { ApiError } from "@/lib/api";
import type { ChartItem } from "@/lib/types";
import ChartCard from "@/components/ChartCard";
import GateBlur from "@/components/GateBlur";
import ResponsesTable from "@/components/ResponsesTable";

type Tab = "summary" | "individual";

function pct(rate: number): string {
  return `${(rate * 100).toFixed(0)}%`;
}

export default function ResultsPage({ params }: { params: { id: string } }) {
  const formId = params.id;
  const [tab, setTab] = useState<Tab>("summary");
  const { data: form } = useForm(formId);
  const { data: sections } = useSections(formId);
  const { data, isLoading, isError, error } = useResults(formId);
  const { data: funnel } = useFunnel(formId, !isError);

  if (isError && error instanceof ApiError && error.status === 403) {
    return (
      <div className="space-y-4">
        <h1 className="text-xl font-extrabold">결과 대시보드</h1>
        <GateBlur message="본인이 만든 설문의 결과만 열람할 수 있습니다. 상단에서 제작자 계정(u-owner)으로 전환해 보세요." />
      </div>
    );
  }

  const summary = data?.summary;
  const items = data?.items ?? [];

  const grouped = useMemo(() => {
    if (!items.length) return [] as { title: string; items: ChartItem[] }[];
    if (!sections?.length) {
      return [{ title: "", items }];
    }
    const bySection = new Map<string, ChartItem[]>();
    const orphans: ChartItem[] = [];
    for (const item of items) {
      const sid = item.sectionId;
      if (!sid) {
        orphans.push(item);
        continue;
      }
      const list = bySection.get(sid) ?? [];
      list.push(item);
      bySection.set(sid, list);
    }
    const groups: { title: string; items: ChartItem[] }[] = [];
    for (const s of sections) {
      const list = bySection.get(s.sectionId);
      if (list?.length) groups.push({ title: s.title, items: list });
    }
    if (orphans.length) groups.push({ title: "기타", items: orphans });
    return groups.length ? groups : [{ title: "", items }];
  }, [items, sections]);

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between gap-2">
        <div>
          <h1 className="text-xl font-extrabold">{form?.title ?? "결과 대시보드"}</h1>
          <p className="text-sm text-slate-500">
            요약 차트는 상호 응답으로 열린(unlocked) 성실 응답만 집계합니다.
          </p>
        </div>
        <Link href="/home" className="btn-ghost">
          내 설문
        </Link>
      </div>

      <div className="flex w-fit gap-1 rounded-xl bg-slate-100 p-1">
        {(
          [
            { id: "summary" as const, label: "요약" },
            { id: "individual" as const, label: "개별 응답" },
          ] as const
        ).map((t) => (
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

      {tab === "summary" && data && (
        <>
          {summary && (
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
              <div className="card">
                <p className="text-xs text-slate-500">전체 응답</p>
                <p className="text-2xl font-extrabold text-brand">{summary.totalResponses}</p>
              </div>
              <div className="card">
                <p className="text-xs text-slate-500">열린 성실(차트)</p>
                <p className="text-2xl font-extrabold">{summary.unlockedPassCount}</p>
              </div>
              <div className="card">
                <p className="text-xs text-slate-500">잠김</p>
                <p className="text-2xl font-extrabold text-amber-700">{summary.lockedCount}</p>
              </div>
              <div className="card">
                <p className="text-xs text-slate-500">품질 pass / hold / reject</p>
                <p className="text-lg font-extrabold">
                  {summary.passCount}
                  <span className="text-sm font-semibold text-slate-400">
                    {" "}
                    / {summary.holdCount} / {summary.rejectCount}
                  </span>
                </p>
              </div>
              <div className="card">
                <p className="text-xs text-slate-500">언락률</p>
                <p className="text-2xl font-extrabold">{pct(summary.unlockRate)}</p>
              </div>
            </div>
          )}

          {funnel && funnel.viewCount > 0 && (
            <div className="card flex flex-wrap items-center gap-4 text-sm">
              <span className="font-semibold text-slate-700">응답 퍼널</span>
              <span>
                조회 <b>{funnel.viewCount}</b>
              </span>
              <span aria-hidden>→</span>
              <span>
                시작 <b>{funnel.startCount}</b>
              </span>
              <span aria-hidden>→</span>
              <span>
                제출 <b>{funnel.submitCount}</b>
              </span>
              <span className="ml-auto text-slate-500">
                완료율 <b className="text-brand">{funnel.completionRate}%</b>
              </span>
            </div>
          )}

          {summary && summary.totalResponses > 0 && summary.unlockedPassCount === 0 && (
            <div className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
              응답은 있지만 아직 상호 응답으로 열린 성실 응답이 없어 차트에 표시할 데이터가
              없습니다. 상대 설문에 답하면 집계가 채워집니다.
            </div>
          )}

          {items.length === 0 && (
            <div className="card text-center text-slate-500">질문이 없습니다.</div>
          )}

          {grouped.map((g) => (
            <div key={g.title || "all"} className="space-y-3">
              {g.title && (
                <h2 className="font-display text-lg font-bold text-slate-800">{g.title}</h2>
              )}
              <div className="grid gap-4 sm:grid-cols-2">
                {g.items.map((item) => (
                  <ChartCard key={item.questionId} item={item} />
                ))}
              </div>
            </div>
          ))}
        </>
      )}

      {tab === "individual" && (
        <ResponsesTable formId={formId} active={tab === "individual"} />
      )}
    </div>
  );
}
