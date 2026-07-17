import type { Metadata } from "next";
import { API_BASE } from "@/lib/api";
import { SITE_NAME, SITE_TAGLINE } from "@/lib/site";

type Props = {
  params: { token: string };
  children: React.ReactNode;
};

async function fetchPublicForm(token: string): Promise<{
  title?: string;
  description?: string;
} | null> {
  try {
    const res = await fetch(
      `${API_BASE}/pmsi/public/forms/${encodeURIComponent(token)}`,
      { next: { revalidate: 60 } }
    );
    if (!res.ok) return null;
    return (await res.json()) as { title?: string; description?: string };
  } catch {
    return null;
  }
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const form = await fetchPublicForm(params.token);
  const title = form?.title?.trim() || "공개 설문";
  const description =
    form?.description?.trim() ||
    `${title} — ${SITE_NAME}에서 응답하고 크레딧을 받아보세요. ${SITE_TAGLINE}`;

  return {
    title,
    description,
    openGraph: {
      title: `${title} | ${SITE_NAME}`,
      description,
      type: "website",
    },
    twitter: {
      card: "summary_large_image",
      title: `${title} | ${SITE_NAME}`,
      description,
    },
    robots: {
      index: true,
      follow: true,
    },
  };
}

export default function ShareTokenLayout({ children }: Props) {
  return children;
}
