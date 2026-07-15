"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";
import { useCurrentUser } from "@/context/CurrentUserContext";
import type {
  ChartItem,
  CreditBalance,
  FormVO,
  QuestionVO,
  SubmitRequest,
  SubmitResult,
} from "@/lib/types";

/** 내 폼 목록 */
export function useMyForms() {
  const { userId } = useCurrentUser();
  return useQuery({
    queryKey: ["forms", "mine", userId],
    queryFn: () => apiFetch<FormVO[]>(`/pmsi/form?ownerId=${encodeURIComponent(userId)}`, { userId }),
  });
}

/** 응답 피드(게시된 남의 설문) */
export function useFeed() {
  const { userId } = useCurrentUser();
  return useQuery({
    queryKey: ["feed", userId],
    queryFn: () => apiFetch<FormVO[]>(`/pmsi/feed`, { userId }),
  });
}

export function useForm(formId: string) {
  const { userId } = useCurrentUser();
  return useQuery({
    queryKey: ["form", formId, userId],
    queryFn: () => apiFetch<FormVO>(`/pmsi/form/${formId}`, { userId }),
    enabled: !!formId,
  });
}

export function useQuestions(formId: string) {
  const { userId } = useCurrentUser();
  return useQuery({
    queryKey: ["questions", formId],
    queryFn: () => apiFetch<QuestionVO[]>(`/pmsi/form/${formId}/questions`, { userId }),
    enabled: !!formId,
  });
}

export function useCredit(targetUserId?: string) {
  const { userId } = useCurrentUser();
  const uid = targetUserId ?? userId;
  return useQuery({
    queryKey: ["credit", uid],
    queryFn: () => apiFetch<CreditBalance>(`/pmsi/credit/${uid}`, { userId }),
    enabled: !!uid,
  });
}

export function useResults(formId: string) {
  const { userId } = useCurrentUser();
  return useQuery({
    queryKey: ["results", formId, userId],
    queryFn: () => apiFetch<ChartItem[]>(`/pmsi/form/${formId}/results`, { userId }),
    enabled: !!formId,
  });
}

// ── Mutations ──

export function useCreateForm() {
  const { userId } = useCurrentUser();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: { title: string; description?: string; maxResponses: number }) =>
      apiFetch<{ formId: string }>(`/pmsi/form`, { method: "POST", userId, body }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["forms", "mine", userId] }),
  });
}

export function useAddQuestion(formId: string) {
  const { userId } = useCurrentUser();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: Partial<QuestionVO>) =>
      apiFetch<void>(`/pmsi/form/${formId}/questions`, { method: "POST", userId, body }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["questions", formId] }),
  });
}

export function usePublishForm(formId: string) {
  const { userId } = useCurrentUser();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => apiFetch<FormVO>(`/pmsi/form/${formId}/publish`, { method: "POST", userId }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["form", formId, userId] });
      qc.invalidateQueries({ queryKey: ["forms", "mine", userId] });
      qc.invalidateQueries({ queryKey: ["credit", userId] });
    },
  });
}

export function useSubmitResponse(formId: string) {
  const { userId } = useCurrentUser();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: SubmitRequest) =>
      apiFetch<SubmitResult>(`/pmsi/form/${formId}/responses`, { method: "POST", userId, body }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["credit", userId] });
      qc.invalidateQueries({ queryKey: ["feed", userId] });
    },
  });
}
