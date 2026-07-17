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

/** 데모 계정 스위처·자동 로그인 허용 */
export const DEMO_AUTH = process.env.NEXT_PUBLIC_DEMO_AUTH !== "false";

interface LoginResult {
  token: string;
  userId: string;
  expiresAt: string;
}

interface CurrentUserContextValue {
  userId: string | null;
  token: string | null;
  ready: boolean;
  users: DemoUser[];
  demoAuth: boolean;
  switchUser: (id: string) => Promise<void>;
  loginWithMagicToken: (token: string) => Promise<void>;
  requestMagicLink: (email: string, displayName?: string) => Promise<{
    email: string;
    expiresAt: string;
    echoedToken?: string;
    message: string;
  }>;
  logout: () => Promise<void>;
}

const CurrentUserContext = createContext<CurrentUserContextValue | null>(null);

const USER_KEY = "pumasi.userId";
const TOKEN_KEY = "pumasi.token";

export function CurrentUserProvider({ children }: { children: ReactNode }) {
  const [userId, setUserId] = useState<string | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [ready, setReady] = useState(false);

  const persist = useCallback((uid: string, tok: string) => {
    setUserId(uid);
    setToken(tok);
    window.localStorage.setItem(USER_KEY, uid);
    window.localStorage.setItem(TOKEN_KEY, tok);
  }, []);

  const clear = useCallback(() => {
    setUserId(null);
    setToken(null);
    window.localStorage.removeItem(USER_KEY);
    window.localStorage.removeItem(TOKEN_KEY);
  }, []);

  const switchUser = useCallback(
    async (id: string) => {
      const res = await apiFetch<LoginResult>("/pmsi/auth/login", {
        method: "POST",
        body: { userId: id },
      });
      persist(res.userId, res.token);
    },
    [persist]
  );

  const loginWithMagicToken = useCallback(
    async (magicToken: string) => {
      const res = await apiFetch<LoginResult>("/pmsi/auth/magic-link/verify", {
        method: "POST",
        body: { token: magicToken },
      });
      persist(res.userId, res.token);
    },
    [persist]
  );

  const requestMagicLink = useCallback(async (email: string, displayName?: string) => {
    return apiFetch<{
      email: string;
      expiresAt: string;
      echoedToken?: string;
      message: string;
    }>("/pmsi/auth/magic-link/request", {
      method: "POST",
      body: { email, displayName },
    });
  }, []);

  const logout = useCallback(async () => {
    const saved = window.localStorage.getItem(TOKEN_KEY);
    if (saved) {
      try {
        await apiFetch<void>("/pmsi/auth/logout", { method: "POST", token: saved });
      } catch {
        /* ignore */
      }
    }
    clear();
  }, [clear]);

  useEffect(() => {
    const savedUser = window.localStorage.getItem(USER_KEY);
    const savedToken = window.localStorage.getItem(TOKEN_KEY);
    const boot = async () => {
      if (savedUser && savedToken) {
        try {
          const me = await apiFetch<{ userId: string }>("/pmsi/auth/me", {
            token: savedToken,
          });
          setUserId(me.userId);
          setToken(savedToken);
          return;
        } catch {
          clear();
        }
      }
      if (DEMO_AUTH) {
        try {
          await switchUser(DEMO_USERS[0].id);
        } catch {
          clear();
        }
      }
    };
    void boot().finally(() => setReady(true));
  }, [switchUser, clear]);

  return (
    <CurrentUserContext.Provider
      value={{
        userId,
        token,
        ready,
        users: DEMO_USERS,
        demoAuth: DEMO_AUTH,
        switchUser,
        loginWithMagicToken,
        requestMagicLink,
        logout,
      }}
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
