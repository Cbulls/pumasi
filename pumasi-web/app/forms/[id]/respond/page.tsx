"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { useForm, useQuestions, useStartResponse, useSubmitResponse } from "@/lib/hooks";
import { ApiError } from "@/lib/api";
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
  const start = useStartResponse(formId);
  const submit = useSubmitResponse(formId);

  const [answers, setAnswers] = useState<Record<string, string[]>>({});
  const [result, setResult] = useState<SubmitResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [consent, setConsent] = useState(false);
  const [blocked, setBlocked] = useState<string | null>(null);

  // 진입 시 응답 시작을 서버에 기록(소요시간은 서버가 계산)
  const startMutate = start.mutateAsync;
  const startedRef = useRef(false);
  useEffect(() => {
    if (startedRef.current || !form) return;
    startedRef.current = true;
    startMutate().catch((e) => {
      const err = e as ApiError;
      if (err.code === "form.closed" || err.code === "form.not.active") {
        setBlocked("이 설문은 마감되었거나 아직 게시되지 않았습니다.");
      } else if (err.code === "response.own.form") {
        setBlocked("본인 설문에는 응답할 수 없습니다.");
      } else {
        setBlocked(err.message);
      }
    });
  }, [form, startMutate]);

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
      answers: (questions ?? []).map((q) => ({
        questionId: q.questionId,
        values: answers[q.questionId] ?? [],
      })),
      consentAgreed: consent,
    };
    try {
      const res = await submit.mutateAsync(payload);
      setResult(res);
    } catch (e) {
      const err = e as ApiError;
      if (err.code === "form.full") {
        setError("응답 정원이 가득 차서 더 이상 응답을 받을 수 없습니다.");
      } else if (err.code === "form.closed") {
        setError("이 설문은 마감되었습니다.");
      } else {
        setError(err.message);
      }
    }
  };

  if (blocked) {
    return (
      <div className="mx-auto max-w-lg space-y-4 text-center">
        <div className="card space-y-3">
          <h1 className="text-xl font-extrabold">응답할 수 없습니다</h1>
          <p className="text-sm text-slate-500">{blocked}</p>
          <div className="flex justify-center gap-2">
            <Link href="/feed" className="btn-primary">피드로 돌아가기</Link>
            <Link href="/" className="btn-ghost">내 설문</Link>
          </div>
        </div>
      </div>
    );
  }

  if (result) {
    const ui = FLAG_UI[result.qualityFlag];
    return (
      <div className="mx-auto max-w-lg space-y-4 text-center">
        <div className="card space-y-3">
          <span className={`badge mx-auto ${ui.cls}`}>{ui.label}</span>
          <h1 className="text-xl font-extrabold">응답이 제출되었습니다</h1>
          <p className="text-sm text-slate-500">{ui.desc}</p>
          <p className="text-3xl font-extrabold text-brand">+{result.rewardCredited} 크레딧</p>
          <p className="text-xs text-slate-400">
            이 응답은 <b>{result.anonLabel}</b> 로 익명 처리되어 제작자에게 개인 식별자가 노출되지 않습니다.
          </p>
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

      <label className="flex items-start gap-2 rounded-xl bg-slate-100 p-3 text-sm">
        <input
          type="checkbox"
          className="mt-0.5"
          checked={consent}
          onChange={(e) => setConsent(e.target.checked)}
        />
        <span>
          <b>개인정보 수집·이용 동의(필수)</b> — 응답 데이터는 설문 결과 집계 목적으로 수집되며,
          제작자에게는 익명 라벨로만 제공됩니다.
        </span>
      </label>

      {error && <p className="rounded-lg bg-red-50 p-3 text-sm text-red-600">{error}</p>}

      <button
        className="btn-primary w-full"
        onClick={onSubmit}
        disabled={submit.isPending || !consent}
      >
        {submit.isPending ? "제출 중…" : "제출하기"}
      </button>
      <p className="text-center text-xs text-slate-400">
        너무 빨리 제출하거나 불성실하게 응답하면 크레딧이 지급되지 않을 수 있습니다.
      </p>
    </div>
  );
}
