"use client";

import Link from "next/link";
import { usePublicForm } from "@/lib/hooks";
import { useCurrentUser } from "@/context/CurrentUserContext";

export default function SharePreviewPage({ params }: { params: { token: string } }) {
  const { data, isLoading, isError, error } = usePublicForm(params.token);
  const { ready, token } = useCurrentUser();

  if (isLoading) return <p className="text-slate-500">불러오는 중…</p>;
  if (isError) {
    return (
      <div className="card mx-auto max-w-lg text-center">
        <p className="text-red-600">{(error as Error).message}</p>
        <Link href="/feed" className="btn-primary mt-4 inline-block">
          피드로
        </Link>
      </div>
    );
  }
  if (!data) return null;

  return (
    <div className="mx-auto max-w-xl space-y-5">
      <div className="card space-y-3">
        <p className="text-xs font-semibold text-brand">공개 미리보기</p>
        <h1 className="text-2xl font-extrabold">{data.title}</h1>
        {data.description && <p className="text-sm text-slate-500">{data.description}</p>}
        <ul className="text-sm text-slate-600 space-y-1">
          <li>예상 보상: 응답 완료 시 크레딧 지급</li>
          <li>최대 {data.maxResponses}명 · 비용 {data.costCredits}크레딧/응답</li>
          {data.closesAt && (
            <li>마감: {new Date(data.closesAt).toLocaleString("ko-KR")}</li>
          )}
          <li>섹션 {data.sections?.length ?? 0}개 · 문항 {data.questions?.length ?? 0}개</li>
        </ul>
        {!ready || !token ? (
          <p className="text-sm text-amber-700">로그인 후 응답할 수 있습니다. 상단에서 계정을 선택하세요.</p>
        ) : (
          <Link href={`/forms/${data.formId}/respond`} className="btn-primary inline-block">
            응답 시작하기
          </Link>
        )}
      </div>
    </div>
  );
}
