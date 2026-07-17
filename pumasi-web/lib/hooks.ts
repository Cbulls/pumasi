"use client";

import {
  useInfiniteQuery,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { apiFetch, API_BASE } from "@/lib/api";
import { useCurrentUser } from "@/context/CurrentUserContext";
import type {
  CreditBalance,
  FormVO,
  QuestionVO,
  ResponsesTable,
  ResultsPayload,
  SectionVO,
  SubmitRequest,
  SubmitResult,
} from "@/lib/types";

export function useMyForms() {
  const { userId, token } = useCurrentUser();
  return useQuery({
    queryKey: ["forms", "mine", userId],
    queryFn: () => apiFetch<FormVO[]>(`/pmsi/form`, { token }),
    enabled: !!token && !!userId,
  });
}

export const FEED_PAGE_SIZE = 20;

export type FeedFilters = {
  maxMinutes?: number | null;
  minReward?: number | null;
  reciprocalOnly?: boolean;
};

/** 페이지네이션 피드: "더 보기"로 다음 페이지 로드 + 실용 필터 */
export function useFeed(filters: FeedFilters = {}) {
  const { token } = useCurrentUser();
  const maxMinutes = filters.maxMinutes ?? undefined;
  const minReward = filters.minReward ?? undefined;
  const reciprocalOnly = !!filters.reciprocalOnly;
  return useInfiniteQuery({
    queryKey: ["feed", token, maxMinutes ?? null, minReward ?? null, reciprocalOnly],
    queryFn: ({ pageParam }) => {
      const q = new URLSearchParams({
        page: String(pageParam),
        size: String(FEED_PAGE_SIZE),
      });
      if (maxMinutes != null) q.set("maxMinutes", String(maxMinutes));
      if (minReward != null) q.set("minReward", String(minReward));
      if (reciprocalOnly) q.set("reciprocalOnly", "true");
      return apiFetch<FormVO[]>(`/pmsi/feed?${q}`, { token });
    },
    initialPageParam: 0,
    getNextPageParam: (lastPage, pages) =>
      lastPage.length < FEED_PAGE_SIZE ? undefined : pages.length,
    enabled: !!token,
  });
}

export function useUnlockOpportunities() {
  const { userId, token } = useCurrentUser();
  return useQuery({
    queryKey: ["unlockOpportunities", userId],
    queryFn: () =>
      apiFetch<{ count: number; items: Array<{ formId: string; title: string; costCredits: number }> }>(
        `/pmsi/me/unlock-opportunities`,
        { token }
      ),
    enabled: !!token,
  });
}

export function useMyActivity() {
  const { userId, token } = useCurrentUser();
  return useQuery({
    queryKey: ["myActivity", userId],
    queryFn: () =>
      apiFetch<{
        items: Array<{
          responseId: string;
          formId: string;
          formTitle: string;
          qualityFlag: string;
          anonLabel: string;
          submittedAt: string;
          costCredits: number;
        }>;
      }>(`/pmsi/me/responses`, { token }),
    enabled: !!token,
  });
}

export function useNotifications() {
  const { userId, token } = useCurrentUser();
  return useQuery({
    queryKey: ["notifications", userId],
    queryFn: () =>
      apiFetch<{
        unreadCount: number;
        items: Array<{
          id: string;
          type: string;
          title: string;
          body?: string;
          linkUrl?: string;
          readAt?: string | null;
          createdAt: string;
        }>;
      }>(`/pmsi/notifications?limit=30`, { token }),
    enabled: !!token,
    refetchInterval: 30_000,
  });
}

export function useMarkNotificationRead() {
  const { userId, token } = useCurrentUser();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) =>
      apiFetch<void>(`/pmsi/notifications/${id}/read`, { method: "POST", token }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["notifications", userId] }),
  });
}

export function useMarkAllNotificationsRead() {
  const { userId, token } = useCurrentUser();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => apiFetch<void>(`/pmsi/notifications/read-all`, { method: "POST", token }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["notifications", userId] }),
  });
}

