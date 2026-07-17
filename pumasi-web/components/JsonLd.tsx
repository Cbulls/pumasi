type JsonLdProps = {
  data: Record<string, unknown> | Record<string, unknown>[];
};

/** 서버 컴포넌트용 JSON-LD 스크립트 */
export default function JsonLd({ data }: JsonLdProps) {
  return (
    <script
      type="application/ld+json"
      dangerouslySetInnerHTML={{ __html: JSON.stringify(data) }}
    />
  );
}
