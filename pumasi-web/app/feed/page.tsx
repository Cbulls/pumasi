"use client";

import Link from "next/link";
import { useFeed } from "@/lib/hooks";
import { rewardPreview } from "@/lib/format";

export default function FeedPage() {
  const { data: forms, isLoading, isError, error } = useFeed();

  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-xl font-extrabold">응답 피드</h1>
        <p className="text-sm text-slate-500">
          다른 설문에 응답하면 크레딧을 받고, 내게 응답한 사람의 설문이 위에 우선 노출됩니다.
          그 설문에 답해야 내 결과에서 그 사람의 응답이 열립니다.
        </p>
      </div>

      {isLoading && <p className="text-slate-500">불러오는 중…</p>}
      {isError && (
        <p className="rounded-lg bg-red-50 p-3 text-sm text-red-600">
          {(error as Error).message}
        </p>
      )}
      {forms && forms.length === 0 && (
        <div className="card text-center text-slate-500">
          지금 응답할 수 있는 설문이 없습니다. 다른 계정으로 설문을 게시해 보세요.
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
              <span>⏱ 약 {f.costCredits}분</span>
              <span>제작자 {f.ownerId}</span>
            </div>
            <Link href={`/forms/${f.formId}/respond`} className="btn-primary mt-auto">
              응답하기
            </Link>
          </div>
        ))}
      </div>
    </div>
  );
}
