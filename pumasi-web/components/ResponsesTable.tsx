"use client";

import Link from "next/link";
import { useState } from "react";
import { serverCsvUrl, useResponsesTable } from "@/lib/hooks";
import { useCurrentUser } from "@/context/CurrentUserContext";

const FLAG_CLS: Record<string, string> = {
  pass: "bg-emerald-100 text-emerald-700",
  hold: "bg-amber-100 text-amber-700",
  reject: "bg-red-100 text-red-700",
};

/** Content-Disposition에서 filename* / filename 추출 */
function filenameFromDisposition(header: string | null, fallback: string): string {
  if (!header) return fallback;
  const star = /filename\*\s*=\s*UTF-8''([^;]+)/i.exec(header);
  if (star?.[1]) {
    try {
      return decodeURIComponent(star[1].trim().replace(/^["']|["']$/g, ""));
    } catch {
      /* fall through */
    }
  }
  const plain = /filename\s*=\s*"([^"]+)"/i.exec(header)
    ?? /filename\s*=\s*([^;]+)/i.exec(header);
  if (plain?.[1]) return plain[1].trim();
  return fallback;
}

export default function ResponsesTable({ formId, active }: { formId: string; active: boolean }) {
  const { token } = useCurrentUser();
  const { data, isLoading, isError, error } = useResponsesTable(formId, active);
  const [downloading, setDownloading] = useState(false);

  const downloadServerCsv = async () => {
    setDownloading(true);
    try {
      const res = await fetch(serverCsvUrl(formId), {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      if (!res.ok) {
        alert("CSV 다운로드에 실패했습니다.");
        return;
      }
      const blob = await res.blob();
      const name = filenameFromDisposition(
        res.headers.get("Content-Disposition"),
        `pumasi-export-${formId.slice(0, 8)}.csv`
      );
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = name;
      a.click();
      URL.revokeObjectURL(url);
    } finally {
      setDownloading(false);
    }
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
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div className="text-sm text-slate-500">
          <p>
            총 {data.rows.length}개 · 열림 {data.unlockedCount ?? 0} · 잠김 {data.lockedCount ?? 0}
          </p>
          {data.reciprocityRule && (
            <p className="mt-1 text-xs text-slate-400">{data.reciprocityRule}</p>
          )}
          <p className="mt-1 text-xs text-slate-400">
            CSV에서 잠긴 응답은 열림=N이고 답 칸은 비웁니다. UTF-8 BOM이라 엑셀에서 한글이 깨지지
            않습니다.
          </p>
        </div>
        <button
          type="button"
          className="btn-primary shrink-0"
          disabled={downloading}
          onClick={downloadServerCsv}
        >
          {downloading ? "내려받는 중…" : "엑셀용 CSV 다운로드"}
        </button>
      </div>

      <div className="overflow-auto rounded-xl border border-slate-200">
        <table className="min-w-full border-collapse text-sm">
          <thead className="bg-slate-50">
            <tr className="text-left">
              <th className="sticky left-0 z-10 bg-slate-50 px-3 py-2 font-semibold">익명 ID</th>
              <th className="px-3 py-2 font-semibold">상태</th>
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
            {data.rows.map((row, i) => {
              const locked = row.unlocked === false;
              return (
                <tr
                  key={i}
                  className={`border-t border-slate-100 ${locked ? "bg-slate-50/80" : ""}`}
                >
                  <td className="sticky left-0 z-10 bg-inherit px-3 py-2 font-medium">
                    {row.anonLabel}
                  </td>
                  <td className="px-3 py-2">
                    {locked ? (
                      <div className="space-y-1">
                        <span className="badge bg-amber-100 text-amber-800">잠김</span>
                        {row.unlockFormId ? (
                          <Link
                            href={`/forms/${row.unlockFormId}/respond`}
                            className="block text-xs font-semibold text-brand hover:underline"
                          >
                            상대 설문 응답하기
                            {row.unlockFormTitle ? ` · ${row.unlockFormTitle}` : ""}
                          </Link>
                        ) : (
                          <p className="text-xs text-slate-500">{row.unlockHint}</p>
                        )}
                      </div>
                    ) : (
                      <span className="badge bg-emerald-100 text-emerald-700">열림</span>
                    )}
                  </td>
                  <td className="px-3 py-2">
                    <span
                      className={`badge ${FLAG_CLS[row.qualityFlag] ?? "bg-slate-100 text-slate-600"}`}
                    >
                      {row.qualityFlag}
                    </span>
                  </td>
                  <td className="px-3 py-2 whitespace-nowrap text-slate-500">{row.submittedAt}</td>
                  {data.questions.map((q) => (
                    <td
                      key={q.questionId}
                      className={`px-3 py-2 ${locked ? "italic text-slate-400 blur-[2px] select-none" : ""}`}
                    >
                      {row.answers[q.questionId] ?? <span className="text-slate-300">-</span>}
                    </td>
                  ))}
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
