import type { Metadata } from "next";
import AppShell from "@/components/AppShell";

export const metadata: Metadata = {
  title: "내 활동",
  robots: { index: false, follow: false },
};

export default function ActivityLayout({ children }: { children: React.ReactNode }) {
  return <AppShell>{children}</AppShell>;
}
