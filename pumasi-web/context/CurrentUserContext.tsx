"use client";

import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";

// 인증 모듈 미구현 → 데모 사용자(백엔드 seed 계정)를 스위처로 전환.
export interface DemoUser {
  id: string;
  label: string;
  role: "creator" | "respondent";
}

export const DEMO_USERS: DemoUser[] = [
  { id: "u-owner", label: "제작자 (u-owner)", role: "creator" },
  { id: "u-alice", label: "응답자 앨리스 (u-alice)", role: "respondent" },
  { id: "u-bob", label: "응답자 밥 (u-bob)", role: "respondent" },
];

interface CurrentUserContextValue {
  userId: string;
  setUserId: (id: string) => void;
  users: DemoUser[];
}

const CurrentUserContext = createContext<CurrentUserContextValue | null>(null);

const STORAGE_KEY = "pumasi.userId";

export function CurrentUserProvider({ children }: { children: ReactNode }) {
  const [userId, setUserIdState] = useState<string>(DEMO_USERS[0].id);

  // 최초 마운트 시 localStorage 복원(SSR 안전을 위해 effect 안에서)
  useEffect(() => {
    const saved = window.localStorage.getItem(STORAGE_KEY);
    if (saved) setUserIdState(saved);
  }, []);

  const setUserId = (id: string) => {
    setUserIdState(id);
    window.localStorage.setItem(STORAGE_KEY, id);
  };

  return (
    <CurrentUserContext.Provider value={{ userId, setUserId, users: DEMO_USERS }}>
      {children}
    </CurrentUserContext.Provider>
  );
}

export function useCurrentUser(): CurrentUserContextValue {
  const ctx = useContext(CurrentUserContext);
  if (!ctx) throw new Error("useCurrentUser must be used within CurrentUserProvider");
  return ctx;
}
