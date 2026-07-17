"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { useFeed, type FeedFilters } from "@/lib/hooks";
import { rewardPreview } from "@/lib/format";

export default function FeedPage() {
  const [maxMinutes, setMaxMinutes] = useState<string>("");
  const [minReward, setMinReward] = useState<string>("");
  const [reciprocalOnly, setReciprocalOnly] = useState(false);

  const filters: FeedFilters = useMemo(
    () => ({
      maxMinutes: maxMinutes ? Number(maxMinutes) : null,
      minReward: minReward ? Number(minReward) : null,
      reciprocalOnly,
    }),
    [maxMinutes, minReward, reciprocalOnly]
  );

  const { data, isLoading, isError, error, fetchNextPage, hasNextPage, isFetchingNextPage } =
    useFeed(filters);
  const forms = data?.pages.flat();

  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-xl font-extrabold">응답 피드</h1>
        <p className="text-sm text-slate-500">
          다른 설문에 응답하면 크레딧을 받고, 내게 응답한 사람의 설문이 위에 우선 노출됩니다.
          그 설문에 답해야 내 결과에서 그 사람의 응답이 열립니다.
        </p>
      </div>

      <div className="flex flex-wrap items-end gap-3 rounded-xl border border-slate-200 bg-slate-50 p-3 text-sm">
        <label className="flex flex-col gap-1">
          <span className="text-xs text-slate-500">최대 소요(분)</span>
          <select
            className="rounded-lg border border-slate-300 bg-white px-2 py-1.5"
            value={maxMinutes}
            onChange={(e) => setMaxMinutes(e.target.value)}
          >
            <option value="">전체</option>
            <option value="3">3분 이하</option>
            <option value="5">5분 이하</option>
            <option value="10">10분 이하</option>
          </select>
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-xs text-slate-500">최소 보상</span>
          <select
            className="rounded-lg border border-slate-300 bg-white px-2 py-1.5"
            value={minReward}
            onChange={(e) => setMinReward(e.target.value)}
          >
            <option value="">전체</option>
            <option value="1">1+</option>
            <option value="2">2+</option>
            <option value="5">5+</option>
          </select>
        </label>
        <label className="flex items-center gap-2 pb-1.5">
          <input
            type="checkbox"
            checked={reciprocalOnly}
            onChange={(e) => setReciprocalOnly(e.target.checked)}
          />
          <span>상호 교환 가능만</span>
        </label>
      </div>

      {isLoading && <p className="text-slate-500">불러오는 중…</p>}
      {isError && (
        <p className="rounded-lg bg-red-50 p-3 text-sm text-red-600">
          {(error as Error).message}
        </p>
      )}
      {forms && forms.length === 0 && (
        <div className="card text-center text-slate-500">
          조건에 맞는 설문이 없습니다. 필터를 바꾸거나 나중에 다시 확인해 보세요.
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-2">
        {forms?.map((f) => (
          <div key={f.formId} className="card flex flex-col gap-3">
            <div className="flex items-start justify-between gap-2">
              <h3 className="font-bold">{f.title}</h3>
              <span className="badge bg-brand-light text-brand-dark">
                완료 시 +{rewardPreview(f.costCredits)}
              </span>
            </div>
            {f.description && (
              <p className="line-clamp-2 text-sm text-slate-500">{f.description}</p>
            )}
            <div className="flex items-center gap-3 text-xs text-slate-500">
              <span>약 {f.costCredits}분</span>
              <span>제작자 {f.ownerId}</span>
            </div>
            <Link href={`/forms/${f.formId}/respond`} className="btn-primary mt-auto">
              응답하기
            </Link>
          </div>
        ))}
      </div>

      {hasNextPage && (
        <div className="flex justify-center">
          <button
            type="button"
            className="btn-ghost"
            disabled={isFetchingNextPage}
            onClick={() => void fetchNextPage()}
          >
            {isFetchingNextPage ? "불러오는 중…" : "더 보기"}
          </button>
        </div>
      )}
    </div>
  );
}
