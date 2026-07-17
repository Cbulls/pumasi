import Link from "next/link";
import type { Metadata } from "next";
import JsonLd from "@/components/JsonLd";
import { LANDING_FAQ } from "@/lib/faq";
import {
  SITE_DESCRIPTION,
  SITE_NAME,
  SITE_NAME_EN,
  SITE_TAGLINE,
  SITE_URL,
} from "@/lib/site";

export const metadata: Metadata = {
  title: { absolute: `${SITE_NAME} — 설문 품앗이 플랫폼` },
  description: SITE_DESCRIPTION,
  alternates: { canonical: SITE_URL },
  openGraph: {
    title: `${SITE_NAME} — 설문 품앗이 플랫폼`,
    description: SITE_TAGLINE,
    url: SITE_URL,
  },
};

const steps = [
  {
    n: "01",
    title: "설문을 게시한다",
    body: "크레딧을 예치해 내 설문을 올리고 응답자를 모읍니다.",
  },
  {
    n: "02",
    title: "상대가 내게 응답한다",
    body: "응답이 들어오면 그 사람의 답은 잠긴 채로 대기합니다.",
  },
  {
    n: "03",
    title: "상대 설문에 답해 연다",
    body: "그 사람의 설문에 응답하면 그 사람의 내 설문 응답이 열립니다.",
  },
] as const;

