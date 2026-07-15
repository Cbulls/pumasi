"use client";

export interface ToggleOption<T extends string> {
  id: T;
  label: string;
}

interface Props<T extends string> {
  options: ToggleOption<T>[];
  value: T;
  onChange: (v: T) => void;
}

/** 차트/표 보기 전환용 세그먼트 버튼. */
export default function ChartTypeToggle<T extends string>({ options, value, onChange }: Props<T>) {
  return (
    <div className="flex flex-wrap gap-1">
      {options.map((o) => (
        <button
          key={o.id}
          type="button"
          onClick={() => onChange(o.id)}
          className={`rounded-md px-2.5 py-1 text-xs font-semibold transition ${
            value === o.id
              ? "bg-brand text-white"
              : "bg-slate-100 text-slate-600 hover:bg-slate-200"
          }`}
        >
          {o.label}
        </button>
      ))}
    </div>
  );
}
