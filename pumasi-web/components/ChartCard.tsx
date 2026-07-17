"use client";

import { useMemo, useState } from "react";
import {
  Bar,
  BarChart,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import type { ChartItem } from "@/lib/types";
import ChartTypeToggle, { type ToggleOption } from "@/components/ChartTypeToggle";

const COLORS = ["#4f46e5", "#06b6d4", "#f59e0b", "#ef4444", "#10b981", "#8b5cf6", "#ec4899", "#0ea5e9"];

type View = "pie" | "donut" | "bar" | "hbar" | "table" | "list" | "files" | "matrix";
type SortMode = "order" | "count";

const CHOICE_TYPES = ["RADIO", "CHECKBOX", "DROPDOWN"];
const SCALE_TYPES = ["LINEAR_SCALE", "RATING"];
const TEXT_TYPES = ["SHORT_TEXT", "LONG_TEXT"];
const GRID_TYPES = ["MULTIPLE_CHOICE_GRID", "CHECKBOX_GRID"];

function optionsFor(type: string, chartType: string): ToggleOption<View>[] {
  if (CHOICE_TYPES.includes(type))
    return [
      { id: "pie", label: "원형" },
      { id: "donut", label: "도넛" },
      { id: "bar", label: "세로막대" },
      { id: "hbar", label: "가로막대" },
      { id: "table", label: "표" },
    ];
  if (SCALE_TYPES.includes(type))
    return [
      { id: "bar", label: "세로막대" },
      { id: "hbar", label: "가로막대" },
      { id: "table", label: "표" },
    ];
  if (TEXT_TYPES.includes(type) || chartType === "text_freq")
    return [
      { id: "bar", label: "빈도" },
      { id: "list", label: "목록" },
      { id: "table", label: "표" },
    ];
  if (GRID_TYPES.includes(type) || chartType === "matrix")
    return [
      { id: "matrix", label: "표" },
      { id: "bar", label: "막대" },
    ];
  if (type === "FILE" || chartType === "file_list")
    return [{ id: "files", label: "파일" }];
  return [];
}

function defaultView(item: ChartItem): View {
  switch (item.chartType) {
    case "pie":
      return "pie";
    case "text_freq":
      return "bar";
    case "text_list":
      return "list";
    case "file_list":
      return "files";
    case "matrix":
      return "matrix";
    default:
      return "bar";
  }
}

function fileLabel(raw: string): string {
  try {
    const u = new URL(raw, "http://local");
    const parts = u.pathname.split("/");
    return parts[parts.length - 1] || raw;
  } catch {
    const parts = raw.split(/[/\\]/);
    return parts[parts.length - 1] || raw;
  }
}

export default function ChartCard({ item }: { item: ChartItem }) {
  const [view, setView] = useState<View>(defaultView(item));
  const [sortMode, setSortMode] = useState<SortMode>("order");
  const options = optionsFor(item.type, item.chartType);
  const isChoice = CHOICE_TYPES.includes(item.type);
  const isScale = SCALE_TYPES.includes(item.type);
  const isText = TEXT_TYPES.includes(item.type) || item.chartType === "text_freq";
  const isMatrix = GRID_TYPES.includes(item.type) || item.chartType === "matrix";

  const matrixRows = item.rowLabels ?? [];
  const matrixCols = item.columnLabels ?? [];

  const data = useMemo(() => {
    const rows = Object.entries(item.counts).map(([name, value]) => ({
      name: name.length > 40 ? name.slice(0, 37) + "…" : name,
      fullName: name,
      value,
      ratio: item.ratios[name] ?? 0,
    }));
    if ((isChoice || isText || isMatrix) && sortMode === "count") {
      return [...rows].sort((a, b) => b.value - a.value || a.name.localeCompare(b.name));
    }
    return rows;
  }, [item.counts, item.ratios, isChoice, isText, isMatrix, sortMode]);

  return (
    <div className="card space-y-3">
      <div className="flex items-start justify-between gap-2">
        <h3 className="font-bold">{item.title}</h3>
        <span className="badge shrink-0 bg-slate-100 text-slate-500">
          응답 {item.respondentCount}
        </span>
      </div>

      {item.ratioSumMayExceed100 && (
        <p className="text-xs text-slate-500">
          복수 선택이므로 보기별 비율 합이 100%를 넘을 수 있습니다.
        </p>
      )}

      <div className="flex flex-wrap items-center gap-2">
        {options.length > 1 && (
          <ChartTypeToggle options={options} value={view} onChange={setView} />
        )}
        {(isChoice || (isText && view === "bar")) && data.length > 1 && (
          <div className="flex gap-1 rounded-lg bg-slate-100 p-0.5 text-xs">
            {(
              [
                { id: "order" as const, label: "원본순" },
                { id: "count" as const, label: "응답수순" },
              ] as const
            ).map((s) => (
              <button
                key={s.id}
                type="button"
                onClick={() => setSortMode(s.id)}
                className={`rounded-md px-2 py-1 font-semibold ${
                  sortMode === s.id ? "bg-white text-brand shadow-sm" : "text-slate-500"
                }`}
              >
                {s.label}
              </button>
            ))}
          </div>
        )}
      </div>

      {view === "pie" || view === "donut" ? (
        <ResponsiveContainer width="100%" height={260}>
          <PieChart>
            <Pie
              data={data}
              dataKey="value"
              nameKey="name"
              outerRadius={95}
              innerRadius={view === "donut" ? 55 : 0}
              label={({ name, percent }) =>
                `${name} ${((percent ?? 0) * 100).toFixed(0)}%`
              }
            >
              {data.map((_, i) => (
                <Cell key={i} fill={COLORS[i % COLORS.length]} />
              ))}
            </Pie>
            <Tooltip formatter={(v: number, n) => [`${v}명`, n as string]} />
            <Legend />
          </PieChart>
        </ResponsiveContainer>
      ) : null}

      {view === "bar" && (
        <ResponsiveContainer width="100%" height={260}>
          <BarChart data={data}>
            <XAxis dataKey="name" fontSize={12} interval={0} angle={data.length > 8 ? -25 : 0} textAnchor={data.length > 8 ? "end" : "middle"} height={data.length > 8 ? 70 : 30} />
            <YAxis allowDecimals={false} fontSize={12} />
            <Tooltip formatter={(v: number) => [`${v}명`, "응답"]} />
            <Bar dataKey="value" radius={[4, 4, 0, 0]}>
              {data.map((_, i) => (
                <Cell key={i} fill={COLORS[i % COLORS.length]} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      )}

      {view === "hbar" && (
        <ResponsiveContainer width="100%" height={Math.max(200, data.length * 40)}>
          <BarChart data={data} layout="vertical" margin={{ left: 20 }}>
            <XAxis type="number" allowDecimals={false} fontSize={12} />
            <YAxis type="category" dataKey="name" width={100} fontSize={12} />
            <Tooltip formatter={(v: number) => [`${v}명`, "응답"]} />
            <Bar dataKey="value" radius={[0, 4, 4, 0]}>
              {data.map((_, i) => (
                <Cell key={i} fill={COLORS[i % COLORS.length]} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      )}

      {view === "matrix" && (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[240px] border-collapse text-sm">
            <thead>
              <tr className="border-b text-slate-500">
                <th className="py-1.5 pr-2 text-left font-medium" />
                {matrixCols.map((col) => (
                  <th key={col} className="px-2 py-1.5 text-center font-medium">
                    {col}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {matrixRows.map((row) => (
                <tr key={row} className="border-b border-slate-100">
                  <th className="py-1.5 pr-2 text-left font-medium text-slate-700">{row}</th>
                  {matrixCols.map((col) => {
                    const key = `${row}=${col}`;
                    const n = item.counts[key] ?? 0;
                    return (
                      <td key={col} className="px-2 py-1.5 text-center font-medium">
                        {n}
                      </td>
                    );
                  })}
                </tr>
              ))}
              {matrixRows.length === 0 && (
                <tr>
                  <td colSpan={Math.max(1, matrixCols.length + 1)} className="py-3 text-center text-slate-400">
                    응답 없음
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {view === "table" &&
        item.chartType !== "text_list" &&
        item.chartType !== "file_list" &&
        item.chartType !== "matrix" && (
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b text-left text-slate-500">
              <th className="py-1.5">항목</th>
              <th className="py-1.5 text-right">응답 수</th>
              <th className="py-1.5 text-right">비율</th>
            </tr>
          </thead>
          <tbody>
            {data.map((d) => (
              <tr key={d.fullName} className="border-b border-slate-100">
                <td className="py-1.5" title={d.fullName}>
                  {d.name}
                </td>
                <td className="py-1.5 text-right font-medium">{d.value}</td>
                <td className="py-1.5 text-right text-slate-500">{d.ratio.toFixed(1)}%</td>
              </tr>
            ))}
            {data.length === 0 && (
              <tr>
                <td colSpan={3} className="py-3 text-center text-slate-400">
                  응답 없음
                </td>
              </tr>
            )}
          </tbody>
        </table>
      )}

      {view === "list" && (
        <ul className="max-h-60 space-y-1 overflow-auto">
          {item.textResponses.length === 0 && (
            <li className="text-sm text-slate-400">응답 없음</li>
          )}
          {item.textResponses.map((t, i) => (
            <li key={i} className="rounded bg-slate-50 px-3 py-2 text-sm">
              {t}
            </li>
          ))}
        </ul>
      )}

      {view === "table" && (item.chartType === "text_list" || item.chartType === "text_freq") && (
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b text-left text-slate-500">
              <th className="w-12 py-1.5">번호</th>
              <th className="py-1.5">응답</th>
            </tr>
          </thead>
          <tbody>
            {item.textResponses.map((t, i) => (
              <tr key={i} className="border-b border-slate-100">
                <td className="py-1.5 text-slate-400">{i + 1}</td>
                <td className="py-1.5">{t}</td>
              </tr>
            ))}
            {item.textResponses.length === 0 && (
              <tr>
                <td colSpan={2} className="py-3 text-center text-slate-400">
                  응답 없음
                </td>
              </tr>
            )}
          </tbody>
        </table>
      )}

      {view === "files" && (
        <ul className="max-h-60 space-y-1 overflow-auto">
          {item.textResponses.length === 0 && (
            <li className="text-sm text-slate-400">업로드된 파일 없음</li>
          )}
          {item.textResponses.map((t, i) => (
            <li key={i} className="rounded bg-slate-50 px-3 py-2 text-sm">
              <span className="font-medium text-slate-800">{fileLabel(t)}</span>
              {t !== fileLabel(t) && (
                <span className="mt-0.5 block truncate text-xs text-slate-400">{t}</span>
              )}
            </li>
          ))}
        </ul>
      )}

      {isScale && (
        <p className="text-sm text-slate-500">
          평균 <b>{item.average.toFixed(2)}</b> · 중앙값 <b>{item.median}</b> · 응답{" "}
          <b>{item.respondentCount}</b>
        </p>
      )}

      {item.chartType === "unsupported" && (
        <p className="text-sm text-slate-400">이 유형은 아직 시각화를 지원하지 않습니다.</p>
      )}

      {item.respondentCount === 0 && item.chartType !== "unsupported" && (
        <p className="text-xs text-slate-400">
          열린(unlocked) 성실 응답이 없어 집계할 데이터가 없습니다.
        </p>
      )}
    </div>
  );
}
