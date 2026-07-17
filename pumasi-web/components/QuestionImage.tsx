"use client";

import { useCallback, useState } from "react";
import { mediaVariantUrl } from "@/lib/media";

interface Props {
  imageUrl: string;
  alt?: string;
  className?: string;
  /** 기본 display. 편집기 썸네일은 thumb */
  prefer?: "thumb" | "display";
  enableLightbox?: boolean;
}

export default function QuestionImage({
  imageUrl,
  alt = "",
  className = "",
  prefer = "display",
  enableLightbox = true,
}: Props) {
  const [open, setOpen] = useState(false);
  const thumb = mediaVariantUrl(imageUrl, "thumb");
  const display = mediaVariantUrl(imageUrl, "display");
  const orig = mediaVariantUrl(imageUrl, "orig");
  const primary = prefer === "thumb" ? thumb : display;

  const onKey = useCallback((e: React.KeyboardEvent) => {
    if (e.key === "Escape") setOpen(false);
  }, []);

  if (!primary) return null;

  const isLegacyExternal = imageUrl.startsWith("http");

  return (
    <>
      <button
        type="button"
        className={`block w-full overflow-hidden rounded-xl bg-slate-100 text-left ${className}`}
        onClick={() => enableLightbox && setOpen(true)}
        disabled={!enableLightbox}
      >
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          src={primary}
          srcSet={
            isLegacyExternal
              ? undefined
              : `${thumb} 480w, ${display} 1280w`
          }
          sizes="(max-width: 640px) 100vw, 560px"
          alt={alt}
          loading="lazy"
          decoding="async"
          className="max-h-72 w-full object-contain"
        />
      </button>

      {open && orig && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-ink/80 p-4 backdrop-blur-sm"
          role="dialog"
          aria-modal="true"
          aria-label="이미지 확대"
          onClick={() => setOpen(false)}
          onKeyDown={onKey}
        >
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src={orig}
            alt={alt}
            className="max-h-[90vh] max-w-full object-contain"
            onClick={(e) => e.stopPropagation()}
          />
        </div>
      )}
    </>
  );
}
