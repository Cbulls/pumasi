import BottomNav from "@/components/BottomNav";
import RequireAuth from "@/components/RequireAuth";

export default function AppShell({ children }: { children: React.ReactNode }) {
  return (
    <RequireAuth>
      <main className="mx-auto max-w-5xl px-4 py-6 pb-24 sm:pb-6">{children}</main>
      <BottomNav />
    </RequireAuth>
  );
}
