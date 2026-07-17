"use client";

import { useRef, useState } from "react";
import { uploadFormMedia } from "@/lib/hooks";
import { useCurrentUser } from "@/context/CurrentUserContext";
import QuestionImage from "@/components/QuestionImage";

interface Props {
  formId?: string;
  value: string;
  onChange: (url: string) => void;
  label?: string;
  required?: boolean;
}

const MAX_BYTES = 8 * 1024 * 1024;

export default function ImageUploader({
  formId,
  value,
  onChange,
  label = "이미지",
  required,
}: Props) {
  const { token } = useCurrentUser();
  const inputRef = useRef<HTMLInputElement>(null);
  const [dragging, setDragging] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleFile = async (file: File | undefined) => {
    if (!file) return;
    setError(null);
    if (!formId) {
      setError("폼을 먼저 저장한 뒤 이미지를 올릴 수 있습니다.");
      return;
    }
    if (!file.type.startsWith("image/")) {
      setError("이미지 파일만 업로드할 수 있습니다.");
      return;
    }
    if (file.size > MAX_BYTES) {
      setError("이미지는 8MB 이하여야 합니다.");
      return;
    }
    setBusy(true);
    try {
      const res = await uploadFormMedia(formId, token, file);
      onChange(res.url);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="space-y-2">
      <label className="label">
        {label}
        {required ? <span className="text-red-500"> *</span> : null}
      </label>

      {value ? (
        <div className="space-y-2">
          <QuestionImage imageUrl={value} prefer="thumb" enableLightbox alt={label} />
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              className="btn-ghost text-sm"
              disabled={busy || !formId}
              onClick={() => inputRef.current?.click()}
            >
              {busy ? "올리는 중…" : "교체"}
            </button>
            <button
              type="button"
              className="btn-ghost text-sm text-red-600"
              disabled={busy}
              onClick={() => onChange("")}
            >
              삭제
            </button>
          </div>
        </div>
      ) : (
        <div
          role="button"
          tabIndex={0}
          onKeyDown={(e) => {
            if (e.key === "Enter" || e.key === " ") inputRef.current?.click();
          }}
          onClick={() => !busy && inputRef.current?.click()}
          onDragEnter={(e) => {
            e.preventDefault();
            setDragging(true);
          }}
          onDragOver={(e) => e.preventDefault()}
          onDragLeave={() => setDragging(false)}
          onDrop={(e) => {
            e.preventDefault();
            setDragging(false);
            void handleFile(e.dataTransfer.files?.[0]);
          }}
          className={`flex cursor-pointer flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed px-4 py-10 text-center transition ${
            dragging
              ? "border-sky-400 bg-sky-50"
              : "border-slate-300 bg-slate-50 hover:border-brand/40"
          } ${busy ? "pointer-events-none opacity-60" : ""}`}
        >
          <p className="text-sm font-semibold text-slate-700">
            {busy ? "최적화·업로드 중…" : "클릭하거나 이미지를 끌어다 놓으세요"}
          </p>
          <p className="text-xs text-slate-500">JPEG·PNG·WebP·GIF · 최대 8MB · 자동 리사이즈</p>
        </div>
      )}

      <input
        ref={inputRef}
        type="file"
        accept="image/jpeg,image/png,image/webp,image/gif"
        className="hidden"
        onChange={(e) => {
          void handleFile(e.target.files?.[0]);
          e.target.value = "";
        }}
      />
      {error && <p className="text-xs text-red-600">{error}</p>}
    </div>
  );
}
