import type { Metadata } from "next";
import { Noto_Sans_KR, Outfit } from "next/font/google";
import "./globals.css";
import Providers from "./providers";
import Header from "@/components/Header";
import OnboardingModal from "@/components/OnboardingModal";
import {
  SITE_DESCRIPTION,
  SITE_NAME,
  SITE_TAGLINE,
  SITE_URL,
} from "@/lib/site";

const outfit = Outfit({
  subsets: ["latin"],
  variable: "--font-display",
  display: "swap",
});

const noto = Noto_Sans_KR({
  subsets: ["latin"],
  weight: ["400", "500", "700", "900"],
  variable: "--font-sans",
  display: "swap",
});

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: {
    default: `${SITE_NAME} — 설문 품앗이 플랫폼`,
    template: `%s | ${SITE_NAME}`,
  },
  description: SITE_DESCRIPTION,
  keywords: [
    "품앗이폼",
    "PumasiForm",
    "설문 품앗이",
    "설문 응답자 모으기",
    "크레딧 설문",
    "상호 응답 설문",
    "설문조사 플랫폼",
    "구글폼 대안",
    "네이버폼 대안",
    "학술 설문",
    "논문 설문 모집",
  ],
  authors: [{ name: SITE_NAME }],
  creator: SITE_NAME,
  openGraph: {
    type: "website",
    locale: "ko_KR",
    url: SITE_URL,
    siteName: SITE_NAME,
    title: `${SITE_NAME} — 설문 품앗이 플랫폼`,
    description: SITE_TAGLINE,
  },
  twitter: {
    card: "summary_large_image",
    title: `${SITE_NAME} — 설문 품앗이 플랫폼`,
    description: SITE_TAGLINE,
  },
  alternates: {
    canonical: SITE_URL,
  },
  robots: {
    index: true,
    follow: true,
  },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="ko" className={`${outfit.variable} ${noto.variable}`}>
      <body className="font-sans">
        <Providers>
          <Header />
          {children}
          <OnboardingModal />
        </Providers>
      </body>
    </html>
  );
}