export function useCreditLedger() {
  const { userId, token } = useCurrentUser();
  return useQuery({
    queryKey: ["creditLedger", userId],
    queryFn: () =>
      apiFetch<{
        items: Array<{
          id: number;
          delta: number;
          reason: string;
          refId: string;
          createdAt: string;
        }>;
      }>(`/pmsi/credit/ledger?limit=50`, { token }),
    enabled: !!token,
  });
}

export function useExportJob(formId: string) {
  const { token } = useCurrentUser();
  const qc = useQueryClient();
  const create = useMutation({
    mutationFn: () =>
      apiFetch<{ jobId: string }>(`/pmsi/form/${formId}/results/export-jobs`, {
        method: "POST",
        token,
      }),
  });
  const status = useQuery({
    queryKey: ["exportJob", formId, create.data?.jobId],
    queryFn: () =>
      apiFetch<{
        jobId: string;
        status: string;
        fileName?: string;
        error?: string;
      }>(`/pmsi/form/${formId}/results/export-jobs/${create.data!.jobId}`, { token }),
    enabled: !!token && !!create.data?.jobId,
    refetchInterval: (q) => {
      const s = q.state.data?.status;
      return s === "DONE" || s === "FAILED" ? false : 1500;
    },
  });
  return { create, status, token, qc };
}

export function useForm(formId: string) {
  const { userId, token } = useCurrentUser();
  return useQuery({
    queryKey: ["form", formId, userId],
    queryFn: () => apiFetch<FormVO>(`/pmsi/form/${formId}`, { token }),
    enabled: !!formId && !!token,
  });
}

export function useQuestions(formId: string) {
  const { userId, token } = useCurrentUser();
  return useQuery({
    queryKey: ["questions", formId, userId],
    queryFn: () => apiFetch<QuestionVO[]>(`/pmsi/form/${formId}/questions`, { token }),
    enabled: !!formId && !!token,
  });
}

export function useSections(formId: string) {
  const { userId, token } = useCurrentUser();
  return useQuery({
    queryKey: ["sections", formId, userId],
    queryFn: () => apiFetch<SectionVO[]>(`/pmsi/form/${formId}/sections`, { token }),
    enabled: !!formId && !!token,
  });
}

export function usePublicForm(shareToken: string) {
  return useQuery({
    queryKey: ["publicForm", shareToken],
    queryFn: () =>
      apiFetch<{
        formId: string;
        title: string;
        description?: string;
        status: string;
        costCredits: number;
        maxResponses: number;
        closesAt?: string | null;
        shareToken: string;
        sections: SectionVO[];
        questions: QuestionVO[];
      }>(`/pmsi/public/forms/${shareToken}`),
    enabled: !!shareToken,
  });
}

export function useCredit() {
  const { userId, token } = useCurrentUser();
  return useQuery({
    queryKey: ["credit", userId],
    queryFn: () => apiFetch<CreditBalance>(`/pmsi/credit/me`, { token }),
    enabled: !!token,
  });
}

export function useResults(formId: string) {
  const { userId, token } = useCurrentUser();
  return useQuery({
    queryKey: ["results", formId, userId],
    queryFn: () => apiFetch<ResultsPayload>(`/pmsi/form/${formId}/results`, { token }),
    enabled: !!formId && !!token,
  });
}

export function useResponsesTable(formId: string, enabled: boolean) {
  const { userId, token } = useCurrentUser();
  return useQuery({
    queryKey: ["responseTable", formId, userId],
    queryFn: () => apiFetch<ResponsesTable>(`/pmsi/form/${formId}/results/responses`, { token }),
    enabled: !!formId && !!token && enabled,
  });
}

function invalidateForm(qc: ReturnType<typeof useQueryClient>, formId: string, userId: string | null) {
  qc.invalidateQueries({ queryKey: ["form", formId] });
  qc.invalidateQueries({ queryKey: ["questions", formId] });
  qc.invalidateQueries({ queryKey: ["sections", formId] });
  qc.invalidateQueries({ queryKey: ["forms", "mine", userId] });
}

export function useCreateForm() {
  const { userId, token } = useCurrentUser();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: {
      title: string;
      description?: string;
      maxResponses: number;
      closesAt?: string | null;
    }) => apiFetch<{ formId: string }>(`/pmsi/form`, { method: "POST", token, body }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["forms", "mine", userId] }),
  });
}

