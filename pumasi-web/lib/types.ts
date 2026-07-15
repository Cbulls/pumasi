// 백엔드 VO에 대응하는 타입 정의 (pumasi-egov)

export type QuestionType =
  | "SHORT_TEXT"
  | "LONG_TEXT"
  | "RADIO"
  | "CHECKBOX"
  | "LINEAR_SCALE";

export type FormStatus = "DRAFT" | "ACTIVE" | "CLOSED";

export interface FormVO {
  formId: string;
  ownerId: string;
  title: string;
  description?: string;
  status: FormStatus;
  costCredits: number;
  maxResponses: number;
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
}

export interface AnswerVO {
  questionId: string;
  values: string[];
}

export interface SubmitRequest {
  elapsedSeconds: number;
  answers: AnswerVO[];
  attentionPassed?: boolean | null;
}

export interface SubmitResult {
  responseId: string;
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
