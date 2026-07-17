import type { Metadata } from "next";
import AppShell from "@/components/AppShell";

export const metadata: Metadata = {
  title: "응답 피드",
  robots: { index: false, follow: false },
};

export default function FeedLayout({ children }: { children: React.ReactNode }) {
  return <AppShell>{children}</AppShell>;
}