export function useUpdateForm(formId: string) {
  const { userId, token } = useCurrentUser();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: Partial<FormVO>) =>
      apiFetch<FormVO>(`/pmsi/form/${formId}`, { method: "PUT", token, body }),
    onSuccess: () => invalidateForm(qc, formId, userId),
  });
}

export function useAddQuestion(formId: string) {
  const { userId, token } = useCurrentUser();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: Partial<QuestionVO>) =>
      apiFetch<void>(`/pmsi/form/${formId}/questions`, { method: "POST", token, body }),
    onSuccess: () => invalidateForm(qc, formId, userId),
  });
}

export function useUpdateQuestion(formId: string) {
  const { userId, token } = useCurrentUser();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ questionId, body }: { questionId: string; body: Partial<QuestionVO> }) =>
      apiFetch<void>(`/pmsi/form/${formId}/questions/${questionId}`, {
        method: "PUT",
        token,
        body,
      }),
    onSuccess: () => invalidateForm(qc, formId, userId),
  });
}

export function useDeleteQuestion(formId: string) {
  const { userId, token } = useCurrentUser();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (questionId: string) =>
      apiFetch<void>(`/pmsi/form/${formId}/questions/${questionId}`, {
        method: "DELETE",
        token,
      }),
    onSuccess: () => invalidateForm(qc, formId, userId),
  });
}

export function useReorderQuestions(formId: string) {
  const { userId, token } = useCurrentUser();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (questionIds: string[]) =>
      apiFetch<void>(`/pmsi/form/${formId}/questions/reorder`, {
        method: "PUT",
        token,
        body: { questionIds },
      }),
    onSuccess: () => invalidateForm(qc, formId, userId),
  });
}

export function useAddSection(formId: string) {
  const { userId, token } = useCurrentUser();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (title: string) =>
      apiFetch<SectionVO>(`/pmsi/form/${formId}/sections`, {
        method: "POST",
        token,
        body: { title },
      }),
    onSuccess: () => invalidateForm(qc, formId, userId),
  });
}

export function useUpdateSection(formId: string) {
  const { userId, token } = useCurrentUser();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ sectionId, title }: { sectionId: string; title: string }) =>
      apiFetch<SectionVO>(`/pmsi/form/${formId}/sections/${sectionId}`, {
        method: "PUT",
        token,
        body: { title },
      }),
    onSuccess: () => invalidateForm(qc, formId, userId),
  });
}

export function useDeleteSection(formId: string) {
  const { userId, token } = useCurrentUser();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (sectionId: string) =>
      apiFetch<void>(`/pmsi/form/${formId}/sections/${sectionId}`, {
        method: "DELETE",
        token,
      }),
    onSuccess: () => invalidateForm(qc, formId, userId),
  });
}

export function usePublishForm(formId: string) {
  const { userId, token } = useCurrentUser();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => apiFetch<FormVO>(`/pmsi/form/${formId}/publish`, { method: "POST", token }),
    onSuccess: () => {
      invalidateForm(qc, formId, userId);
      qc.invalidateQueries({ queryKey: ["credit", userId] });
    },
  });
}

export function useCloseForm(formId: string) {
  const { userId, token } = useCurrentUser();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => apiFetch<FormVO>(`/pmsi/form/${formId}/close`, { method: "POST", token }),
    onSuccess: () => {
      invalidateForm(qc, formId, userId);
      qc.invalidateQueries({ queryKey: ["credit", userId] });
    },
  });
}

/** 가드레일 PAUSED 해제(소유자) */
export function useResumeForm(formId: string) {
  const { userId, token } = useCurrentUser();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => apiFetch<FormVO>(`/pmsi/form/${formId}/resume`, { method: "POST", token }),
    onSuccess: () => invalidateForm(qc, formId, userId),
  });
}

/** 크레딧 충전(베타 Fake 결제). refId는 클라이언트 멱등 키 */
export function usePurchaseCredits() {
  const { userId, token } = useCurrentUser();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (amount: number) =>
      apiFetch<CreditBalance>(`/pmsi/credit/purchase`, {
        method: "POST",
        token,
        body: { amount, refId: crypto.randomUUID() },
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["credit", userId] }),
  });
}

