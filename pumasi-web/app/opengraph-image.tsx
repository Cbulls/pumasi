import { ImageResponse } from "next/og";

export const runtime = "edge";
export const alt = "품앗이폼 — 설문 품앗이 플랫폼";
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

export default function OpenGraphImage() {
  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          flexDirection: "column",
          justifyContent: "center",
          padding: "72px",
          background: "linear-gradient(145deg, #0f172a 0%, #1e293b 55%, #0c4a6e 100%)",
          color: "#f8fafc",
          fontFamily: "sans-serif",
        }}
      >
        <div style={{ fontSize: 36, color: "#7dd3fc", fontWeight: 700, letterSpacing: 2 }}>
          PUMASIFORM
        </div>
        <div style={{ marginTop: 20, fontSize: 84, fontWeight: 800, lineHeight: 1.1 }}>
          품앗이폼
        </div>
        <div style={{ marginTop: 28, fontSize: 34, color: "#cbd5e1", maxWidth: 900, lineHeight: 1.4 }}>
          응답해서 벌고, 벌어서 응답자를 모은다
        </div>
      </div>
    ),
    { ...size }
  );
}
