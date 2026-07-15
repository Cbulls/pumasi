"use client";

import { useCurrentUser } from "@/context/CurrentUserContext";

/** 데모 계정 전환. 선택 시 해당 계정으로 로그인해 Bearer 토큰을 발급받는다. */
export default function UserSwitcher() {
  const { userId, users, switchUser, ready } = useCurrentUser();
  return (
    <label className="flex items-center gap-2 text-sm">
      <span className="hidden text-slate-500 sm:inline">계정</span>
      <select
        className="rounded-lg border border-slate-300 bg-white px-2 py-1.5 text-sm font-medium outline-none focus:border-brand disabled:opacity-50"
        value={userId ?? ""}
        disabled={!ready}
        onChange={(e) => void switchUser(e.target.value)}
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
