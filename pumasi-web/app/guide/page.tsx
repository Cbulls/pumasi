import Link from "next/link";
import type { Metadata } from "next";
import { GUIDE_CAUTIONS, GUIDE_RULE_SUMMARY, GUIDE_STEPS } from "@/lib/guide";
import { SITE_NAME, SITE_URL } from "@/lib/site";

export const metadata: Metadata = {
  title: "품앗이 안내",
  description:
    "품앗이폼의 1:1 상호 응답 규칙, 이용 흐름, 유의사항을 안내합니다. 상대 설문에 답해야 그 사람의 응답을 볼 수 있습니다.",
  alternates: { canonical: `${SITE_URL}/guide` },
  openGraph: {
    title: `품앗이 안내 | ${SITE_NAME}`,
    description: GUIDE_RULE_SUMMARY,
    url: `${SITE_URL}/guide`,
  },
};

export default function GuidePage() {
  return (
    <div className="bg-ink text-slate-100">
      <section className="relative overflow-hidden border-b border-white/10">
        <div
          aria-hidden
          className="pointer-events-none absolute inset-0"
          style={{
            background:
              "radial-gradient(ellipse 70% 50% at 80% 0%, rgba(14,165,233,0.25), transparent 55%), linear-gradient(165deg, #0f172a 0%, #1e293b 100%)",
          }}
        />
        <div className="relative mx-auto max-w-5xl px-4 py-16 sm:px-6 sm:py-20">
          <p className="font-display text-sm font-semibold tracking-[0.2em] text-sky-300">
            HOW PUMASI WORKS
          </p>
          <h1 className="font-display mt-3 text-4xl font-extrabold tracking-tight text-white sm:text-5xl">
            품앗이 안내
          </h1>
          <p className="mt-5 max-w-2xl text-lg leading-relaxed text-slate-300">
            {GUIDE_RULE_SUMMARY}
          </p>
        </div>
      </section>

      <section className="border-b border-white/10">
        <div className="mx-auto max-w-5xl px-4 py-16 sm:px-6">
          <h2 className="font-display text-2xl font-bold text-white sm:text-3xl">핵심 규칙</h2>
          <p className="mt-4 max-w-2xl text-base leading-relaxed text-slate-300">
            품앗이폼은 단순 크레딧 교환만으로 응답을 열지 않습니다.{" "}
            <strong className="font-semibold text-white">서로 설문에 답하는 관계</strong>가
            있어야 상대의 답을 볼 수 있습니다. 예를 들어 민수가 지수의 설문에 답했다면,
            지수는 민수의 설문에 답해야 민수의 응답을 열람할 수 있습니다.
          </p>
        </div>
      </section>

      <section className="border-b border-white/10 bg-ink-soft">
        <div className="mx-auto max-w-5xl px-4 py-16 sm:px-6">
          <h2 className="font-display text-2xl font-bold text-white sm:text-3xl">이용 흐름</h2>
          <p className="mt-3 text-slate-400">게시부터 응답 열림까지</p>
          <ol className="mt-10 grid gap-8 sm:grid-cols-2">
            {GUIDE_STEPS.map((s) => (
              <li key={s.n}>
                <p className="font-display text-sm font-bold tracking-widest text-sky-400">
                  {s.n}
                </p>
                <h3 className="mt-2 font-display text-xl font-bold text-white">{s.title}</h3>
                <p className="mt-2 text-sm leading-relaxed text-slate-400">{s.body}</p>
              </li>
            ))}
          </ol>
        </div>
      </section>

      <section className="border-b border-white/10 bg-slate-50 text-slate-900">
        <div className="mx-auto max-w-5xl px-4 py-16 sm:px-6">
          <h2 className="font-display text-2xl font-bold sm:text-3xl">유의사항</h2>
          <p className="mt-3 text-slate-600">시작하기 전에 알아두면 좋은 점입니다.</p>
          <ul className="mt-8 space-y-4">
            {GUIDE_CAUTIONS.map((item) => (
              <li
                key={item}
                className="border-b border-slate-200 pb-4 text-sm leading-relaxed text-slate-700 last:border-0 sm:text-base"
              >
                {item}
              </li>
            ))}
          </ul>
        </div>
      </section>

      <section>
        <div className="mx-auto flex max-w-5xl flex-col items-start gap-6 px-4 py-16 sm:flex-row sm:items-center sm:justify-between sm:px-6">
          <div>
            <h2 className="font-display text-2xl font-bold text-white">준비됐다면 시작하세요</h2>
            <p className="mt-2 text-slate-400">내 설문으로 가거나, 피드에서 먼저 응답해 보세요.</p>
          </div>
          <div className="flex flex-wrap gap-3">
            <Link
              href="/home"
              className="inline-flex items-center justify-center rounded-lg bg-sky-400 px-5 py-2.5 text-sm font-bold text-ink hover:bg-sky-300"
            >
              시작하기
            </Link>
            <Link
              href="/feed"
              className="inline-flex items-center justify-center rounded-lg border border-white/30 px-5 py-2.5 text-sm font-semibold text-white hover:bg-white/10"
            >
              응답 피드
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
