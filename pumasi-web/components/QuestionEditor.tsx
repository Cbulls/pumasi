"use client";

import { useEffect, useState } from "react";
import ImageUploader from "@/components/ImageUploader";
import type { QuestionType, QuestionVO, SectionVO } from "@/lib/types";

const TYPE_LABELS: Record<QuestionType, string> = {
  SHORT_TEXT: "단답형",
  LONG_TEXT: "장문형",
  RADIO: "객관식(단일선택)",
  CHECKBOX: "체크박스(다중선택)",
  DROPDOWN: "드롭다운",
  LINEAR_SCALE: "선형 배율",
  RATING: "별점(평점)",
  DATE: "날짜",
  TIME: "시간",
  MULTIPLE_CHOICE_GRID: "객관식 그리드(표)",
  CHECKBOX_GRID: "체크박스 그리드(표)",
  DESCRIPTION: "설명 문구",
  IMAGE: "이미지",
  FILE: "파일 업로드",
};

const isChoice = (t: QuestionType) =>
  t === "RADIO" || t === "CHECKBOX" || t === "DROPDOWN";
const isGrid = (t: QuestionType) =>
  t === "MULTIPLE_CHOICE_GRID" || t === "CHECKBOX_GRID";
const isScale = (t: QuestionType) => t === "LINEAR_SCALE" || t === "RATING";
const isContent = (t: QuestionType) => t === "DESCRIPTION" || t === "IMAGE";
const isText = (t: QuestionType) => t === "SHORT_TEXT" || t === "LONG_TEXT";
const canAttachImage = (t: QuestionType) => !isContent(t);

interface Props {
  disabled?: boolean;
  formId?: string;
  sections?: SectionVO[];
  initial?: QuestionVO | null;
  onSubmit: (q: Partial<QuestionVO>) => Promise<void>;
  onCancel?: () => void;
}

