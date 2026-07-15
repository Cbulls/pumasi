"use client";

import Link from "next/link";

/**
 * 결과 게이트(요구사항 와이어프레임 ②) 감성의 블러 오버레이.
 *
 * 참고: 백엔드 D7 상 결과 열람은 무료이며, 이 화면은 "본인 설문이 아닐 때"의
 * 접근 차단(403)을 시각적으로 표현한다(크레딧 게이트는 데모 표기).
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
          <Link href="/" className="btn-ghost">내 설문</Link>
        </div>
      </div>
    </div>
  );
}
