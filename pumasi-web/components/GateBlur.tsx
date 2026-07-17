"use client";

import Link from "next/link";

/**
 * 비소유자 결과 접근(403) 시각화.
 * 소유자 결과의 행 단위 잠금은 ResponsesTable에서 상호 응답 언락으로 처리한다.
 */
export default function GateBlur({ message }: { message: string }) {
  return (
    <div className="relative overflow-hidden rounded-2xl border border-slate-200">
      {/* 블러 처리된 가짜 차트 미리보기 */}
      <div className="grid gap-4 p-5 blur-sm sm:grid-cols-2" aria-hidden>
        {[0, 1, 2, 3].map((i) => (
          <div key={i} className="card">
            <div className="mb-3 h-4 w-1/2 rounded bg-slate-200" />
            <div className="flex items-end gap-2">
              {[40, 70, 30, 90, 55].map((h, j) => (
                <div key={j} className="w-6 rounded bg-brand/40" style={{ height: h }} />
              ))}
            </div>
          </div>
        ))}
      </div>

      {/* 게이트 오버레이 */}
      <div className="absolute inset-0 flex flex-col items-center justify-center gap-3 bg-white/70 p-6 text-center backdrop-blur-sm">
        <div className="text-2xl">🔒</div>
        <p className="max-w-sm text-sm font-medium text-slate-700">{message}</p>
        <div className="flex gap-2">
          <Link href="/feed" className="btn-primary">응답 피드로 가기</Link>
          <Link href="/home" className="btn-ghost">내 설문</Link>
        </div>
      </div>
    </div>
  );
}
