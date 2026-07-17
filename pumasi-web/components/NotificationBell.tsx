"use client";

import { useState } from "react";
import Link from "next/link";
import { useMarkAllNotificationsRead, useMarkNotificationRead, useNotifications } from "@/lib/hooks";

const TYPE_LABEL: Record<string, string> = {
  NEW_RESPONSE: "새 응답",
  UNLOCK_AVAILABLE: "언락 가능",
  FORM_PAUSED: "일시정지",
  HOLD_REVIEW: "HOLD 검토",
};

export default function NotificationBell() {
  const { data, isLoading } = useNotifications();
  const markRead = useMarkNotificationRead();
  const markAll = useMarkAllNotificationsRead();
  const [open, setOpen] = useState(false);
  const unread = data?.unreadCount ?? 0;

  return (
    <div className="relative">
      <button
        type="button"
        className="relative rounded-lg border border-slate-200 px-2 py-1.5 text-sm hover:bg-slate-50"
        aria-label="알림"
        onClick={() => setOpen((v) => !v)}
      >
        알림
        {unread > 0 && (
          <span className="absolute -right-1 -top-1 flex h-4 min-w-[1rem] items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-bold text-white">
            {unread > 9 ? "9+" : unread}
          </span>
        )}
      </button>
      {open && (
        <div className="absolute right-0 z-40 mt-2 w-80 max-w-[90vw] rounded-xl border border-slate-200 bg-white shadow-lg">
          <div className="flex items-center justify-between border-b border-slate-100 px-3 py-2">
            <span className="text-sm font-bold">알림</span>
            <button
              type="button"
              className="text-xs text-brand hover:underline disabled:opacity-40"
              disabled={markAll.isPending || unread === 0}
              onClick={() => markAll.mutate()}
            >
              모두 읽음
            </button>
          </div>
          <ul className="max-h-80 overflow-auto">
            {isLoading && <li className="p-3 text-sm text-slate-500">불러오는 중…</li>}
            {data?.items?.length === 0 && (
              <li className="p-3 text-sm text-slate-500">새 알림이 없습니다.</li>
            )}
            {data?.items?.map((n) => (
              <li
                key={n.id}
                className={`border-b border-slate-50 px-3 py-2 text-sm ${
                  n.readAt ? "bg-white" : "bg-sky-50/60"
                }`}
              >
                <p className="text-[11px] font-semibold text-slate-400">
                  {TYPE_LABEL[n.type] ?? n.type}
                </p>
                <p className="font-semibold text-slate-800">{n.title}</p>
                {n.body && <p className="mt-0.5 text-xs text-slate-500">{n.body}</p>}
                <div className="mt-1 flex gap-2">
                  {n.linkUrl && (
                    <Link
                      href={n.linkUrl}
                      className="text-xs font-semibold text-brand hover:underline"
                      onClick={() => {
                        if (!n.readAt) markRead.mutate(n.id);
                        setOpen(false);
                      }}
                    >
                      바로가기
                    </Link>
                  )}
                  {!n.readAt && (
                    <button
                      type="button"
                      className="text-xs text-slate-500 hover:underline"
                      onClick={() => markRead.mutate(n.id)}
                    >
                      읽음
                    </button>
                  )}
                </div>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
