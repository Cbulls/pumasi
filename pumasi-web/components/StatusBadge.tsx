import type { FormStatus } from "@/lib/types";

const MAP: Record<FormStatus, { label: string; cls: string }> = {
  DRAFT: { label: "작성중", cls: "bg-amber-100 text-amber-700" },
  ACTIVE: { label: "게시중", cls: "bg-emerald-100 text-emerald-700" },
  CLOSED: { label: "마감", cls: "bg-slate-200 text-slate-600" },
};

export default function StatusBadge({ status }: { status: FormStatus }) {
  const s = MAP[status] ?? MAP.DRAFT;
  return <span className={`badge ${s.cls}`}>{s.label}</span>;
}
