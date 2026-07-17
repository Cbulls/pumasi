import { API_BASE } from "@/lib/api";

/** 문항 이미지 기준 URL → variant URL (절대경로, API 호스트 포함) */
export function mediaVariantUrl(
  imageUrl: string | null | undefined,
  variant: "thumb" | "display" | "orig" = "display"
): string | null {
  if (!imageUrl) return null;
  const base = imageUrl.trim();
  if (base.startsWith("http://") || base.startsWith("https://")) {
    return base;
  }
  if (base.includes("/media/")) {
    const path = base.split("?")[0];
    return `${API_BASE}${path}?v=${variant}`;
  }
  // 레거시 상대 경로
  if (base.startsWith("/")) return `${API_BASE}${base}`;
  return base;
}