export default function LandingPage() {
  const jsonLd = [
    {
      "@context": "https://schema.org",
      "@type": "WebApplication",
      name: SITE_NAME,
      alternateName: SITE_NAME_EN,
      url: SITE_URL,
      description: SITE_DESCRIPTION,
      applicationCategory: "BusinessApplication",
      operatingSystem: "Web",
      inLanguage: "ko-KR",
      offers: {
        "@type": "Offer",
        price: "0",
        priceCurrency: "KRW",
      },
    },
    {
      "@context": "https://schema.org",
      "@type": "FAQPage",
      mainEntity: LANDING_FAQ.map((item) => ({
        "@type": "Question",
        name: item.question,
        acceptedAnswer: {
          "@type": "Answer",
          text: item.answer,
        },
      })),
    },
  ];

  return (
    <>
      <JsonLd data={jsonLd} />
      <div className="bg-ink text-slate-100">
        {/* Hero — brand first, one composition, full-bleed atmosphere */}
        <section className="relative min-h-[88vh] overflow-hidden">
          <div
            aria-hidden
            className="pointer-events-none absolute inset-0"
            style={{
              background:
                "radial-gradient(ellipse 80% 60% at 70% 20%, rgba(14,165,233,0.28), transparent 55%), radial-gradient(ellipse 50% 40% at 10% 80%, rgba(79,70,229,0.22), transparent 50%), linear-gradient(165deg, #0f172a 0%, #1e293b 45%, #0c4a6e 100%)",
            }}
          />
          <div
            aria-hidden
            className="pointer-events-none absolute inset-0 opacity-[0.12]"
            style={{
              backgroundImage:
                "url(\"data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='1'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E\")",
            }}
          />
          <div className="relative mx-auto flex min-h-[88vh] max-w-5xl flex-col justify-center px-4 py-20 sm:px-6">
            <p className="font-display text-sm font-semibold tracking-[0.28em] text-sky-300">
              {SITE_NAME_EN}
            </p>
            <h1 className="font-display mt-4 text-5xl font-extrabold tracking-tight text-white sm:text-7xl">
              {SITE_NAME}
            </h1>
            <p className="mt-6 max-w-xl text-lg leading-relaxed text-slate-300 sm:text-xl">
              {SITE_TAGLINE}
            </p>
            <div className="mt-10 flex flex-wrap gap-3">
              <Link
                href="/home"
                className="inline-flex items-center justify-center rounded-lg bg-sky-400 px-6 py-3 text-sm font-bold text-ink transition hover:bg-sky-300"
              >
                시작하기
              </Link>
              <Link
                href="/feed"
                className="inline-flex items-center justify-center rounded-lg border border-white/30 px-6 py-3 text-sm font-semibold text-white transition hover:bg-white/10"
              >
                응답 피드 보기
              </Link>
            </div>
          </div>
        </section>

        {/* Problem → solution */}
        <section className="border-t border-white/10 bg-ink-soft">
          <div className="mx-auto max-w-5xl px-4 py-20 sm:px-6">
            <h2 className="font-display text-3xl font-bold text-white sm:text-4xl">
              설문은 쉬운데, 응답자가 없다
            </h2>
            <p className="mt-5 max-w-2xl text-base leading-relaxed text-slate-300 sm:text-lg">
              논문·스타트업·마케팅 설문 모두 같은 벽에 부딪힙니다. 품앗이폼은 1:1 상호 응답으로
              그 벽을 넘습니다. 상대가 내게 답하면, 나도 상대 설문에 답해야 그 응답을 볼 수
              있습니다.
            </p>
          </div>
        </section>

        {/* How it works */}
        <section className="border-t border-white/10">
          <div className="mx-auto max-w-5xl px-4 py-20 sm:px-6">
            <h2 className="font-display text-3xl font-bold text-white sm:text-4xl">
              세 걸음이면 충분합니다
            </h2>
            <p className="mt-3 text-slate-400">설문 품앗이의 기본 흐름</p>
            <ol className="mt-12 grid gap-10 sm:grid-cols-3">
              {steps.map((s) => (
                <li key={s.n}>
                  <p className="font-display text-sm font-bold tracking-widest text-sky-400">
                    {s.n}
                  </p>
                  <h3 className="mt-3 font-display text-2xl font-bold text-white">
                    {s.title}
                  </h3>
                  <p className="mt-2 text-sm leading-relaxed text-slate-400">{s.body}</p>
                </li>
              ))}
            </ol>
          </div>
        </section>

        {/* FAQ — AEO */}
        <section className="border-t border-white/10 bg-slate-50 text-slate-900" id="faq">
          <div className="mx-auto max-w-5xl px-4 py-20 sm:px-6">
            <h2 className="font-display text-3xl font-bold sm:text-4xl">자주 묻는 질문</h2>
            <p className="mt-3 text-slate-600">
              설문조사 플랫폼·응답자 모집·구글폼 대안을 찾는 분들을 위한 안내입니다.
            </p>
            <dl className="mt-12 space-y-8">
              {LANDING_FAQ.map((item) => (
                <div key={item.question} className="border-b border-slate-200 pb-8 last:border-0">
                  <dt className="font-display text-lg font-bold text-ink">{item.question}</dt>
                  <dd className="mt-2 text-sm leading-relaxed text-slate-600 sm:text-base">
                    {item.answer}
                  </dd>
                </div>
              ))}
            </dl>
          </div>
        </section>

        {/* Bottom CTA */}
        <section className="border-t border-white/10 bg-ink">
          <div className="mx-auto flex max-w-5xl flex-col items-start gap-6 px-4 py-20 sm:flex-row sm:items-center sm:justify-between sm:px-6">
            <div>
              <h2 className="font-display text-2xl font-bold text-white sm:text-3xl">
                지금 설문을 열고 응답자를 모으세요
              </h2>
              <p className="mt-2 text-slate-400">계정만 있으면 바로 시작할 수 있습니다.</p>
            </div>
            <Link
              href="/home"
              className="inline-flex shrink-0 items-center justify-center rounded-lg bg-sky-400 px-6 py-3 text-sm font-bold text-ink transition hover:bg-sky-300"
            >
              내 설문으로 이동
            </Link>
          </div>
        </section>

        <footer className="border-t border-white/10 py-8 text-center text-xs text-slate-500">
          © {new Date().getFullYear()} {SITE_NAME} ({SITE_NAME_EN})
        </footer>
      </div>
    </>
  );
}
