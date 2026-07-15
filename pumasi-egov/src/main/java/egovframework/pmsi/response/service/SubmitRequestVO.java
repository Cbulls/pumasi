package egovframework.pmsi.response.service;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 응답 제출 요청 본문.
 *
 * formId는 경로에서, respondentId는 인증 주체에서 주입되므로 본문엔 없다.
 */
public class SubmitRequestVO {
    @Min(value = 0, message = "소요시간은 0 이상이어야 합니다.")
    @Max(value = 86_400, message = "소요시간 값이 비정상적으로 큽니다.")
    private int elapsedSeconds;             // 응답 소요시간(초) — 어뷰징(고속제출) 판정

    @Size(max = 500, message = "답변 항목이 너무 많습니다.")
    private List<AnswerVO> answers;
    private Boolean attentionPassed;        // 주의 검증 문항 통과 여부(있을 때만)
    private boolean consentAgreed;          // 개인정보 수집·이용 동의(필수)

    public int getElapsedSeconds() { return elapsedSeconds; }
    public void setElapsedSeconds(int elapsedSeconds) { this.elapsedSeconds = elapsedSeconds; }
    public List<AnswerVO> getAnswers() { return answers; }
    public void setAnswers(List<AnswerVO> answers) { this.answers = answers; }
    public Boolean getAttentionPassed() { return attentionPassed; }
    public void setAttentionPassed(Boolean attentionPassed) { this.attentionPassed = attentionPassed; }
    public boolean isConsentAgreed() { return consentAgreed; }
    public void setConsentAgreed(boolean consentAgreed) { this.consentAgreed = consentAgreed; }
}
