"use client";

import { useState } from "react";
import type { QuestionType, QuestionVO } from "@/lib/types";

const TYPE_LABELS: Record<QuestionType, string> = {
  SHORT_TEXT: "단답형",
  LONG_TEXT: "장문형",
  RADIO: "객관식(단일선택)",
  CHECKBOX: "체크박스(다중선택)",
  LINEAR_SCALE: "선형 배율",
};

const isChoice = (t: QuestionType) => t === "RADIO" || t === "CHECKBOX";
const isText = (t: QuestionType) => t === "SHORT_TEXT" || t === "LONG_TEXT";

interface Props {
  disabled?: boolean;
  onAdd: (q: Partial<QuestionVO>) => Promise<void>;
}

/** 질문 추가 폼. 유형별로 필요한 필드만 노출하고, 그 필드만 payload에 담는다. */
export default function QuestionEditor({ disabled, onAdd }: Props) {
  const [type, setType] = useState<QuestionType>("RADIO");
  const [title, setTitle] = useState("");
  const [required, setRequired] = useState(true);
  const [options, setOptions] = useState<string[]>(["", ""]);
  const [scaleMin, setScaleMin] = useState(1);
  const [scaleMax, setScaleMax] = useState(5);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const reset = () => {
    setTitle("");
    setRequired(true);
    setOptions(["", ""]);
    setScaleMin(1);
    setScaleMax(5);
  };

  const submit = async () => {
    setError(null);
    if (!title.trim()) {
      setError("질문 제목을 입력하세요.");
      return;
    }
    const payload: Partial<QuestionVO> = { type, title: title.trim(), required };
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
    try {
      setBusy(true);
      await onAdd(payload);
      reset();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="card space-y-3">
      <h3 className="font-bold">질문 추가</h3>

      <div className="grid gap-3 sm:grid-cols-2">
        <div>
          <label className="label">유형</label>
          <select
            className="input"
            value={type}
            onChange={(e) => setType(e.target.value as QuestionType)}
          >
            {(Object.keys(TYPE_LABELS) as QuestionType[]).map((t) => (
              <option key={t} value={t}>
                {TYPE_LABELS[t]}
              </option>
            ))}
          </select>
        </div>
        <label className="flex items-end gap-2 pb-2 text-sm">
          <input
            type="checkbox"
            checked={required}
            onChange={(e) => setRequired(e.target.checked)}
          />
          필수 응답
        </label>
      </div>

      <div>
        <label className="label">질문 제목</label>
        <input
          className="input"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="예: 가장 좋아하는 색은?"
        />
      </div>

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

      {isText(type) && (
        <p className="text-xs text-slate-400">텍스트형은 별도 옵션 없이 자유 입력을 받습니다.</p>
      )}

      {error && <p className="rounded bg-red-50 p-2 text-sm text-red-600">{error}</p>}

      <button className="btn-primary" onClick={submit} disabled={disabled || busy}>
        {busy ? "추가 중…" : "질문 추가"}
      </button>
    </div>
  );
}
