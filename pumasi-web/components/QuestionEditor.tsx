"use client";

import { useEffect, useState } from "react";
import type { QuestionType, QuestionVO, SectionVO } from "@/lib/types";

const TYPE_LABELS: Record<QuestionType, string> = {
  SHORT_TEXT: "단답형",
  LONG_TEXT: "장문형",
  RADIO: "객관식(단일선택)",
  CHECKBOX: "체크박스(다중선택)",
  LINEAR_SCALE: "선형 배율",
  DESCRIPTION: "설명 문구",
  IMAGE: "이미지",
  FILE: "파일 업로드",
};

const isChoice = (t: QuestionType) => t === "RADIO" || t === "CHECKBOX";
const isContent = (t: QuestionType) => t === "DESCRIPTION" || t === "IMAGE";

interface Props {
  disabled?: boolean;
  sections?: SectionVO[];
  initial?: QuestionVO | null;
  onSubmit: (q: Partial<QuestionVO>) => Promise<void>;
  onCancel?: () => void;
}

export default function QuestionEditor({
  disabled,
  sections,
  initial,
  onSubmit,
  onCancel,
}: Props) {
  const editing = !!initial;
  const [type, setType] = useState<QuestionType>(initial?.type ?? "RADIO");
  const [title, setTitle] = useState(initial?.title ?? "");
  const [required, setRequired] = useState(initial?.required ?? true);
  const [options, setOptions] = useState<string[]>(initial?.options ?? ["", ""]);
  const [scaleMin, setScaleMin] = useState(initial?.scaleMin ?? 1);
  const [scaleMax, setScaleMax] = useState(initial?.scaleMax ?? 5);
  const [bodyHtml, setBodyHtml] = useState(initial?.bodyHtml ?? "");
  const [imageUrl, setImageUrl] = useState(initial?.imageUrl ?? "");
  const [sectionId, setSectionId] = useState(
    initial?.sectionId ?? sections?.[0]?.sectionId ?? ""
  );
  const [branchRules, setBranchRules] = useState<Record<string, string>>(
    initial?.branchRules ?? {}
  );
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!initial) return;
    setType(initial.type);
    setTitle(initial.title);
    setRequired(initial.required);
    setOptions(initial.options?.length ? [...initial.options] : ["", ""]);
    setScaleMin(initial.scaleMin ?? 1);
    setScaleMax(initial.scaleMax ?? 5);
    setBodyHtml(initial.bodyHtml ?? "");
    setImageUrl(initial.imageUrl ?? "");
    setSectionId(initial.sectionId ?? sections?.[0]?.sectionId ?? "");
    setBranchRules(initial.branchRules ?? {});
  }, [initial, sections]);

  const submit = async () => {
    setError(null);
    if (!title.trim()) {
      setError("질문 제목을 입력하세요.");
      return;
    }
    const payload: Partial<QuestionVO> = {
      type,
      title: title.trim(),
      required: isContent(type) ? false : required,
      sectionId: sectionId || undefined,
    };
    if (isChoice(type)) {
      const opts = options.map((o) => o.trim()).filter(Boolean);
      if (opts.length < 2) {
        setError("선택형은 보기가 2개 이상이어야 합니다.");
        return;
      }
      payload.options = opts;
    }
    if (type === "LINEAR_SCALE") {
      if (scaleMin >= scaleMax) {
        setError("scaleMin은 scaleMax보다 작아야 합니다.");
        return;
      }
      payload.scaleMin = scaleMin;
      payload.scaleMax = scaleMax;
    }
    if (type === "DESCRIPTION") payload.bodyHtml = bodyHtml;
    if (type === "IMAGE") {
      if (!imageUrl.trim()) {
        setError("이미지 URL을 입력하세요.");
        return;
      }
      payload.imageUrl = imageUrl.trim();
    }
    if (type === "RADIO" && Object.keys(branchRules).length > 0) {
      payload.branchRules = branchRules;
    } else {
      payload.branchRules = null;
    }
    try {
      setBusy(true);
      await onSubmit(payload);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  };

  const laterSections = (sections ?? []).filter((s) => s.sectionId !== sectionId);

  return (
    <div className="card space-y-3">
      <h3 className="font-bold">{editing ? "질문 수정" : "질문 추가"}</h3>

      <div className="grid gap-3 sm:grid-cols-2">
        <div>
          <label className="label">유형</label>
          <select
            className="input"
            value={type}
            disabled={editing}
            onChange={(e) => setType(e.target.value as QuestionType)}
          >
            {(Object.keys(TYPE_LABELS) as QuestionType[]).map((t) => (
              <option key={t} value={t}>
                {TYPE_LABELS[t]}
              </option>
            ))}
          </select>
        </div>
        {!isContent(type) && type !== "FILE" && (
          <label className="flex items-end gap-2 pb-2 text-sm">
            <input
              type="checkbox"
              checked={required}
              onChange={(e) => setRequired(e.target.checked)}
            />
            필수 응답
          </label>
        )}
        {type === "FILE" && (
          <label className="flex items-end gap-2 pb-2 text-sm">
            <input
              type="checkbox"
              checked={required}
              onChange={(e) => setRequired(e.target.checked)}
            />
            필수 첨부
          </label>
        )}
      </div>

      {sections && sections.length > 0 && (
        <div>
          <label className="label">섹션</label>
          <select
            className="input"
            value={sectionId}
            onChange={(e) => setSectionId(e.target.value)}
          >
            {sections.map((s) => (
              <option key={s.sectionId} value={s.sectionId}>
                {s.title}
              </option>
            ))}
          </select>
        </div>
      )}

      <div>
        <label className="label">{isContent(type) ? "제목" : "질문 제목"}</label>
        <input
          className="input"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="예: 가장 좋아하는 색은?"
        />
      </div>

      {type === "DESCRIPTION" && (
        <div>
          <label className="label">설명 본문</label>
          <textarea
            className="input"
            rows={3}
            value={bodyHtml}
            onChange={(e) => setBodyHtml(e.target.value)}
            placeholder="안내 문구를 입력하세요"
          />
        </div>
      )}

      {type === "IMAGE" && (
        <div>
          <label className="label">이미지 URL</label>
          <input
            className="input"
            value={imageUrl}
            onChange={(e) => setImageUrl(e.target.value)}
            placeholder="https://..."
          />
        </div>
      )}

      {isChoice(type) && (
        <div className="space-y-2">
          <label className="label">보기</label>
          {options.map((opt, i) => (
            <div key={i} className="flex gap-2">
              <input
                className="input"
                value={opt}
                onChange={(e) =>
                  setOptions((prev) => prev.map((o, j) => (j === i ? e.target.value : o)))
                }
                placeholder={`보기 ${i + 1}`}
              />
              {options.length > 2 && (
                <button
                  type="button"
                  className="btn-ghost px-2"
                  onClick={() => setOptions((prev) => prev.filter((_, j) => j !== i))}
                >
                  ✕
                </button>
              )}
            </div>
          ))}
          <button
            type="button"
            className="text-sm font-semibold text-brand"
            onClick={() => setOptions((prev) => [...prev, ""])}
          >
            + 보기 추가
          </button>
        </div>
      )}

      {type === "LINEAR_SCALE" && (
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="label">최소값</label>
            <input
              type="number"
              className="input"
              value={scaleMin}
              onChange={(e) => setScaleMin(Number(e.target.value))}
            />
          </div>
          <div>
            <label className="label">최대값</label>
            <input
              type="number"
              className="input"
              value={scaleMax}
              onChange={(e) => setScaleMax(Number(e.target.value))}
            />
          </div>
        </div>
      )}

      {type === "RADIO" && laterSections.length > 0 && (
        <div className="space-y-2 rounded-lg bg-slate-50 p-3">
          <p className="text-sm font-semibold">조건부 분기 (선택 → 뒤 섹션)</p>
          {options
            .map((o) => o.trim())
            .filter(Boolean)
            .map((opt) => (
              <div key={opt} className="flex items-center gap-2 text-sm">
                <span className="w-24 truncate text-slate-600">{opt}</span>
                <select
                  className="input"
                  value={branchRules[opt] ?? ""}
                  onChange={(e) => {
                    const v = e.target.value;
                    setBranchRules((prev) => {
                      const next = { ...prev };
                      if (!v) delete next[opt];
                      else next[opt] = v;
                      return next;
                    });
                  }}
                >
                  <option value="">다음 섹션(기본)</option>
                  {laterSections.map((s) => (
                    <option key={s.sectionId} value={s.sectionId}>
                      {s.title}
                    </option>
                  ))}
                </select>
              </div>
            ))}
        </div>
      )}

      {error && <p className="rounded bg-red-50 p-2 text-sm text-red-600">{error}</p>}

      <div className="flex gap-2">
        <button className="btn-primary" onClick={submit} disabled={disabled || busy}>
          {busy ? "저장 중…" : editing ? "수정 저장" : "질문 추가"}
        </button>
        {onCancel && (
          <button type="button" className="btn-ghost" onClick={onCancel}>
            취소
          </button>
        )}
      </div>
    </div>
  );
}
