"use client";

import Link from "next/link";
import CreditBadge from "@/components/CreditBadge";
import UserSwitcher from "@/components/UserSwitcher";

export default function Header() {
  return (
    <header className="sticky top-0 z-20 border-b border-slate-200 bg-white/90 backdrop-blur">
      <div className="mx-auto flex max-w-5xl items-center justify-between gap-3 px-4 py-3">
        <nav className="flex items-center gap-4">
          <Link href="/" className="text-lg font-extrabold text-brand">
            품앗이<span className="text-slate-900">폼</span>
          </Link>
          <div className="hidden gap-3 text-sm text-slate-600 sm:flex">
            <Link href="/" className="hover:text-brand">내 설문</Link>
            <Link href="/feed" className="hover:text-brand">응답 피드</Link>
            <Link href="/forms/new" className="hover:text-brand">새 설문</Link>
          </div>
        </nav>
        <div className="flex items-center gap-3">
          <CreditBadge />
          <UserSwitcher />
        </div>
      </div>
    </header>
  );
}
