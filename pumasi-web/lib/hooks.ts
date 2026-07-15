"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";
import { useCurrentUser } from "@/context/CurrentUserContext";
import type {
  ChartItem,
  CreditBalance,
  FormVO,
  QuestionVO,
  ResponsesTable,
  SubmitRequest,
  SubmitResult,
} from "@/lib/types";

/** 내 폼 목록 */
export function useMyForms() {
  const { userId, token } = useCurrentUser();
  return useQuery({
    queryKey: ["forms", "mine", userId],
    queryFn: () =>
      apiFetch<FormVO[]>(`/pmsi/form?ownerId=${encodeURIComponent(userId!)}`, { token }),
    enabled: !!token && !!userId,
  });
}

/** 응답 피드(게시된 남의 설문) */
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

/** 인증된 본인 크레딧 잔액 */
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

/** 개별 응답 표(익명화, 소유자만) */
export function useResponsesTable(formId: string, enabled: boolean) {
  const { userId, token } = useCurrentUser();
  return useQuery({
    queryKey: ["responseTable", formId, userId],
    queryFn: () => apiFetch<ResponsesTable>(`/pmsi/form/${formId}/results/responses`, { token }),
    enabled: !!formId && !!token && enabled,
  });
}

// ── Mutations ──

export function useCreateForm() {
  const { userId, token } = useCurrentUser();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: { title: string; description?: string; maxResponses: number }) =>
      apiFetch<{ formId: string }>(`/pmsi/form`, { method: "POST", token, body }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["forms", "mine", userId] }),
  });
}

export function useAddQuestion(formId: string) {
  const { token } = useCurrentUser();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: Partial<QuestionVO>) =>
      apiFetch<void>(`/pmsi/form/${formId}/questions`, { method: "POST", token, body }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["questions", formId] }),
  });
}

export function usePublishForm(formId: string) {
  const { userId, token } = useCurrentUser();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => apiFetch<FormVO>(`/pmsi/form/${formId}/publish`, { method: "POST", token }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["form", formId] });
      qc.invalidateQueries({ queryKey: ["forms", "mine", userId] });
      qc.invalidateQueries({ queryKey: ["credit", userId] });
    },
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
    },
  });
}
