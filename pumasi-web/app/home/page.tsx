"use client";

import Link from "next/link";
import { useCloseForm, useMyForms, useResumeForm } from "@/lib/hooks";
import { useCurrentUser } from "@/context/CurrentUserContext";
import StatusBadge from "@/components/StatusBadge";
import { rewardPreview } from "@/lib/format";
import type { FormVO } from "@/lib/types";

function ResumeFormButton({ form }: { form: FormVO }) {
  const resume = useResumeForm(form.formId);
  return (
    <button
      className="btn-primary"
      disabled={resume.isPending}
      onClick={() => resume.mutate()}
    >
      {resume.isPending ? "재개 중…" : "게시 재개"}
    </button>
  );
}

function CloseFormButton({ form }: { form: FormVO }) {
  const close = useCloseForm(form.formId);
  return (
    <button
      className="btn-ghost"
      disabled={close.isPending}
      onClick={() => {
        if (window.confirm("설문을 마감할까요? 남은 예치 크레딧은 환불됩니다.")) {
          close.mutate();
        }
      }}
    >
      {close.isPending ? "마감 중…" : "마감"}
    </button>
  );
}

export default function DashboardPage() {
  const { userId } = useCurrentUser();
  const { data: forms, isLoading, isError, error } = useMyForms();

  return (
    <div className="space-y-6">
      <section className="flex flex-col gap-3 rounded-2xl bg-gradient-to-br from-brand to-indigo-500 p-6 text-white sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-extrabold">내 설문</h1>
          <p className="mt-1 text-sm text-white/80">
            남의 설문에 응답해 크레딧을 벌고, 그 크레딧으로 내 설문의 응답자를 모으세요.
          </p>
          <p className="mt-1 text-xs text-white/60">현재 사용자: {userId ?? "로그인 중…"}</p>
        </div>
        <div className="flex gap-2">
          <Link href="/forms/new" className="btn bg-white text-brand hover:bg-white/90">
            + 새 설문 만들기
          </Link>
          <Link href="/feed" className="btn border border-white/40 text-white hover:bg-white/10">
            응답 피드
          </Link>
        </div>
      </section>

      {isLoading && <p className="text-slate-500">불러오는 중…</p>}
      {isError && (
        <p className="rounded-lg bg-red-50 p-3 text-sm text-red-600">
          목록을 불러오지 못했습니다: {(error as Error)?.message}
        </p>
      )}

      {forms && forms.length === 0 && (
        <div className="card text-center text-slate-500">
          아직 만든 설문이 없습니다.{" "}
          <Link href="/forms/new" className="font-semibold text-brand">
            첫 설문 만들기
          </Link>
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-2">
        {forms?.map((f) => (
          <div key={f.formId} className="card flex flex-col gap-3">
            <div className="flex items-start justify-between gap-2">
              <h3 className="font-bold">{f.title}</h3>
              <StatusBadge status={f.status} />
            </div>
            {f.description && (
              <p className="line-clamp-2 text-sm text-slate-500">{f.description}</p>
            )}
            <div className="text-xs text-slate-500 space-y-1">
              <div>
                {f.status === "ACTIVE" ? (
                  <>응답 1건당 비용 {f.costCredits} · 보상 +{rewardPreview(f.costCredits)} · 최대 {f.maxResponses}건</>
                ) : f.status === "PAUSED" ? (
                  <>가드레일로 일시정지됨 — 불성실 응답이 몰려 자동으로 멈췄습니다</>
                ) : f.status === "CLOSED" ? (
                  <>마감됨 · 최대 {f.maxResponses}건</>
                ) : (
                  <>초안 · 최대 {f.maxResponses}건</>
                )}
              </div>
              {f.closesAt && (
                <div>마감: {new Date(f.closesAt).toLocaleString("ko-KR")}</div>
              )}
            </div>
            <div className="mt-auto flex flex-wrap gap-2">
              {f.status === "DRAFT" ? (
                <Link href={`/forms/new?formId=${f.formId}`} className="btn-primary flex-1">
                  이어서 편집 / 게시
                </Link>
              ) : (
                <>
                  <Link href={`/forms/${f.formId}/results`} className="btn-primary flex-1">
                    결과 보기
                  </Link>
                  {f.status === "ACTIVE" && f.shareToken && (
                    <button
                      type="button"
                      className="btn-ghost"
                      onClick={() =>
                        navigator.clipboard.writeText(
                          `${window.location.origin}/s/${f.shareToken}`
                        )
                      }
                    >
                      공유 링크
                    </button>
                  )}
                  {f.status === "ACTIVE" && <CloseFormButton form={f} />}
                  {f.status === "PAUSED" && <ResumeFormButton form={f} />}
                </>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
