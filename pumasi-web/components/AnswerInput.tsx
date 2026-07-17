"use client";

import { useMemo, useState, type ReactNode } from "react";
import { uploadFormFile } from "@/lib/hooks";
import { useCurrentUser } from "@/context/CurrentUserContext";
import QuestionImage from "@/components/QuestionImage";
import { respondShuffleSeed, seededShuffle } from "@/lib/shuffle";
import type { QuestionVO } from "@/lib/types";

const OTHER_PREFIX = "기타:";

interface Props {
  question: QuestionVO;
  value: string[];
  onChange: (values: string[]) => void;
  formId?: string;
}

function useDisplayOptions(q: QuestionVO): string[] {
  return useMemo(() => {
    const opts = q.options ?? [];
    if (!q.shuffleOptions || opts.length === 0) return opts;
    return seededShuffle(opts, respondShuffleSeed(q.questionId));
  }, [q.options, q.shuffleOptions, q.questionId]);
}

function otherValue(values: string[]): string | undefined {
  return values.find((v) => v.startsWith(OTHER_PREFIX));
}

export default function AnswerInput({ question, value, onChange, formId }: Props) {
  const q = question;
  const { token } = useCurrentUser();
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const displayOpts = useDisplayOptions(q);
  const otherSelected = !!otherValue(value);
  const otherText = otherValue(value)?.slice(OTHER_PREFIX.length) ?? "";

  const wrap = (body: ReactNode) => (
    <div className="space-y-3">
      {q.type !== "IMAGE" && q.imageUrl ? (
        <QuestionImage imageUrl={q.imageUrl} alt={q.title} />
      ) : null}
      {body}
    </div>
  );

  const setOtherText = (text: string) => {
    const cleaned = value.filter((v) => !v.startsWith(OTHER_PREFIX));
    onChange([...cleaned, `${OTHER_PREFIX}${text}`]);
  };

  switch (q.type) {
    case "DESCRIPTION":
      return wrap(
        <div className="rounded-lg bg-slate-50 p-3 text-sm text-slate-600 whitespace-pre-wrap">
          {q.bodyHtml || q.title}
        </div>
      );

    case "IMAGE":
      return q.imageUrl ? (
        <QuestionImage imageUrl={q.imageUrl} alt={q.title} />
      ) : (
        <p className="text-sm text-slate-400">이미지 없음</p>
      );

    case "FILE":
      return wrap(
        <div className="space-y-2">
          <input
            type="file"
            accept="image/*,.pdf,text/plain"
            disabled={!formId || uploading}
            onChange={async (e) => {
              const file = e.target.files?.[0];
              if (!file || !formId) return;
              setUploadError(null);
              setUploading(true);
              try {
                const res = await uploadFormFile(formId, token, file);
                onChange([res.fileId]);
              } catch (err) {
                setUploadError((err as Error).message);
              } finally {
                setUploading(false);
              }
            }}
          />
          {uploading && <p className="text-xs text-slate-500">업로드 중…</p>}
          {value[0] && <p className="text-xs text-emerald-700">첨부됨: {value[0]}</p>}
          {uploadError && <p className="text-xs text-red-600">{uploadError}</p>}
        </div>
      );

    case "SHORT_TEXT":
      return wrap(
        <div>
          <input
            className="input"
            value={value[0] ?? ""}
            maxLength={q.maxLength ?? undefined}
            onChange={(e) => onChange([e.target.value])}
            placeholder="답변 입력"
          />
          {(q.minLength != null || q.maxLength != null) && (
            <p className="mt-1 text-xs text-slate-400">
              {q.minLength != null ? `최소 ${q.minLength}자` : ""}
              {q.minLength != null && q.maxLength != null ? " · " : ""}
              {q.maxLength != null ? `최대 ${q.maxLength}자` : ""}
              {value[0] ? ` · 현재 ${value[0].length}자` : ""}
            </p>
          )}
        </div>
      );

    case "LONG_TEXT":
      return wrap(
        <div>
          <textarea
            className="input"
            rows={3}
            value={value[0] ?? ""}
            maxLength={q.maxLength ?? undefined}
            onChange={(e) => onChange([e.target.value])}
            placeholder="답변 입력"
          />
          {(q.minLength != null || q.maxLength != null) && (
            <p className="mt-1 text-xs text-slate-400">
              {q.minLength != null ? `최소 ${q.minLength}자` : ""}
              {q.minLength != null && q.maxLength != null ? " · " : ""}
              {q.maxLength != null ? `최대 ${q.maxLength}자` : ""}
            </p>
          )}
        </div>
      );

    case "RADIO":
      return wrap(
        <div className="space-y-2">
          {displayOpts.map((opt) => (
            <label
              key={opt}
              className="flex items-center gap-2 rounded-lg border border-slate-200 p-2 text-sm hover:bg-slate-50"
            >
              <input
                type="radio"
                name={q.questionId}
                checked={!otherSelected && value[0] === opt}
                onChange={() => onChange([opt])}
              />
              {opt}
            </label>
          ))}
          {q.allowOther && (
            <label className="flex flex-col gap-1 rounded-lg border border-slate-200 p-2 text-sm">
              <span className="flex items-center gap-2">
                <input
                  type="radio"
                  name={q.questionId}
                  checked={otherSelected}
                  onChange={() => onChange([`${OTHER_PREFIX}`])}
                />
                기타
              </span>
              {otherSelected && (
                <input
                  className="input"
                  value={otherText}
                  onChange={(e) => setOtherText(e.target.value)}
                  placeholder="직접 입력"
                />
              )}
            </label>
          )}
        </div>
      );

    case "CHECKBOX":
      return wrap(
        <div className="space-y-2">
          {(q.minSelect != null || q.maxSelect != null) && (
            <p className="text-xs text-slate-400">
              {q.minSelect != null ? `최소 ${q.minSelect}개` : ""}
              {q.minSelect != null && q.maxSelect != null ? " · " : ""}
              {q.maxSelect != null ? `최대 ${q.maxSelect}개` : ""}
            </p>
          )}
          {displayOpts.map((opt) => {
            const checked = value.includes(opt);
            return (
              <label
                key={opt}
                className="flex items-center gap-2 rounded-lg border border-slate-200 p-2 text-sm hover:bg-slate-50"
              >
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
          {q.allowOther && (
            <label className="flex flex-col gap-1 rounded-lg border border-slate-200 p-2 text-sm">
              <span className="flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={otherSelected}
                  onChange={() => {
                    if (otherSelected) {
                      onChange(value.filter((v) => !v.startsWith(OTHER_PREFIX)));
                    } else {
                      onChange([...value.filter((v) => !v.startsWith(OTHER_PREFIX)), `${OTHER_PREFIX}`]);
                    }
                  }}
                />
                기타
              </span>
              {otherSelected && (
                <input
                  className="input"
                  value={otherText}
                  onChange={(e) => setOtherText(e.target.value)}
                  placeholder="직접 입력"
                />
              )}
            </label>
          )}
        </div>
      );

    case "DROPDOWN":
      return wrap(
        <select
          className="input"
          value={value[0] ?? ""}
          onChange={(e) => onChange(e.target.value ? [e.target.value] : [])}
        >
          <option value="">선택하세요</option>
          {displayOpts.map((opt) => (
            <option key={opt} value={opt}>
              {opt}
            </option>
          ))}
        </select>
      );

    case "DATE":
      return wrap(
        <input
          type="date"
          className="input"
          value={value[0] ?? ""}
          onChange={(e) => onChange(e.target.value ? [e.target.value] : [])}
        />
      );

    case "TIME":
      return wrap(
        <input
          type="time"
          className="input"
          value={value[0] ?? ""}
          onChange={(e) => onChange(e.target.value ? [e.target.value] : [])}
        />
      );

    case "RATING": {
      const min = q.scaleMin ?? 1;
      const max = q.scaleMax ?? 5;
      const nums = Array.from({ length: max - min + 1 }, (_, i) => min + i);
      const selected = value[0] ? Number(value[0]) : null;
      return wrap(
        <div className="flex flex-wrap items-center gap-1">
          {nums.map((n) => {
            const filled = selected !== null && n <= selected;
            return (
              <button
                key={n}
                type="button"
                aria-label={`${n}점`}
                onClick={() => onChange([String(n)])}
                className={`text-2xl transition ${
                  filled ? "text-amber-400" : "text-slate-300 hover:text-amber-300"
                }`}
              >
                ★
              </button>
            );
          })}
          {selected !== null && (
            <span className="ml-2 text-sm text-slate-500">
              {selected} / {max}
            </span>
          )}
        </div>
      );
    }

    case "LINEAR_SCALE": {
      const min = q.scaleMin ?? 1;
      const max = q.scaleMax ?? 5;
      const nums = Array.from({ length: max - min + 1 }, (_, i) => min + i);
      return wrap(
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

    case "MULTIPLE_CHOICE_GRID":
    case "CHECKBOX_GRID": {
      const rows = q.rowLabels ?? [];
      const cols = displayOpts;
      const multi = q.type === "CHECKBOX_GRID";
      const cellKey = (row: string, col: string) => `${row}=${col}`;
      const setRadio = (row: string, col: string) => {
        const rest = value.filter((v) => !v.startsWith(`${row}=`));
        onChange([...rest, cellKey(row, col)]);
      };
      const toggleCheck = (row: string, col: string) => {
        const key = cellKey(row, col);
        if (value.includes(key)) {
          onChange(value.filter((v) => v !== key));
          return;
        }
        const max = q.maxSelect;
        if (max != null) {
          const rowCount = value.filter((v) => v.startsWith(`${row}=`)).length;
          if (rowCount >= max) {
            // 행당 max 초과 시 가장 오래된 해당 행 선택 제거 후 추가하지 않고 안내만 — 단순 거부
            return;
          }
        }
        onChange([...value, key]);
      };
      return wrap(
        <div className="overflow-x-auto">
          {(multi && (q.minSelect != null || q.maxSelect != null)) && (
            <p className="mb-2 text-xs text-slate-400">
              {q.minSelect != null ? `행당 최소 ${q.minSelect}개` : ""}
              {q.minSelect != null && q.maxSelect != null ? " · " : ""}
              {q.maxSelect != null ? `행당 최대 ${q.maxSelect}개` : ""}
            </p>
          )}
          <table className="w-full min-w-[280px] border-collapse text-sm">
            <thead>
              <tr>
                <th className="border-b border-slate-200 p-2 text-left font-medium text-slate-500" />
                {cols.map((col) => (
                  <th
                    key={col}
                    className="border-b border-slate-200 p-2 text-center font-medium text-slate-600"
                  >
                    {col}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row} className="border-b border-slate-100">
                  <th className="p-2 text-left font-medium text-slate-700">{row}</th>
                  {cols.map((col) => {
                    const key = cellKey(row, col);
                    const checked = value.includes(key);
                    return (
                      <td key={col} className="p-2 text-center">
                        {multi ? (
                          <input
                            type="checkbox"
                            checked={checked}
                            onChange={() => toggleCheck(row, col)}
                            aria-label={`${row} ${col}`}
                          />
                        ) : (
                          <input
                            type="radio"
                            name={`${q.questionId}-${row}`}
                            checked={checked}
                            onChange={() => setRadio(row, col)}
                            aria-label={`${row} ${col}`}
                          />
                        )}
                      </td>
                    );
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      );
    }

    default:
      return <p className="text-sm text-slate-400">지원하지 않는 유형</p>;
  }
}
