"use client";

import { useState } from "react";
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

type View = "pie" | "donut" | "bar" | "hbar" | "table" | "list";

const CHOICE_TYPES = ["RADIO", "CHECKBOX", "DROPDOWN"];
const SCALE_TYPES = ["LINEAR_SCALE", "RATING"];
const TEXT_TYPES = ["SHORT_TEXT", "LONG_TEXT"];

function optionsFor(type: string): ToggleOption<View>[] {
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
  if (TEXT_TYPES.includes(type))
    return [
      { id: "list", label: "목록" },
      { id: "table", label: "표" },
    ];
  return [];
}

function defaultView(item: ChartItem): View {
  switch (item.chartType) {
    case "pie":
      return "pie";
    case "text_list":
      return "list";
    default:
      return "bar";
  }
}

export default function ChartCard({ item }: { item: ChartItem }) {
  const [view, setView] = useState<View>(defaultView(item));
  const options = optionsFor(item.type);

  const data = Object.entries(item.counts).map(([name, value]) => ({
    name,
    value,
    ratio: item.ratios[name] ?? 0,
  }));

  const isScale = SCALE_TYPES.includes(item.type);

  return (
    <div className="card space-y-3">
      <div className="flex items-start justify-between gap-2">
        <h3 className="font-bold">{item.title}</h3>
        <span className="badge shrink-0 bg-slate-100 text-slate-500">응답 {item.respondentCount}</span>
      </div>

      {options.length > 1 && (
        <ChartTypeToggle options={options} value={view} onChange={setView} />
      )}

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
            <XAxis dataKey="name" fontSize={12} />
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

      {view === "table" && item.chartType !== "text_list" && (
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
              <tr key={d.name} className="border-b border-slate-100">
                <td className="py-1.5">{d.name}</td>
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

      {view === "table" && item.chartType === "text_list" && (
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

      {isScale && (
        <p className="text-sm text-slate-500">
          평균 <b>{item.average.toFixed(2)}</b> · 중앙값 <b>{item.median}</b> · 응답{" "}
          <b>{item.respondentCount}</b>
        </p>
      )}

      {item.chartType === "unsupported" && (
        <p className="text-sm text-slate-400">이 유형은 아직 시각화를 지원하지 않습니다.</p>
      )}
    </div>
  );
}
