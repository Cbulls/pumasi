import type { Metadata } from "next";
import AppShell from "@/components/AppShell";

export const metadata: Metadata = {
  title: "내 설문",
  robots: { index: false, follow: false },
};

export default function HomeLayout({ children }: { children: React.ReactNode }) {
  return <AppShell>{children}</AppShell>;
}
