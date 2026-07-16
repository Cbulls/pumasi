"use client";

import { serverCsvUrl, useResponsesTable } from "@/lib/hooks";
import { useCurrentUser } from "@/context/CurrentUserContext";
import type { ResponsesTable as ResponsesTableData } from "@/lib/types";

const FLAG_CLS: Record<string, string> = {
  pass: "bg-emerald-100 text-emerald-700",
  hold: "bg-amber-100 text-amber-700",
  reject: "bg-red-100 text-red-700",
};

function buildCsv(table: ResponsesTableData): string {
  const esc = (s: unknown) => `"${String(s ?? "").replace(/"/g, '""')}"`;
  const header = ["익명ID", "품질", "제출시각", ...table.questions.map((q) => q.title)];
  const lines = [header.map(esc).join(",")];
  for (const row of table.rows) {
    const cells = [
      row.anonLabel,
      row.qualityFlag,
      row.submittedAt,
      ...table.questions.map((q) => row.answers[q.questionId] ?? ""),
    ];
    lines.push(cells.map(esc).join(","));
  }
  return lines.join("\n");
}

function downloadCsv(table: ResponsesTableData) {
  // BOM 추가(엑셀에서 한글 깨짐 방지)
  const blob = new Blob(["\uFEFF" + buildCsv(table)], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = "responses.csv";
  a.click();
  URL.revokeObjectURL(url);
}

export default function ResponsesTable({ formId, active }: { formId: string; active: boolean }) {
  const { token } = useCurrentUser();
  const { data, isLoading, isError, error } = useResponsesTable(formId, active);

  const downloadServerCsv = async () => {
    const res = await fetch(serverCsvUrl(formId), {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });
    if (!res.ok) {
      alert("CSV 다운로드에 실패했습니다.");
      return;
    }
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "responses.csv";
    a.click();
    URL.revokeObjectURL(url);
  };

  if (isLoading) return <p className="text-slate-500">개별 응답 불러오는 중…</p>;
  if (isError)
    return <p className="rounded-lg bg-red-50 p-3 text-sm text-red-600">{(error as Error).message}</p>;
  if (!data) return null;

  if (data.rows.length === 0) {
    return <div className="card text-center text-slate-500">아직 응답이 없습니다.</div>;
  }

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <p className="text-sm text-slate-500">
          총 {data.rows.length}개 응답 · 응답자는 익명 라벨로만 표시됩니다.
        </p>
        <div className="flex gap-2">
          <button className="btn-ghost" onClick={() => downloadCsv(data)}>
            CSV (브라우저)
          </button>
          <button className="btn-primary" onClick={downloadServerCsv}>
            CSV (서버)
          </button>
        </div>
      </div>

      <div className="overflow-auto rounded-xl border border-slate-200">
        <table className="min-w-full border-collapse text-sm">
          <thead className="bg-slate-50">
            <tr className="text-left">
              <th className="sticky left-0 z-10 bg-slate-50 px-3 py-2 font-semibold">익명 ID</th>
              <th className="px-3 py-2 font-semibold">품질</th>
              <th className="px-3 py-2 font-semibold whitespace-nowrap">제출시각</th>
              {data.questions.map((q) => (
                <th key={q.questionId} className="px-3 py-2 font-semibold whitespace-nowrap">
                  {q.title}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {data.rows.map((row, i) => (
              <tr key={i} className="border-t border-slate-100">
                <td className="sticky left-0 z-10 bg-white px-3 py-2 font-medium">{row.anonLabel}</td>
                <td className="px-3 py-2">
                  <span className={`badge ${FLAG_CLS[row.qualityFlag] ?? "bg-slate-100 text-slate-600"}`}>
                    {row.qualityFlag}
                  </span>
                </td>
                <td className="px-3 py-2 whitespace-nowrap text-slate-500">{row.submittedAt}</td>
                {data.questions.map((q) => (
                  <td key={q.questionId} className="px-3 py-2">
                    {row.answers[q.questionId] ?? <span className="text-slate-300">-</span>}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
