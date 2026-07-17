"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import {
  recordEvent,
  useForm,
  useSections,
  useStartResponse,
  useSubmitResponse,
} from "@/lib/hooks";
import { useCurrentUser } from "@/context/CurrentUserContext";
import { ApiError } from "@/lib/api";
import AnswerInput from "@/components/AnswerInput";
import ProgressBar from "@/components/ProgressBar";
import type { QuestionVO, SectionVO, SubmitResult } from "@/lib/types";

const FLAG_UI: Record<SubmitResult["qualityFlag"], { label: string; cls: string; desc: string }> = {
  pass: {
    label: "통과",
    cls: "bg-emerald-100 text-emerald-700",
    desc: "성실한 응답으로 판정되어 크레딧이 지급되었습니다.",
  },
  hold: {
    label: "보류",
    cls: "bg-amber-100 text-amber-700",
    desc: "일부 응답이 검토 대상입니다. 크레딧은 지급되지 않았습니다.",
  },
  reject: {
    label: "거절",
    cls: "bg-red-100 text-red-700",
    desc: "불성실 응답으로 판정되어 크레딧이 지급되지 않았습니다(데이터는 저장됨).",
  },
};

const CONTENT = new Set(["DESCRIPTION", "IMAGE"]);

function nextSectionId(
  sections: SectionVO[],
  current: SectionVO,
  answers: Record<string, string[]>
): string | null {
  const ordered = [...sections].sort((a, b) => a.orderIndex - b.orderIndex);
  for (const q of current.questions ?? []) {
    if (q.type !== "RADIO" || !q.branchRules) continue;
    const choice = answers[q.questionId]?.[0];
    if (choice && q.branchRules[choice]) return q.branchRules[choice];
    if (q.branchRules._default) return q.branchRules._default;
  }
  const idx = ordered.findIndex((s) => s.sectionId === current.sectionId);
  if (idx >= 0 && idx + 1 < ordered.length) return ordered[idx + 1].sectionId;
  return null;
}

