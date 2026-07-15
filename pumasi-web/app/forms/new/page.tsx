"use client";

import { Suspense, useMemo, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import {
  useAddQuestion,
  useCreateForm,
  useForm,
  usePublishForm,
  useQuestions,
} from "@/lib/hooks";
import QuestionEditor from "@/components/QuestionEditor";
import StatusBadge from "@/components/StatusBadge";
import { rewardPreview } from "@/lib/format";
import type { QuestionType } from "@/lib/types";

const TYPE_KO: Record<QuestionType, string> = {
  SHORT_TEXT: "단답형",
  LONG_TEXT: "장문형",
  RADIO: "단일선택",
  CHECKBOX: "다중선택",
  LINEAR_SCALE: "선형배율",
};

export default function NewFormPage() {
  return (
    <Suspense fallback={<p className="text-slate-500">로딩…</p>}>
      <BuilderInner />
    </Suspense>
  );
}

function BuilderInner() {
  const search = useSearchParams();
  const router = useRouter();
  const [formId, setFormId] = useState(search.get("formId") ?? "");

  return formId ? (
    <Builder formId={formId} onPublished={() => router.push("/")} />
  ) : (
    <CreateForm onCreated={setFormId} />
  );
}

function CreateForm({ onCreated }: { onCreated: (id: string) => void }) {
  const create = useCreateForm();
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [maxResponses, setMaxResponses] = useState(5);

  const submit = async () => {
    const res = await create.mutateAsync({ title: title.trim(), description, maxResponses });
    onCreated(res.formId);
  };

  return (
    <div className="mx-auto max-w-xl space-y-4">
      <h1 className="text-xl font-extrabold">새 설문 만들기</h1>
      <div className="card space-y-3">
        <div>
          <label className="label">제목</label>
          <input className="input" value={title} onChange={(e) => setTitle(e.target.value)} />
        </div>
        <div>
          <label className="label">설명 (선택)</label>
          <textarea
            className="input"
            rows={2}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
        </div>
        <div>
          <label className="label">최대 응답 수</label>
          <input
            type="number"
            min={1}
            className="input"
            value={maxResponses}
            onChange={(e) => setMaxResponses(Number(e.target.value))}
          />
          <p className="mt-1 text-xs text-slate-400">
            게시 시 <b>비용 × 최대 응답 수</b> 만큼 크레딧이 예치(escrow)됩니다.
          </p>
        </div>
        {create.isError && (
          <p className="rounded bg-red-50 p-2 text-sm text-red-600">
            {(create.error as Error).message}
          </p>
        )}
        <button className="btn-primary" onClick={submit} disabled={create.isPending || !title.trim()}>
          {create.isPending ? "생성 중…" : "폼 만들고 질문 추가하기"}
        </button>
      </div>
    </div>
  );
}

function Builder({ formId, onPublished }: { formId: string; onPublished: () => void }) {
  const { data: form } = useForm(formId);
  const { data: questions } = useQuestions(formId);
  const addQuestion = useAddQuestion(formId);
  const publish = usePublishForm(formId);
  const [publishError, setPublishError] = useState<string | null>(null);

  const estimate = useMemo(() => {
    const minutes = (questions ?? []).reduce(
      (acc, q) => acc + (q.type === "LONG_TEXT" ? 2 : 1),
      0
    );
    const cost = Math.max(1, minutes);
    return { cost, escrow: cost * (form?.maxResponses ?? 0) };
  }, [questions, form?.maxResponses]);

  const isDraft = form?.status === "DRAFT";

  const doPublish = async () => {
    setPublishError(null);
    try {
      await publish.mutateAsync();
      onPublished();
    } catch (e) {
      setPublishError((e as Error).message);
    }
  };

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between gap-2">
        <div>
          <h1 className="text-xl font-extrabold">{form?.title ?? "폼 편집"}</h1>
          <p className="text-xs text-slate-500">formId: {formId}</p>
        </div>
        {form && <StatusBadge status={form.status} />}
      </div>

      {isDraft && <QuestionEditor onAdd={(q) => addQuestion.mutateAsync(q)} />}

      <div className="card">
        <h3 className="mb-3 font-bold">질문 목록 ({questions?.length ?? 0})</h3>
        {(!questions || questions.length === 0) && (
          <p className="text-sm text-slate-400">아직 질문이 없습니다.</p>
        )}
        <ol className="space-y-2">
          {questions?.map((q, i) => (
            <li key={q.questionId} className="flex items-start gap-2 rounded-lg bg-slate-50 p-3">
              <span className="mt-0.5 text-sm font-bold text-brand">{i + 1}</span>
              <div className="flex-1">
                <div className="flex items-center gap-2">
                  <span className="font-medium">{q.title}</span>
                  {q.required && <span className="text-red-500">*</span>}
                  <span className="badge bg-slate-200 text-slate-600">{TYPE_KO[q.type]}</span>
                </div>
                {q.options && q.options.length > 0 && (
                  <p className="mt-1 text-xs text-slate-500">보기: {q.options.join(", ")}</p>
                )}
                {q.type === "LINEAR_SCALE" && (
                  <p className="mt-1 text-xs text-slate-500">
                    척도: {q.scaleMin} ~ {q.scaleMax}
                  </p>
                )}
              </div>
            </li>
          ))}
        </ol>
      </div>

      {isDraft ? (
        <div className="card space-y-3">
          <div className="text-sm text-slate-600">
            예상 비용 <b>{estimate.cost} 크레딧/응답</b> · 게시 시 예치{" "}
            <b>{estimate.escrow} 크레딧</b> · 응답자 보상 <b>+{rewardPreview(estimate.cost)}</b>
          </div>
          {publishError && (
            <p className="rounded bg-red-50 p-2 text-sm text-red-600">{publishError}</p>
          )}
          <button
            className="btn-primary"
            onClick={doPublish}
            disabled={publish.isPending || (questions?.length ?? 0) === 0}
          >
            {publish.isPending ? "게시 중…" : "게시하고 예치하기"}
          </button>
        </div>
      ) : (
        <div className="card flex items-center justify-between">
          <span className="text-sm text-emerald-700">게시 완료! 이제 피드에 노출됩니다.</span>
          <div className="flex gap-2">
            <Link href={`/forms/${formId}/results`} className="btn-ghost">결과</Link>
            <Link href="/" className="btn-primary">대시보드</Link>
          </div>
        </div>
      )}
    </div>
  );
}
