"use client";

import { Suspense, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import {
  useAddQuestion,
  useAddSection,
  useCreateForm,
  useDeleteQuestion,
  useDeleteSection,
  useForm,
  usePublishForm,
  useQuestions,
  useReorderQuestions,
  useSections,
  useUpdateForm,
  useUpdateQuestion,
  useUpdateSection,
} from "@/lib/hooks";
import QuestionEditor from "@/components/QuestionEditor";
import ShareQr from "@/components/ShareQr";
import StatusBadge from "@/components/StatusBadge";
import { useCurrentUser } from "@/context/CurrentUserContext";
import { apiFetch } from "@/lib/api";
import { rewardPreview } from "@/lib/format";
import { FORM_TEMPLATES, getTemplate } from "@/lib/templates";
import type { QuestionType, QuestionVO } from "@/lib/types";

const TYPE_KO: Record<QuestionType, string> = {
  SHORT_TEXT: "단답형",
  LONG_TEXT: "장문형",
  RADIO: "단일선택",
  CHECKBOX: "다중선택",
  DROPDOWN: "드롭다운",
  LINEAR_SCALE: "선형배율",
  RATING: "별점",
  DATE: "날짜",
  TIME: "시간",
  MULTIPLE_CHOICE_GRID: "객관식 그리드",
  CHECKBOX_GRID: "체크박스 그리드",
  DESCRIPTION: "설명",
  IMAGE: "이미지",
  FILE: "파일",
};

const CONTENT = new Set<QuestionType>(["DESCRIPTION", "IMAGE"]);

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
  const templateId = search.get("template");

  return formId ? (
    <Builder formId={formId} onPublished={() => router.push("/home")} />
  ) : (
    <CreateForm
      onCreated={setFormId}
      initialTemplateId={templateId}
      onTemplatePick={(id) => router.replace(`/forms/new?template=${id}`)}
    />
  );
}

function CreateForm({
  onCreated,
  initialTemplateId,
  onTemplatePick,
}: {
  onCreated: (id: string) => void;
  initialTemplateId?: string | null;
  onTemplatePick: (id: string) => void;
}) {
  const create = useCreateForm();
  const { token } = useCurrentUser();
  const tpl = getTemplate(initialTemplateId);
  const [title, setTitle] = useState(tpl?.formTitle ?? "");
  const [description, setDescription] = useState(tpl?.formDescription ?? "");
  const [maxResponses, setMaxResponses] = useState(tpl?.maxResponses ?? 5);
  const [closesAt, setClosesAt] = useState("");
  const [applying, setApplying] = useState(false);
  const [applyError, setApplyError] = useState<string | null>(null);

  const submit = async () => {
    setApplying(true);
    setApplyError(null);
    try {
      const res = await create.mutateAsync({
        title: title.trim(),
        description,
        maxResponses,
        closesAt: closesAt ? new Date(closesAt).toISOString() : null,
      });
      if (tpl) {
        for (const q of tpl.questions) {
          await apiFetch(`/pmsi/form/${res.formId}/questions`, {
            method: "POST",
            token,
            body: {
              type: q.type,
              title: q.title,
              required: q.required ?? false,
              options: q.options,
              scaleMin: q.scaleMin,
              scaleMax: q.scaleMax,
              bodyHtml: q.bodyHtml,
            },
          });
        }
      }
      onCreated(res.formId);
    } catch (e) {
      setApplyError((e as Error).message);
    } finally {
      setApplying(false);
    }
  };

  return (
    <div className="mx-auto max-w-xl space-y-4">
      <h1 className="text-xl font-extrabold">새 설문 만들기</h1>

      <div className="space-y-2">
        <p className="text-sm font-semibold text-slate-600">템플릿으로 시작</p>
        <div className="grid gap-2 sm:grid-cols-3">
          {FORM_TEMPLATES.map((t) => (
            <button
              key={t.id}
              type="button"
              className={`rounded-xl border p-3 text-left text-sm transition ${
                tpl?.id === t.id
                  ? "border-brand bg-brand-light"
                  : "border-slate-200 hover:border-brand"
              }`}
              onClick={() => {
                onTemplatePick(t.id);
                setTitle(t.formTitle);
                setDescription(t.formDescription);
                setMaxResponses(t.maxResponses);
              }}
            >
              <p className="font-bold">{t.title}</p>
              <p className="mt-1 text-xs text-slate-500">{t.description}</p>
            </button>
          ))}
        </div>
      </div>

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
            게시 시 <b>비용 × 최대 응답 수</b> 만큼 크레딧이 예치됩니다.
          </p>
        </div>
        <div>
          <label className="label">마감일시 (선택)</label>
          <input
            type="datetime-local"
            className="input"
            value={closesAt}
            onChange={(e) => setClosesAt(e.target.value)}
          />
        </div>
        {(create.isError || applyError) && (
          <p className="rounded bg-red-50 p-2 text-sm text-red-600">
            {applyError ?? (create.error as Error).message}
          </p>
        )}
        <button
          className="btn-primary"
          onClick={submit}
          disabled={create.isPending || applying || !title.trim()}
        >
          {create.isPending || applying
            ? "생성 중…"
            : tpl
              ? `템플릿으로 만들기 (${tpl.title})`
              : "폼 만들고 질문 추가하기"}
        </button>
      </div>
    </div>
  );
}

