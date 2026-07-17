import type { Metadata } from "next";
import AppShell from "@/components/AppShell";

export const metadata: Metadata = {
  title: "설문",
  robots: { index: false, follow: false },
};

export default function FormsLayout({ children }: { children: React.ReactNode }) {
  return <AppShell>{children}</AppShell>;
}
