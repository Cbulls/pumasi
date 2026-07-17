export const ONBOARDING_STORAGE_KEY = "pumasi.onboarding.v1";

export function hasSeenOnboarding(): boolean {
  if (typeof window === "undefined") return true;
  try {
    return window.localStorage.getItem(ONBOARDING_STORAGE_KEY) === "1";
  } catch {
    return true;
  }
}

export function markOnboardingSeen(): void {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.setItem(ONBOARDING_STORAGE_KEY, "1");
  } catch {
    // private mode 등 — 무시
  }
}

/** 온보딩 팝업을 띄울 경로 */
export function isOnboardingPath(pathname: string | null): boolean {
  return pathname === "/" || pathname === "/home";
}
