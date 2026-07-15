"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import { apiFetch } from "@/lib/api";

// 데모 계정(백엔드 user_account seed). 비밀번호 없이 계정 선택 로그인.
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

interface LoginResult {
  token: string;
  userId: string;
  expiresAt: string;
}

interface CurrentUserContextValue {
  userId: string | null;
  token: string | null;
  ready: boolean; // 로그인(토큰 확보) 완료 여부
  users: DemoUser[];
  switchUser: (id: string) => Promise<void>;
}

const CurrentUserContext = createContext<CurrentUserContextValue | null>(null);

const USER_KEY = "pumasi.userId";
const TOKEN_KEY = "pumasi.token";

export function CurrentUserProvider({ children }: { children: ReactNode }) {
  const [userId, setUserId] = useState<string | null>(null);
  const [token, setToken] = useState<string | null>(null);

  const switchUser = useCallback(async (id: string) => {
    // 계정 선택 = 해당 계정으로 로그인해 토큰 발급
    const res = await apiFetch<LoginResult>("/pmsi/auth/login", {
      method: "POST",
      body: { userId: id },
    });
    setUserId(res.userId);
    setToken(res.token);
    window.localStorage.setItem(USER_KEY, res.userId);
    window.localStorage.setItem(TOKEN_KEY, res.token);
  }, []);

  // 최초 진입: 저장된 토큰 복원, 없으면 기본 계정으로 로그인
  useEffect(() => {
    const savedUser = window.localStorage.getItem(USER_KEY);
    const savedToken = window.localStorage.getItem(TOKEN_KEY);
    if (savedUser && savedToken) {
      setUserId(savedUser);
      setToken(savedToken);
    } else {
      void switchUser(DEMO_USERS[0].id);
    }
  }, [switchUser]);

  return (
    <CurrentUserContext.Provider
      value={{ userId, token, ready: !!token, users: DEMO_USERS, switchUser }}
    >
      {children}
    </CurrentUserContext.Provider>
  );
}

export function useCurrentUser(): CurrentUserContextValue {
  const ctx = useContext(CurrentUserContext);
  if (!ctx) throw new Error("useCurrentUser must be used within CurrentUserProvider");
  return ctx;
}
