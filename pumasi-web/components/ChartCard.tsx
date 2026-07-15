"use client";

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

const COLORS = ["#4f46e5", "#06b6d4", "#f59e0b", "#ef4444", "#10b981", "#8b5cf6", "#ec4899"];

function toData(counts: Record<string, number>) {
  return Object.entries(counts).map(([name, value]) => ({ name, value }));
}

export default function ChartCard({ item }: { item: ChartItem }) {
  const data = toData(item.counts);

  return (
    <div className="card space-y-3">
      <div className="flex items-start justify-between gap-2">
        <h3 className="font-bold">{item.title}</h3>
        <span className="badge bg-slate-100 text-slate-500">응답 {item.respondentCount}</span>
      </div>

      {item.chartType === "pie" && (
        <ResponsiveContainer width="100%" height={240}>
          <PieChart>
            <Pie data={data} dataKey="value" nameKey="name" outerRadius={90} label>
              {data.map((_, i) => (
                <Cell key={i} fill={COLORS[i % COLORS.length]} />
              ))}
            </Pie>
            <Tooltip />
            <Legend />
          </PieChart>
        </ResponsiveContainer>
      )}

      {(item.chartType === "bar" || item.chartType === "histogram") && (
        <>
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={data}>
              <XAxis dataKey="name" fontSize={12} />
              <YAxis allowDecimals={false} fontSize={12} />
              <Tooltip />
              <Bar dataKey="value" radius={[4, 4, 0, 0]}>
                {data.map((_, i) => (
                  <Cell key={i} fill={COLORS[i % COLORS.length]} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
          {item.chartType === "histogram" && (
            <p className="text-sm text-slate-500">
              평균 <b>{item.average.toFixed(2)}</b> · 중앙값 <b>{item.median}</b>
            </p>
          )}
        </>
      )}

      {item.chartType === "text_list" && (
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

      {item.chartType === "unsupported" && (
        <p className="text-sm text-slate-400">이 유형은 아직 시각화를 지원하지 않습니다.</p>
      )}
    </div>
  );
}
