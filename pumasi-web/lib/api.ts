// 백엔드 REST 호출 래퍼. 모든 요청에 X-User-Id(인증 스텁) 헤더를 붙인다.

export const API_BASE =
  process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";

export class ApiError extends Error {
  status: number;
  code?: string;
  constructor(status: number, message: string, code?: string) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

interface FetchOpts {
  method?: "GET" | "POST" | "PUT" | "DELETE";
  userId?: string;
  body?: unknown;
}

export async function apiFetch<T>(path: string, opts: FetchOpts = {}): Promise<T> {
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (opts.userId) headers["X-User-Id"] = opts.userId;

  const res = await fetch(`${API_BASE}${path}`, {
    method: opts.method ?? "GET",
    headers,
    body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined,
    cache: "no-store",
  });

  const text = await res.text();
  const data = text ? JSON.parse(text) : null;

  if (!res.ok) {
    const message = (data && (data.message as string)) || `요청 실패 (${res.status})`;
    const code = data ? (data.code as string | undefined) : undefined;
    throw new ApiError(res.status, message, code);
  }
  return data as T;
}
