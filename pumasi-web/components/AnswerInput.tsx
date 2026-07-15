"use client";

import type { QuestionVO } from "@/lib/types";

interface Props {
  question: QuestionVO;
  value: string[];
  onChange: (values: string[]) => void;
}

/** 질문 유형별 입력 위젯. 값은 항상 string[]로 정규화(백엔드 AnswerVO.values). */
export default function AnswerInput({ question, value, onChange }: Props) {
  const q = question;

  switch (q.type) {
    case "SHORT_TEXT":
      return (
        <input
          className="input"
          value={value[0] ?? ""}
          onChange={(e) => onChange([e.target.value])}
          placeholder="답변 입력"
        />
      );

    case "LONG_TEXT":
      return (
        <textarea
          className="input"
          rows={3}
          value={value[0] ?? ""}
          onChange={(e) => onChange([e.target.value])}
          placeholder="답변 입력"
        />
      );

    case "RADIO":
      return (
        <div className="space-y-2">
          {(q.options ?? []).map((opt) => (
            <label key={opt} className="flex items-center gap-2 rounded-lg border border-slate-200 p-2 text-sm hover:bg-slate-50">
              <input
                type="radio"
                name={q.questionId}
                checked={value[0] === opt}
                onChange={() => onChange([opt])}
              />
              {opt}
            </label>
          ))}
        </div>
      );

    case "CHECKBOX":
      return (
        <div className="space-y-2">
          {(q.options ?? []).map((opt) => {
            const checked = value.includes(opt);
            return (
              <label key={opt} className="flex items-center gap-2 rounded-lg border border-slate-200 p-2 text-sm hover:bg-slate-50">
                <input
                  type="checkbox"
                  checked={checked}
                  onChange={() =>
                    onChange(checked ? value.filter((v) => v !== opt) : [...value, opt])
                  }
                />
                {opt}
              </label>
            );
          })}
        </div>
      );

    case "LINEAR_SCALE": {
      const min = q.scaleMin ?? 1;
      const max = q.scaleMax ?? 5;
      const nums = Array.from({ length: max - min + 1 }, (_, i) => min + i);
      return (
        <div className="flex flex-wrap gap-2">
          {nums.map((n) => {
            const s = String(n);
            const active = value[0] === s;
            return (
              <button
                key={n}
                type="button"
                onClick={() => onChange([s])}
                className={`h-10 w-10 rounded-full border text-sm font-semibold transition ${
                  active
                    ? "border-brand bg-brand text-white"
                    : "border-slate-300 bg-white text-slate-600 hover:border-brand"
                }`}
              >
                {n}
              </button>
            );
          })}
        </div>
      );
    }

    default:
      return <p className="text-sm text-slate-400">지원하지 않는 유형</p>;
  }
}
