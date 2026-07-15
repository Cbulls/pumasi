"use client";

import { useCurrentUser } from "@/context/CurrentUserContext";

/** 인증 스텁: 데모 사용자 전환 드롭다운. 선택값이 모든 요청의 X-User-Id 가 된다. */
export default function UserSwitcher() {
  const { userId, setUserId, users } = useCurrentUser();
  return (
    <label className="flex items-center gap-2 text-sm">
      <span className="hidden text-slate-500 sm:inline">사용자</span>
      <select
        className="rounded-lg border border-slate-300 bg-white px-2 py-1.5 text-sm font-medium outline-none focus:border-brand"
        value={userId}
        onChange={(e) => setUserId(e.target.value)}
      >
        {users.map((u) => (
          <option key={u.id} value={u.id}>
            {u.label}
          </option>
        ))}
      </select>
    </label>
  );
}
