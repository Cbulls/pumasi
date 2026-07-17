"use client";

import { useEffect, useState } from "react";
import Link from "next/link";

const KEY = "pumasi.unlock-tutorial.v1";

/** 결과 화면 첫 방문 시 상호 언락 규칙 안내 */
export default function UnlockTutorial({ unlockCount }: { unlockCount?: number }) {
  const [open, setOpen] = useState(false);

  useEffect(() => {
    try {
      if (window.localStorage.getItem(KEY) !== "1") setOpen(true);
    } catch {
      setOpen(true);
    }
  }, []);

  if (!open) return null;

  const dismiss = () => {
    try {
      window.localStorage.setItem(KEY, "1");
    } catch {
      /* ignore */
    }
    setOpen(false);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-ink/40 p-4 sm:items-center">
      <div className="w-full max-w-md rounded-2xl bg-white p-5 shadow-xl">
        <h2 className="font-display text-lg font-extrabold">결과가 잠겨 있나요?</h2>
        <p className="mt-2 text-sm text-slate-600">
          품앗이폼은 <strong>상대 설문에 내가 응답해야</strong> 그 사람의 응답이 결과에서 열립니다.
          크레딧으로 결과를 사는 구조가 아닙니다.
        </p>
        <ol className="mt-3 list-decimal space-y-1 pl-5 text-sm text-slate-600">
          <li>상대가 내 설문에 답합니다.</li>
          <li>내가 상대의 ACTIVE 설문에 답합니다.</li>
          <li>그때 해당 행·차트가 열립니다.</li>
        </ol>
        {typeof unlockCount === "number" && unlockCount > 0 && (
          <p className="mt-3 rounded-lg bg-brand-light px-3 py-2 text-sm text-brand-dark">
            지금 응답하면 열 수 있는 설문이 <strong>{unlockCount}개</strong> 있습니다.
          </p>
        )}
        <div className="mt-4 flex flex-col gap-2 sm:flex-row">
          <Link href="/feed" className="btn-primary flex-1 text-center" onClick={dismiss}>
            언락할 설문 보러가기
          </Link>
          <button type="button" className="btn-ghost flex-1" onClick={dismiss}>
            이해했어요
          </button>
        </div>
      </div>
    </div>
  );
}
