// 백엔드 VO에 대응하는 타입 정의 (pumasi-egov)

export type QuestionType =
  | "SHORT_TEXT"
  | "LONG_TEXT"
  | "RADIO"
  | "CHECKBOX"
  | "LINEAR_SCALE"
  | "DESCRIPTION"
  | "IMAGE"
  | "FILE";

export type FormStatus = "DRAFT" | "ACTIVE" | "CLOSED";

export interface FormVO {
  formId: string;
  ownerId: string;
  title: string;
  description?: string;
  status: FormStatus;
  costCredits: number;
  maxResponses: number;
  closesAt?: string | null;
  shareToken?: string | null;
}

export interface SectionVO {
  sectionId: string;
  formId: string;
  title: string;
  orderIndex: number;
  questions?: QuestionVO[];
}

export interface QuestionVO {
  questionId: string;
  formId: string;
  sectionId?: string;
  type: QuestionType;
  title: string;
  required: boolean;
  orderIndex: number;
  options?: string[];
  minSelect?: number | null;
  maxSelect?: number | null;
  minLength?: number | null;
  maxLength?: number | null;
  regex?: string | null;
  scaleMin?: number | null;
  scaleMax?: number | null;
  bodyHtml?: string | null;
  imageUrl?: string | null;
  branchRules?: Record<string, string> | null;
}

export interface AnswerVO {
  questionId: string;
  values: string[];
}

export interface SubmitRequest {
  answers: AnswerVO[];
  consentAgreed: boolean;
}

export interface SubmitResult {
  responseId: string;
  anonLabel: string;
  qualityFlag: "pass" | "hold" | "reject";
  rewardCredited: number;
}

export interface CreditBalance {
  userId: string;
  available: number;
  escrow: number;
  version: number;
}

export interface ChartItem {
  questionId: string;
  title: string;
  type: QuestionType;
  chartType: "pie" | "bar" | "histogram" | "text_list" | "unsupported";
  counts: Record<string, number>;
  ratios: Record<string, number>;
  respondentCount: number;
  average: number;
  median: number;
  textResponses: string[];
}

export interface ResponsesTable {
  questions: { questionId: string; title: string; type: QuestionType }[];
  rows: {
    anonLabel: string;
    qualityFlag: "pass" | "hold" | "reject";
    submittedAt: string;
    answers: Record<string, string>;
  }[];
}
