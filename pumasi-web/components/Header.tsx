"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import CreditBadge from "@/components/CreditBadge";
import UserSwitcher from "@/components/UserSwitcher";

export default function Header() {
  const pathname = usePathname();
  const isMarketing = pathname === "/" || pathname === "/guide";

  return (
    <header
      className={
        isMarketing
          ? "sticky top-0 z-20 border-b border-white/10 bg-ink/80 backdrop-blur"
          : "sticky top-0 z-20 border-b border-slate-200 bg-white/90 backdrop-blur"
      }
    >
      <div className="mx-auto flex max-w-5xl items-center justify-between gap-3 px-4 py-3">
        <nav className="flex items-center gap-4">
          <Link
            href="/"
            className={
              isMarketing
                ? "font-display text-lg font-extrabold text-sky-300"
                : "font-display text-lg font-extrabold text-brand"
            }
          >
            품앗이
            <span className={isMarketing ? "text-white" : "text-slate-900"}>폼</span>
          </Link>
          {isMarketing ? (
            <div className="hidden gap-3 text-sm text-slate-300 sm:flex">
              <Link href="/guide" className="hover:text-white">
                품앗이 안내
              </Link>
              {pathname === "/" && (
                <a href="#faq" className="hover:text-white">
                  FAQ
                </a>
              )}
              <Link href="/home" className="hover:text-white">
                시작하기
              </Link>
            </div>
          ) : (
            <div className="hidden gap-3 text-sm text-slate-600 sm:flex">
              <Link href="/home" className="hover:text-brand">
                내 설문
              </Link>
              <Link href="/feed" className="hover:text-brand">
                응답 피드
              </Link>
              <Link href="/forms/new" className="hover:text-brand">
                새 설문
              </Link>
              <Link href="/guide" className="hover:text-brand">
                품앗이 안내
              </Link>
            </div>
          )}
        </nav>
        <div className="flex items-center gap-3">
          {isMarketing ? (
            <Link
              href="/home"
              className="rounded-lg bg-sky-400 px-3 py-1.5 text-sm font-bold text-ink hover:bg-sky-300"
            >
              시작하기
            </Link>
          ) : (
            <>
              <CreditBadge />
              <UserSwitcher />
            </>
          )}
        </div>
      </div>
    </header>
  );
}
