"use client";

import { useCallback, useEffect, useId, useRef, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { GUIDE_CAUTIONS, GUIDE_RULE_SUMMARY } from "@/lib/guide";
import {
  hasSeenOnboarding,
  isOnboardingPath,
  markOnboardingSeen,
} from "@/lib/onboarding";

export default function OnboardingModal() {
  const pathname = usePathname();
  const titleId = useId();
  const descId = useId();
  const dialogRef = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (!isOnboardingPath(pathname)) {
      setOpen(false);
      return;
    }
    setOpen(!hasSeenOnboarding());
  }, [pathname]);

  const dismiss = useCallback(() => {
    markOnboardingSeen();
    setOpen(false);
  }, []);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") dismiss();
    };
    document.addEventListener("keydown", onKey);
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    dialogRef.current?.querySelector<HTMLElement>("button")?.focus();
    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = prev;
    };
  }, [open, dismiss]);

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-ink/60 p-4 backdrop-blur-sm sm:items-center"
      role="presentation"
      onClick={dismiss}
    >
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={descId}
        className="w-full max-w-lg rounded-2xl border border-slate-200 bg-white p-6 shadow-xl sm:p-8"
        onClick={(e) => e.stopPropagation()}
      >
        <p className="font-display text-xs font-semibold tracking-[0.2em] text-sky-600">
          처음 오셨나요?
        </p>
        <h2 id={titleId} className="font-display mt-2 text-2xl font-extrabold text-ink">
          품앗이 한눈에 보기
        </h2>
        <p id={descId} className="mt-3 text-sm leading-relaxed text-slate-600">
          {GUIDE_RULE_SUMMARY}
        </p>
        <ul className="mt-5 space-y-2 text-sm text-slate-600">
          {GUIDE_CAUTIONS.slice(0, 3).map((c) => (
            <li key={c} className="flex gap-2">
              <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-sky-500" aria-hidden />
              <span>{c}</span>
            </li>
          ))}
        </ul>
        <div className="mt-8 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <button type="button" className="btn-ghost" onClick={dismiss}>
            확인했어요
          </button>
          <Link
            href="/guide"
            className="btn-primary text-center"
            onClick={dismiss}
          >
            자세히 보기
          </Link>
        </div>
      </div>
    </div>
  );
}