export default function QuestionEditor({
  disabled,
  formId,
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
  const [rowLabels, setRowLabels] = useState<string[]>(
    initial?.rowLabels?.length ? [...initial.rowLabels] : [""]
  );
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
  const [attentionAnswer, setAttentionAnswer] = useState(initial?.attentionAnswer ?? "");
  const [minLength, setMinLength] = useState<string>(
    initial?.minLength != null ? String(initial.minLength) : ""
  );
  const [maxLength, setMaxLength] = useState<string>(
    initial?.maxLength != null ? String(initial.maxLength) : ""
  );
  const [regex, setRegex] = useState(initial?.regex ?? "");
  const [minSelect, setMinSelect] = useState<string>(
    initial?.minSelect != null ? String(initial.minSelect) : ""
  );
  const [maxSelect, setMaxSelect] = useState<string>(
    initial?.maxSelect != null ? String(initial.maxSelect) : ""
  );
  const [allowOther, setAllowOther] = useState(!!initial?.allowOther);
  const [shuffleOptions, setShuffleOptions] = useState(!!initial?.shuffleOptions);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!initial) return;
    setType(initial.type);
    setTitle(initial.title);
    setRequired(initial.required);
    setOptions(initial.options?.length ? [...initial.options] : ["", ""]);
    setRowLabels(initial.rowLabels?.length ? [...initial.rowLabels] : [""]);
    setScaleMin(initial.scaleMin ?? 1);
    setScaleMax(initial.scaleMax ?? 5);
    setBodyHtml(initial.bodyHtml ?? "");
    setImageUrl(initial.imageUrl ?? "");
    setSectionId(initial.sectionId ?? sections?.[0]?.sectionId ?? "");
    setBranchRules(initial.branchRules ?? {});
    setAttentionAnswer(initial.attentionAnswer ?? "");
    setMinLength(initial.minLength != null ? String(initial.minLength) : "");
    setMaxLength(initial.maxLength != null ? String(initial.maxLength) : "");
    setRegex(initial.regex ?? "");
    setMinSelect(initial.minSelect != null ? String(initial.minSelect) : "");
    setMaxSelect(initial.maxSelect != null ? String(initial.maxSelect) : "");
    setAllowOther(!!initial.allowOther);
    setShuffleOptions(!!initial.shuffleOptions);
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
      allowOther: type === "RADIO" || type === "CHECKBOX" ? allowOther : false,
      shuffleOptions: isChoice(type) || isGrid(type) ? shuffleOptions : false,
      minLength: null,
      maxLength: null,
      regex: null,
      minSelect: null,
      maxSelect: null,
      rowLabels: null,
    };
    if (isChoice(type)) {
      const opts = options.map((o) => o.trim()).filter(Boolean);
      if (opts.length < 2) {
        setError("선택형은 보기가 2개 이상이어야 합니다.");
        return;
      }
      if (opts.some((o) => o.includes("="))) {
        setError("보기 라벨에 '='를 포함할 수 없습니다.");
        return;
      }
      payload.options = opts;
    }
    if (isGrid(type)) {
      const rows = rowLabels.map((r) => r.trim()).filter(Boolean);
      const cols = options.map((o) => o.trim()).filter(Boolean);
      if (rows.length < 1) {
        setError("그리드는 행이 1개 이상이어야 합니다.");
        return;
      }
      if (cols.length < 2) {
        setError("그리드는 열이 2개 이상이어야 합니다.");
        return;
      }
      if ([...rows, ...cols].some((l) => l.includes("="))) {
        setError("행/열 라벨에 '='를 포함할 수 없습니다.");
        return;
      }
      if (new Set(rows).size !== rows.length || new Set(cols).size !== cols.length) {
        setError("행/열 라벨이 중복됩니다.");
        return;
      }
      payload.rowLabels = rows;
      payload.options = cols;
    }
    if (type === "CHECKBOX" || type === "CHECKBOX_GRID") {
      if (minSelect !== "") payload.minSelect = Number(minSelect);
      if (maxSelect !== "") payload.maxSelect = Number(maxSelect);
    }
    if (isText(type)) {
      if (minLength !== "") payload.minLength = Number(minLength);
      if (maxLength !== "") payload.maxLength = Number(maxLength);
      payload.regex = regex.trim() || null;
    }
    if (isScale(type)) {
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
        setError("이미지를 업로드하세요.");
        return;
      }
      payload.imageUrl = imageUrl.trim();
    } else if (canAttachImage(type) && imageUrl.trim()) {
      payload.imageUrl = imageUrl.trim();
    } else if (canAttachImage(type)) {
      payload.imageUrl = null;
    }
    if (type === "RADIO" && Object.keys(branchRules).length > 0) {
      payload.branchRules = branchRules;
    } else {
      payload.branchRules = null;
    }
    payload.attentionAnswer =
      type === "RADIO" && attentionAnswer.trim() ? attentionAnswer.trim() : null;
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
        <ImageUploader
          formId={formId}
          value={imageUrl}
          onChange={setImageUrl}
          label="이미지"
          required
        />
      )}

      {canAttachImage(type) && (
        <ImageUploader
          formId={formId}
          value={imageUrl}
          onChange={setImageUrl}
          label="문항 이미지 (선택)"
        />
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
          <div className="flex flex-wrap gap-4 pt-1 text-sm">
            {(type === "RADIO" || type === "CHECKBOX") && (
              <label className="flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={allowOther}
                  onChange={(e) => setAllowOther(e.target.checked)}
                />
                「기타」 직접 입력 허용
              </label>
            )}
            <label className="flex items-center gap-2">
              <input
                type="checkbox"
                checked={shuffleOptions}
                onChange={(e) => setShuffleOptions(e.target.checked)}
              />
              응답 시 보기 순서 섞기
            </label>
          </div>
        </div>
      )}

      {isGrid(type) && (
        <div className="space-y-4">
          <div className="space-y-2">
            <label className="label">행 (질문 항목)</label>
            {rowLabels.map((row, i) => (
              <div key={i} className="flex gap-2">
                <input
                  className="input"
                  value={row}
                  onChange={(e) =>
                    setRowLabels((prev) => prev.map((r, j) => (j === i ? e.target.value : r)))
                  }
                  placeholder={`행 ${i + 1}`}
                />
                {rowLabels.length > 1 && (
                  <button
                    type="button"
                    className="btn-ghost px-2"
                    onClick={() => setRowLabels((prev) => prev.filter((_, j) => j !== i))}
                  >
                    ✕
                  </button>
                )}
              </div>
            ))}
            <button
              type="button"
              className="text-sm font-semibold text-brand"
              onClick={() => setRowLabels((prev) => [...prev, ""])}
            >
              + 행 추가
            </button>
          </div>
          <div className="space-y-2">
            <label className="label">열 (선택지)</label>
            {options.map((opt, i) => (
              <div key={i} className="flex gap-2">
                <input
                  className="input"
                  value={opt}
                  onChange={(e) =>
                    setOptions((prev) => prev.map((o, j) => (j === i ? e.target.value : o)))
                  }
                  placeholder={`열 ${i + 1}`}
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
              + 열 추가
            </button>
            <label className="flex items-center gap-2 pt-1 text-sm">
              <input
                type="checkbox"
                checked={shuffleOptions}
                onChange={(e) => setShuffleOptions(e.target.checked)}
              />
              응답 시 열 순서만 섞기 (행은 고정)
            </label>
          </div>
        </div>
      )}

      {(type === "CHECKBOX" || type === "CHECKBOX_GRID") && (
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="label">
              {type === "CHECKBOX_GRID" ? "행당 최소 선택" : "최소 선택 수"}
            </label>
            <input
              type="number"
              min={0}
              className="input"
              value={minSelect}
              onChange={(e) => setMinSelect(e.target.value)}
              placeholder="제한 없음"
            />
          </div>
          <div>
            <label className="label">
              {type === "CHECKBOX_GRID" ? "행당 최대 선택" : "최대 선택 수"}
            </label>
            <input
              type="number"
              min={0}
              className="input"
              value={maxSelect}
              onChange={(e) => setMaxSelect(e.target.value)}
              placeholder="제한 없음"
            />
          </div>
        </div>
      )}

      {isText(type) && (
        <div className="space-y-2 rounded-lg bg-slate-50 p-3">
          <p className="text-sm font-semibold">입력 검증 (선택)</p>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="label">최소 글자</label>
              <input
                type="number"
                min={0}
                className="input"
                value={minLength}
                onChange={(e) => setMinLength(e.target.value)}
              />
            </div>
            <div>
              <label className="label">최대 글자</label>
              <input
                type="number"
                min={1}
                className="input"
                value={maxLength}
                onChange={(e) => setMaxLength(e.target.value)}
              />
            </div>
          </div>
          <div>
            <label className="label">정규식 (예: 이메일)</label>
            <input
              className="input font-mono text-xs"
              value={regex}
              onChange={(e) => setRegex(e.target.value)}
              placeholder="^[^@\s]+@[^@\s]+$"
            />
          </div>
        </div>
      )}

      {isScale(type) && (
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

      {type === "RADIO" && (
        <div className="space-y-1 rounded-lg bg-amber-50 p-3">
          <label className="text-sm font-semibold text-amber-800">
            주의 문항 (선택 — 어뷰징 필터)
          </label>
          <p className="text-xs text-amber-700">
            정답을 지정하면, 응답자가 다른 보기를 고를 때 해당 응답이 자동 reject됩니다.
          </p>
          <select
            className="input"
            value={attentionAnswer}
            onChange={(e) => setAttentionAnswer(e.target.value)}
          >
            <option value="">사용 안 함</option>
            {options
              .map((o) => o.trim())
              .filter(Boolean)
              .map((opt) => (
                <option key={opt} value={opt}>
                  정답: {opt}
                </option>
              ))}
          </select>
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
