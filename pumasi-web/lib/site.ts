/** 공개 사이트 URL (canonical / OG / sitemap). 배포 시 NEXT_PUBLIC_SITE_URL 설정. */
export const SITE_URL =
  process.env.NEXT_PUBLIC_SITE_URL?.replace(/\/$/, "") || "https://pumasi.kr";

export const SITE_NAME = "품앗이폼";
export const SITE_NAME_EN = "PumasiForm";

export const SITE_TAGLINE =
  "상대가 내 설문에 답해 주면, 그 사람의 설문에 답해야 그 응답을 볼 수 있습니다.";

export const SITE_DESCRIPTION =
  "품앗이폼은 1:1 상호 응답 설문 플랫폼입니다. A가 B의 설문에 응답하면 B는 A의 설문에 응답해야 A의 답을 볼 수 있습니다. 크레딧으로 설문을 게시하고 응답자를 모으세요.";
