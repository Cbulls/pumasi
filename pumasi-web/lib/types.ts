// 백엔드 VO에 대응하는 타입 정의 (pumasi-egov)

export type QuestionType =
  | "SHORT_TEXT"
  | "LONG_TEXT"
  | "RADIO"
  | "CHECKBOX"
  | "DROPDOWN"
  | "LINEAR_SCALE"
  | "RATING"
  | "DATE"
  | "DESCRIPTION"
  | "IMAGE"
  | "FILE";

export type FormStatus = "DRAFT" | "ACTIVE" | "PAUSED" | "CLOSED";

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
  /** RADIO 전용 주의 문항 정답. 응답이 다르면 품질 reject */
  attentionAnswer?: string | null;
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
  sectionId?: string | null;
  chartType: "pie" | "bar" | "histogram" | "text_list" | "text_freq" | "file_list" | "unsupported";
  counts: Record<string, number>;
  ratios: Record<string, number>;
  respondentCount: number;
  average: number;
  median: number;
  textResponses: string[];
  ratioSumMayExceed100?: boolean;
}

export interface ResultsSummary {
  totalResponses: number;
  unlockedPassCount: number;
  lockedCount: number;
  passCount: number;
  holdCount: number;
  rejectCount: number;
  unlockedCount: number;
  unlockRate: number;
}

export interface ResultsPayload {
  summary: ResultsSummary;
  items: ChartItem[];
}

export interface ResponsesTable {
  questions: { questionId: string; title: string; type: QuestionType }[];
  rows: {
    responseId?: string;
    anonLabel: string;
    qualityFlag: "pass" | "hold" | "reject";
    submittedAt: string;
    answers: Record<string, string>;
    unlocked?: boolean;
    unlockHint?: string;
    unlockFormId?: string;
    unlockFormTitle?: string;
    unlockShareToken?: string | null;
  }[];
  unlockedCount?: number;
  lockedCount?: number;
  reciprocityRule?: string;
}