/** HOLD 응답 검토(소유자): pass=소급 정산 / reject */
export function useReviewResponse(formId: string) {
  const { userId, token } = useCurrentUser();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ responseId, decision }: { responseId: string; decision: "pass" | "reject" }) =>
      apiFetch<void>(`/pmsi/form/${formId}/responses/${responseId}/review`, {
        method: "POST",
        token,
        body: { decision },
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["responseTable", formId] });
      qc.invalidateQueries({ queryKey: ["results", formId] });
      qc.invalidateQueries({ queryKey: ["credit", userId] });
    },
  });
}

/** 응답 퍼널 이벤트(view/start/submit) — 실패해도 UX를 막지 않는다 */
export function recordEvent(token: string | null, formId: string, eventType: "view" | "start" | "submit") {
  apiFetch<void>(`/pmsi/events`, { method: "POST", token, body: { formId, eventType } }).catch(
    () => undefined
  );
}

/** 소유자용 퍼널 집계 */
export function useFunnel(formId: string, enabled: boolean) {
  const { userId, token } = useCurrentUser();
  return useQuery({
    queryKey: ["funnel", formId, userId],
    queryFn: () =>
      apiFetch<{
        viewCount: number;
        startCount: number;
        submitCount: number;
        completionRate: number;
      }>(`/pmsi/form/${formId}/events/funnel`, { token }),
    enabled: !!formId && !!token && enabled,
  });
}

export function useStartResponse(formId: string) {
  const { token } = useCurrentUser();
  return useMutation({
    mutationFn: () =>
      apiFetch<void>(`/pmsi/form/${formId}/responses/start`, { method: "POST", token }),
  });
}

export function useSubmitResponse(formId: string) {
  const { userId, token } = useCurrentUser();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: SubmitRequest) =>
      apiFetch<SubmitResult>(`/pmsi/form/${formId}/responses`, { method: "POST", token, body }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["credit", userId] });
      qc.invalidateQueries({ queryKey: ["feed"] });
      qc.invalidateQueries({ queryKey: ["notifications", userId] });
      qc.invalidateQueries({ queryKey: ["unlockOpportunities", userId] });
      qc.invalidateQueries({ queryKey: ["myActivity", userId] });
    },
  });
}

export async function uploadFormMedia(
  formId: string,
  token: string | null,
  file: File
): Promise<{
  assetId: string;
  url: string;
  thumbUrl: string;
  displayUrl: string;
  origUrl: string;
  width: number;
  height: number;
}> {
  const fd = new FormData();
  fd.append("file", file);
  const headers: Record<string, string> = {};
  if (token) headers["Authorization"] = `Bearer ${token}`;
  const res = await fetch(`${API_BASE}/pmsi/form/${formId}/media`, {
    method: "POST",
    headers,
    body: fd,
  });
  const text = await res.text();
  let data: any = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = null;
  }
  if (!res.ok) {
    throw new Error((data && data.message) || `이미지 업로드 실패 (${res.status})`);
  }
  return data;
}

export async function uploadFormFile(
  formId: string,
  token: string | null,
  file: File
): Promise<{ fileId: string; fileName: string; url: string }> {
  const fd = new FormData();
  fd.append("file", file);
  const headers: Record<string, string> = {};
  if (token) headers["Authorization"] = `Bearer ${token}`;
  const res = await fetch(`${API_BASE}/pmsi/form/${formId}/files`, {
    method: "POST",
    headers,
    body: fd,
  });
  const text = await res.text();
  let data: any = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = null;
  }
  if (!res.ok) {
    throw new Error((data && data.message) || `업로드 실패 (${res.status})`);
  }
  return data;
}

export function serverCsvUrl(formId: string) {
  return `${API_BASE}/pmsi/form/${formId}/results/export.csv`;
}

export function exportJobDownloadUrl(formId: string, jobId: string) {
  return `${API_BASE}/pmsi/form/${formId}/results/export-jobs/${jobId}/download`;
}
