"use client";

import { useEffect, useState } from "react";

/** 클라이언트에서만 QR 생성(외부 API 없음). qrcode 패키지 사용. */
export default function ShareQr({ url, size = 160 }: { url: string; size?: number }) {
  const [dataUrl, setDataUrl] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const QR = await import("qrcode");
        const png = await QR.toDataURL(url, {
          width: size,
          margin: 1,
          errorCorrectionLevel: "M",
        });
        if (!cancelled) setDataUrl(png);
      } catch {
        if (!cancelled) setError("QR을 만들 수 없습니다.");
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [url, size]);

  if (error) return <p className="text-xs text-red-600">{error}</p>;
  if (!dataUrl) return <p className="text-xs text-slate-400">QR 생성 중…</p>;
  return (
    <img
      src={dataUrl}
      alt="공유 QR 코드"
      width={size}
      height={size}
      className="rounded-lg border border-slate-200 bg-white p-1"
    />
  );
}