export default function RespondPage({ params }: { params: { id: string } }) {
  const formId = params.id;
  const { data: form } = useForm(formId);
  const { data: sections } = useSections(formId);
  const start = useStartResponse(formId);
  const submit = useSubmitResponse(formId);
  const { token } = useCurrentUser();

  const [answers, setAnswers] = useState<Record<string, string[]>>({});
  const [result, setResult] = useState<SubmitResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [consent, setConsent] = useState(false);
  const [blocked, setBlocked] = useState<string | null>(null);
  const [sectionId, setSectionId] = useState<string | null>(null);
  const [path, setPath] = useState<string[]>([]);

  const orderedSections = useMemo(
    () => [...(sections ?? [])].sort((a, b) => a.orderIndex - b.orderIndex),
    [sections]
  );

  useEffect(() => {
    if (orderedSections.length && !sectionId) {
      setSectionId(orderedSections[0].sectionId);
      setPath([orderedSections[0].sectionId]);
    }
  }, [orderedSections, sectionId]);

  const current = orderedSections.find((s) => s.sectionId === sectionId) ?? null;
  const questions: QuestionVO[] = current?.questions ?? [];

  const startMutate = start.mutateAsync;
  const startedRef = useRef(false);
  useEffect(() => {
    if (startedRef.current || !form) return;
    startedRef.current = true;
    recordEvent(token, formId, "view");
    startMutate()
      .then(() => recordEvent(token, formId, "start"))
      .catch((e) => {
        const err = e as ApiError;
        if (err.code === "form.closed" || err.code === "form.not.active") {
          setBlocked("이 설문은 마감되었거나 아직 게시되지 않았습니다.");
        } else if (err.code === "response.own.form") {
          setBlocked("본인 설문에는 응답할 수 없습니다.");
        } else {
          setBlocked(err.message);
        }
      });
  }, [form, formId, startMutate, token]);

  const answerable = useMemo(
    () =>
      (sections ?? [])
        .flatMap((s) => s.questions ?? [])
        .filter((q) => !CONTENT.has(q.type)),
    [sections]
  );
  const answeredCount = useMemo(
    () =>
      answerable.filter((q) =>
        (answers[q.questionId] ?? []).some((x) => x && x.trim() !== "")
      ).length,
    [answers, answerable]
  );
  const progress = answerable.length ? (answeredCount / answerable.length) * 100 : 0;

  const goNext = () => {
    if (!current || !sections) return;
    const nid = nextSectionId(sections, current, answers);
    if (!nid) {
      // last page — submit handled separately
      return;
    }
    setSectionId(nid);
    setPath((p) => (p.includes(nid) ? p : [...p, nid]));
  };

  const goPrev = () => {
    setPath((p) => {
      if (p.length <= 1) return p;
      const next = p.slice(0, -1);
      setSectionId(next[next.length - 1]);
      return next;
    });
  };

  const isLast = current
    ? !nextSectionId(orderedSections, current, answers)
    : true;

  const onSubmit = async () => {
    setError(null);
    const allQs = (sections ?? []).flatMap((s) => s.questions ?? []);
    const payload = {
      answers: allQs
        .filter((q) => !CONTENT.has(q.type))
        .map((q) => ({
          questionId: q.questionId,
          values: answers[q.questionId] ?? [],
        })),
      consentAgreed: consent,
    };
    try {
      const res = await submit.mutateAsync(payload);
      recordEvent(token, formId, "submit");
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
            <Link href="/feed" className="btn-primary">
              피드로 돌아가기
            </Link>
            <Link href="/home" className="btn-ghost">
              내 설문
            </Link>
          </div>
        </div>
      </div>
    );
  }

  if (result) {
    const ui = FLAG_UI[result.qualityFlag];
    const custom =
      form?.confirmationMessage && form.confirmationMessage.trim()
        ? form.confirmationMessage.trim()
        : null;
    return (
      <div className="mx-auto max-w-lg space-y-4 text-center">
        <div className="card space-y-3">
          <span className={`badge mx-auto ${ui.cls}`}>{ui.label}</span>
          <h1 className="text-xl font-extrabold">응답이 제출되었습니다</h1>
          {custom && <p className="text-sm font-medium text-slate-700">{custom}</p>}
          <p className="text-sm text-slate-500">{ui.desc}</p>
          <p className="text-3xl font-extrabold text-brand">+{result.rewardCredited} 크레딧</p>
          <div className="flex justify-center gap-2">
            <Link href="/feed" className="btn-primary">
              피드로 돌아가기
            </Link>
            <Link href="/home" className="btn-ghost">
              내 설문
            </Link>
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
        {form?.closesAt && (
          <p className="mt-1 text-xs text-slate-400">
            마감: {new Date(form.closesAt).toLocaleString("ko-KR")}
          </p>
        )}
      </div>

      <div className="sticky top-16 z-10 space-y-1 rounded-xl bg-white/90 p-3 shadow-sm backdrop-blur">
        <div className="flex justify-between text-xs text-slate-500">
          <span>
            {current?.title ?? "섹션"} · 진행 {answeredCount}/{answerable.length}
          </span>
          <span>
            {path.length}/{orderedSections.length || 1} 페이지
          </span>
        </div>
        <ProgressBar value={progress} />
      </div>

      <div className="space-y-4">
        {questions.map((q, i) => (
          <div key={q.questionId} className="card space-y-2">
            {!CONTENT.has(q.type) && (
              <div className="flex items-center gap-2">
                <span className="text-sm font-bold text-brand">Q{i + 1}</span>
                <span className="font-medium">{q.title}</span>
                {q.required && <span className="text-red-500">*</span>}
              </div>
            )}
            {CONTENT.has(q.type) && <h3 className="font-bold">{q.title}</h3>}
            <AnswerInput
              formId={formId}
              question={q}
              value={answers[q.questionId] ?? []}
              onChange={(vals) =>
                setAnswers((prev) => ({ ...prev, [q.questionId]: vals }))
              }
            />
          </div>
        ))}
      </div>

      {isLast && (
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
      )}

      {error && <p className="rounded-lg bg-red-50 p-3 text-sm text-red-600">{error}</p>}

      <div className="flex gap-2">
        {path.length > 1 && (
          <button type="button" className="btn-ghost" onClick={goPrev}>
            이전
          </button>
        )}
        {!isLast ? (
          <button type="button" className="btn-primary flex-1" onClick={goNext}>
            다음
          </button>
        ) : (
          <button
            className="btn-primary flex-1"
            onClick={onSubmit}
            disabled={submit.isPending || !consent}
          >
            {submit.isPending ? "제출 중…" : "제출하기"}
          </button>
        )}
      </div>
    </div>
  );
}
