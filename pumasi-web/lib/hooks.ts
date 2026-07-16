"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiFetch, API_BASE } from "@/lib/api";
import { useCurrentUser } from "@/context/CurrentUserContext";
import type {
  ChartItem,
  CreditBalance,
  FormVO,
  QuestionVO,
  ResponsesTable,
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

export function useFeed() {
  const { token } = useCurrentUser();
  return useQuery({
    queryKey: ["feed", token],
    queryFn: () => apiFetch<FormVO[]>(`/pmsi/feed`, { token }),
    enabled: !!token,
  });
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
    queryFn: () => apiFetch<ChartItem[]>(`/pmsi/form/${formId}/results`, { token }),
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
    },
  });
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
