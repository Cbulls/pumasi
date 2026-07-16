"use client";

import { useState } from "react";
import { uploadFormFile } from "@/lib/hooks";
import { useCurrentUser } from "@/context/CurrentUserContext";
import type { QuestionVO } from "@/lib/types";

interface Props {
  question: QuestionVO;
  value: string[];
  onChange: (values: string[]) => void;
  formId?: string;
}

export default function AnswerInput({ question, value, onChange, formId }: Props) {
  const q = question;
  const { token } = useCurrentUser();
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);

  switch (q.type) {
    case "DESCRIPTION":
      return (
        <div className="rounded-lg bg-slate-50 p-3 text-sm text-slate-600 whitespace-pre-wrap">
          {q.bodyHtml || q.title}
        </div>
      );

    case "IMAGE":
      return q.imageUrl ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={q.imageUrl} alt={q.title} className="max-h-64 rounded-lg object-contain" />
      ) : (
        <p className="text-sm text-slate-400">이미지 없음</p>
      );

    case "FILE":
      return (
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
            <label
              key={opt}
              className="flex items-center gap-2 rounded-lg border border-slate-200 p-2 text-sm hover:bg-slate-50"
            >
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
