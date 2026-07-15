"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useForm, useQuestions, useSubmitResponse } from "@/lib/hooks";
import AnswerInput from "@/components/AnswerInput";
import ProgressBar from "@/components/ProgressBar";
import type { SubmitResult } from "@/lib/types";

const FLAG_UI: Record<SubmitResult["qualityFlag"], { label: string; cls: string; desc: string }> = {
  pass: { label: "통과", cls: "bg-emerald-100 text-emerald-700", desc: "성실한 응답으로 판정되어 크레딧이 지급되었습니다." },
  hold: { label: "보류", cls: "bg-amber-100 text-amber-700", desc: "일부 응답이 검토 대상입니다. 크레딧은 지급되지 않았습니다." },
  reject: { label: "거절", cls: "bg-red-100 text-red-700", desc: "불성실 응답으로 판정되어 크레딧이 지급되지 않았습니다(데이터는 저장됨)." },
};

export default function RespondPage({ params }: { params: { id: string } }) {
  const formId = params.id;
  const { data: form } = useForm(formId);
  const { data: questions } = useQuestions(formId);
  const submit = useSubmitResponse(formId);

  const [answers, setAnswers] = useState<Record<string, string[]>>({});
  const [startedAt] = useState(() => Date.now());
  const [result, setResult] = useState<SubmitResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const answeredCount = useMemo(
    () =>
      Object.values(answers).filter((v) => v.some((x) => x && x.trim() !== "")).length,
    [answers]
  );
  const total = questions?.length ?? 0;
  const progress = total ? (answeredCount / total) * 100 : 0;

  const onSubmit = async () => {
    setError(null);
    const payload = {
      elapsedSeconds: Math.floor((Date.now() - startedAt) / 1000),
      answers: (questions ?? []).map((q) => ({
        questionId: q.questionId,
        values: answers[q.questionId] ?? [],
      })),
    };
    try {
      const res = await submit.mutateAsync(payload);
      setResult(res);
    } catch (e) {
      setError((e as Error).message);
    }
  };

  if (result) {
    const ui = FLAG_UI[result.qualityFlag];
    return (
      <div className="mx-auto max-w-lg space-y-4 text-center">
        <div className="card space-y-3">
          <span className={`badge mx-auto ${ui.cls}`}>{ui.label}</span>
          <h1 className="text-xl font-extrabold">응답이 제출되었습니다</h1>
          <p className="text-sm text-slate-500">{ui.desc}</p>
          <p className="text-3xl font-extrabold text-brand">+{result.rewardCredited} 크레딧</p>
          <div className="flex justify-center gap-2">
            <Link href="/feed" className="btn-primary">피드로 돌아가기</Link>
            <Link href="/" className="btn-ghost">내 설문</Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl space-y-5">
      <div>
        <h1 className="text-xl font-extrabold">{form?.title ?? "설문 응답"}</h1>
        {form?.description && <p className="text-sm text-slate-500">{form.description}</p>}
      </div>

      <div className="sticky top-16 z-10 space-y-1 rounded-xl bg-white/90 p-3 shadow-sm backdrop-blur">
        <div className="flex justify-between text-xs text-slate-500">
          <span>진행률 {answeredCount}/{total}</span>
          {form && <span>완료 시 보상 지급</span>}
        </div>
        <ProgressBar value={progress} />
      </div>

      <div className="space-y-4">
        {questions?.map((q, i) => (
          <div key={q.questionId} className="card space-y-2">
            <div className="flex items-center gap-2">
              <span className="text-sm font-bold text-brand">Q{i + 1}</span>
              <span className="font-medium">{q.title}</span>
              {q.required && <span className="text-red-500">*</span>}
            </div>
            <AnswerInput
              question={q}
              value={answers[q.questionId] ?? []}
              onChange={(vals) =>
                setAnswers((prev) => ({ ...prev, [q.questionId]: vals }))
              }
            />
          </div>
        ))}
      </div>

      {error && <p className="rounded-lg bg-red-50 p-3 text-sm text-red-600">{error}</p>}

      <button className="btn-primary w-full" onClick={onSubmit} disabled={submit.isPending}>
        {submit.isPending ? "제출 중…" : "제출하기"}
      </button>
      <p className="text-center text-xs text-slate-400">
        너무 빨리 제출하거나 불성실하게 응답하면 크레딧이 지급되지 않을 수 있습니다.
      </p>
    </div>
  );
}
