"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const TABS = [
  { href: "/home", label: "홈", match: (p: string) => p === "/home" },
  { href: "/feed", label: "피드", match: (p: string) => p.startsWith("/feed") },
  { href: "/forms/new", label: "만들기", match: (p: string) => p.startsWith("/forms/new") },
  { href: "/activity", label: "활동", match: (p: string) => p.startsWith("/activity") },
  { href: "/me", label: "내정보", match: (p: string) => p.startsWith("/me") },
];

/** 모바일 하단 탭. sm 이상에서는 숨김(헤더 내비 사용). */
export default function BottomNav() {
  const pathname = usePathname() || "";
  if (pathname === "/" || pathname === "/guide" || pathname === "/login") return null;

  return (
    <nav className="fixed inset-x-0 bottom-0 z-30 border-t border-slate-200 bg-white/95 backdrop-blur sm:hidden">
      <ul className="mx-auto flex max-w-5xl items-stretch justify-around px-1 pb-[env(safe-area-inset-bottom)]">
        {TABS.map((t) => {
          const active = t.match(pathname);
          return (
            <li key={t.href} className="flex-1">
              <Link
                href={t.href}
                className={`flex flex-col items-center py-2 text-[11px] font-semibold ${
                  active ? "text-brand" : "text-slate-500"
                }`}
              >
                <span
                  className={`mb-0.5 h-1 w-6 rounded-full ${active ? "bg-brand" : "bg-transparent"}`}
                />
                {t.label}
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