function Builder({ formId, onPublished }: { formId: string; onPublished: () => void }) {
  const { data: form } = useForm(formId);
  const { data: questions } = useQuestions(formId);
  const { data: sections } = useSections(formId);
  const addQuestion = useAddQuestion(formId);
  const updateQuestion = useUpdateQuestion(formId);
  const deleteQuestion = useDeleteQuestion(formId);
  const reorder = useReorderQuestions(formId);
  const addSection = useAddSection(formId);
  const updateSection = useUpdateSection(formId);
  const deleteSection = useDeleteSection(formId);
  const updateForm = useUpdateForm(formId);
  const publish = usePublishForm(formId);

  const [publishError, setPublishError] = useState<string | null>(null);
  const [editing, setEditing] = useState<QuestionVO | null>(null);
  const [closesLocal, setClosesLocal] = useState("");
  const [confirmMsg, setConfirmMsg] = useState("");

  useEffect(() => {
    if (form?.confirmationMessage != null) setConfirmMsg(form.confirmationMessage);
  }, [form?.confirmationMessage]);

  const estimate = useMemo(() => {
    const minutes = (questions ?? []).reduce((acc, q) => {
      if (CONTENT.has(q.type)) return acc;
      return acc + (q.type === "LONG_TEXT" ? 2 : 1);
    }, 0);
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

  const move = async (qid: string, dir: -1 | 1) => {
    if (!questions) return;
    const ids = questions.map((q) => q.questionId);
    const i = ids.indexOf(qid);
    const j = i + dir;
    if (i < 0 || j < 0 || j >= ids.length) return;
    [ids[i], ids[j]] = [ids[j], ids[i]];
    await reorder.mutateAsync(ids);
  };

  const shareUrl =
    typeof window !== "undefined" && form?.shareToken
      ? `${window.location.origin}/s/${form.shareToken}`
      : form?.shareToken
        ? `/s/${form.shareToken}`
        : "";

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between gap-2">
        <div>
          <h1 className="text-xl font-extrabold">{form?.title ?? "폼 편집"}</h1>
          <p className="text-xs text-slate-500">formId: {formId}</p>
        </div>
        {form && <StatusBadge status={form.status} />}
      </div>

      {form && (
        <div className="card space-y-3">
          <h3 className="font-bold">설문 설정</h3>
          <div className="grid gap-3 sm:grid-cols-2">
            <div>
              <label className="label">최대 응답 수</label>
              <input
                type="number"
                min={1}
                className="input"
                disabled={!isDraft}
                defaultValue={form.maxResponses}
                onBlur={(e) => {
                  if (!isDraft) return;
                  const n = Number(e.target.value);
                  if (n > 0 && n !== form.maxResponses) {
                    updateForm.mutate({ maxResponses: n, closesAt: form.closesAt });
                  }
                }}
              />
            </div>
            <div>
              <label className="label">마감일시</label>
              <input
                type="datetime-local"
                className="input"
                value={
                  closesLocal ||
                  (form.closesAt ? form.closesAt.slice(0, 16) : "")
                }
                onChange={(e) => setClosesLocal(e.target.value)}
                onBlur={() => {
                  const iso = closesLocal ? new Date(closesLocal).toISOString() : null;
                  updateForm.mutate({
                    title: form.title,
                    description: form.description,
                    maxResponses: form.maxResponses,
                    closesAt: iso,
                    confirmationMessage: confirmMsg,
                  });
                }}
              />
            </div>
          </div>
          {isDraft && (
            <div>
              <label className="label">제출 완료 메시지 (선택)</label>
              <input
                className="input"
                value={confirmMsg}
                onChange={(e) => setConfirmMsg(e.target.value)}
                onBlur={() =>
                  updateForm.mutate({
                    title: form.title,
                    description: form.description,
                    maxResponses: form.maxResponses,
                    closesAt: form.closesAt,
                    confirmationMessage: confirmMsg,
                  })
                }
                placeholder="응답해 주셔서 감사합니다!"
                maxLength={500}
              />
            </div>
          )}
          {form.closesAt && (
            <p className="text-xs text-slate-500">
              마감: {new Date(form.closesAt).toLocaleString("ko-KR")}
            </p>
          )}
          {shareUrl && (
            <div className="flex flex-wrap items-start gap-4 text-sm">
              <div className="space-y-2">
                <span className="text-slate-500">공유 링크</span>
                <div className="flex flex-wrap items-center gap-2">
                  <code className="rounded bg-slate-100 px-2 py-1 text-xs">{shareUrl}</code>
                  <button
                    type="button"
                    className="btn-ghost"
                    onClick={() => navigator.clipboard.writeText(shareUrl)}
                  >
                    복사
                  </button>
                </div>
              </div>
              <div>
                <p className="mb-1 text-slate-500">QR</p>
                <ShareQr url={shareUrl} size={128} />
              </div>
            </div>
          )}
        </div>
      )}

      {isDraft && (
        <div className="card space-y-2">
          <div className="flex items-center justify-between">
            <h3 className="font-bold">섹션</h3>
            <button
              type="button"
              className="text-sm font-semibold text-brand"
              onClick={() => addSection.mutate(`섹션 ${(sections?.length ?? 0) + 1}`)}
            >
              + 섹션 추가
            </button>
          </div>
          <ul className="space-y-2">
            {sections?.map((s) => (
              <li key={s.sectionId} className="flex items-center gap-2">
                <input
                  className="input"
                  defaultValue={s.title}
                  onBlur={(e) => {
                    const t = e.target.value.trim();
                    if (t && t !== s.title) {
                      updateSection.mutate({ sectionId: s.sectionId, title: t });
                    }
                  }}
                />
                {(sections?.length ?? 0) > 1 && (
                  <button
                    type="button"
                    className="btn-ghost text-red-600"
                    onClick={() => {
                      if (window.confirm("빈 섹션만 삭제됩니다. 삭제할까요?")) {
                        deleteSection.mutate(s.sectionId);
                      }
                    }}
                  >
                    삭제
                  </button>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}

      {isDraft && !editing && (
        <QuestionEditor
          formId={formId}
          sections={sections}
          onSubmit={async (q) => {
            await addQuestion.mutateAsync(q);
          }}
        />
      )}
      {isDraft && editing && (
        <QuestionEditor
          formId={formId}
          sections={sections}
          initial={editing}
          onCancel={() => setEditing(null)}
          onSubmit={async (q) => {
            await updateQuestion.mutateAsync({ questionId: editing.questionId, body: q });
            setEditing(null);
          }}
        />
      )}

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
                <div className="flex flex-wrap items-center gap-2">
                  <span className="font-medium">{q.title}</span>
                  {q.required && <span className="text-red-500">*</span>}
                  <span className="badge bg-slate-200 text-slate-600">{TYPE_KO[q.type]}</span>
                </div>
                {q.options && q.options.length > 0 && (
                  <p className="mt-1 text-xs text-slate-500">보기: {q.options.join(", ")}</p>
                )}
                {q.branchRules && Object.keys(q.branchRules).length > 0 && (
                  <p className="mt-1 text-xs text-brand">분기 규칙 있음</p>
                )}
              </div>
              {isDraft && (
                <div className="flex flex-col gap-1">
                  <button type="button" className="btn-ghost px-2 text-xs" onClick={() => move(q.questionId, -1)}>
                    ↑
                  </button>
                  <button type="button" className="btn-ghost px-2 text-xs" onClick={() => move(q.questionId, 1)}>
                    ↓
                  </button>
                  <button type="button" className="btn-ghost px-2 text-xs" onClick={() => setEditing(q)}>
                    편집
                  </button>
                  <button
                    type="button"
                    className="btn-ghost px-2 text-xs text-red-600"
                    onClick={() => {
                      if (window.confirm("질문을 삭제할까요?")) deleteQuestion.mutate(q.questionId);
                    }}
                  >
                    삭제
                  </button>
                </div>
              )}
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
            <div className="space-y-2 rounded bg-red-50 p-2 text-sm text-red-600">
              <p>{publishError}</p>
              {(publishError.includes("부족") || publishError.includes("크레딧")) && (
                <p>
                  예치에 {estimate.escrow} 크레딧이 필요합니다.{" "}
                  <Link href="/me" className="font-semibold underline">
                    지갑에서 충전하기
                  </Link>
                </p>
              )}
            </div>
          )}
          <button
            className="btn-primary"
            onClick={doPublish}
            disabled={
              publish.isPending ||
              (questions ?? []).filter((q) => !CONTENT.has(q.type)).length === 0
            }
          >
            {publish.isPending ? "게시 중…" : "게시하고 예치하기"}
          </button>
        </div>
      ) : (
        <div className="card flex flex-wrap items-center justify-between gap-2">
          <span className="text-sm text-emerald-700">게시 완료! 피드·공유 링크로 응답을 모으세요.</span>
          <div className="flex gap-2">
            <Link href={`/forms/${formId}/results`} className="btn-ghost">
              결과
            </Link>
            <Link href="/home" className="btn-primary">
              대시보드
            </Link>
          </div>
        </div>
      )}
    </div>
  );
}
