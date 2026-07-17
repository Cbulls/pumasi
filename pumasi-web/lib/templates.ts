import type { QuestionType } from "@/lib/types";

export type FormTemplate = {
  id: string;
  title: string;
  description: string;
  formTitle: string;
  formDescription: string;
  maxResponses: number;
  questions: Array<{
    type: QuestionType;
    title: string;
    required?: boolean;
    options?: string[];
    scaleMin?: number;
    scaleMax?: number;
    bodyHtml?: string;
  }>;
};

const UNLOCK_NOTE =
  "이 설문은 품앗이폼입니다. 상대가 내 설문에 답하면, 내가 상대 설문에 답해야 그 응답을 볼 수 있습니다.";

export const FORM_TEMPLATES: FormTemplate[] = [
  {
    id: "satisfaction",
    title: "만족도 조사",
    description: "별점·객관식으로 서비스 만족도를 묻습니다.",
    formTitle: "서비스 만족도 조사",
    formDescription: "소중한 의견을 남겨 주세요.",
    maxResponses: 50,
    questions: [
      { type: "DESCRIPTION", title: "품앗이 안내", bodyHtml: UNLOCK_NOTE, required: false },
      {
        type: "RATING",
        title: "전반적인 만족도는?",
        required: true,
        scaleMin: 1,
        scaleMax: 5,
      },
      {
        type: "RADIO",
        title: "추천하시겠습니까?",
        required: true,
        options: ["예", "아니오", "잘 모르겠음"],
      },
      { type: "LONG_TEXT", title: "개선이 필요한 점이 있다면?", required: false },
    ],
  },
  {
    id: "event",
    title: "행사 신청",
    description: "날짜·시간·참석 여부를 받는 신청 폼입니다.",
    formTitle: "행사 참가 신청",
    formDescription: "참가 정보를 입력해 주세요.",
    maxResponses: 100,
    questions: [
      { type: "DESCRIPTION", title: "품앗이 안내", bodyHtml: UNLOCK_NOTE, required: false },
      { type: "SHORT_TEXT", title: "이름(또는 닉네임)", required: true },
      { type: "DATE", title: "참석 희망일", required: true },
      { type: "TIME", title: "도착 예정 시각", required: false },
      {
        type: "RADIO",
        title: "참석 형태",
        required: true,
        options: ["현장", "온라인", "미정"],
      },
    ],
  },
  {
    id: "academic",
    title: "학술 기초설문",
    description: "연구용 기초 문항(드롭다운·척도·단답).",
    formTitle: "기초 설문 (학술)",
    formDescription: "연구 목적의 익명 설문입니다.",
    maxResponses: 200,
    questions: [
      { type: "DESCRIPTION", title: "품앗이 안내", bodyHtml: UNLOCK_NOTE, required: false },
      {
        type: "DROPDOWN",
        title: "연령대",
        required: true,
        options: ["10대", "20대", "30대", "40대", "50대 이상"],
      },
      {
        type: "LINEAR_SCALE",
        title: "주제에 대한 관심도",
        required: true,
        scaleMin: 1,
        scaleMax: 7,
      },
      {
        type: "CHECKBOX",
        title: "관련 경험이 있는 영역 (복수)",
        required: false,
        options: ["수업", "연구", "직장", "취미", "해당 없음"],
      },
      { type: "SHORT_TEXT", title: "한 줄 의견", required: false },
    ],
  },
];

export function getTemplate(id: string | null | undefined): FormTemplate | undefined {
  if (!id) return undefined;
  return FORM_TEMPLATES.find((t) => t.id === id);
}
