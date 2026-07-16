package egovframework.pmsi.response.service;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 응답 제출 요청 본문.
 *
 * formId는 경로에서, respondentId는 인증 주체에서 주입되므로 본문엔 없다.
 * 소요시간(elapsed)은 서버가 response_session.started_at 기준으로 계산한다
 * (클라이언트가 보낸 값을 신뢰하지 않는다).
 */
public class SubmitRequestVO {

    @Size(max = 500, message = "답변 항목이 너무 많습니다.")
    private List<AnswerVO> answers;
    private boolean consentAgreed;          // 개인정보 수집·이용 동의(필수)

    public List<AnswerVO> getAnswers() { return answers; }
    public void setAnswers(List<AnswerVO> answers) { this.answers = answers; }
    public boolean isConsentAgreed() { return consentAgreed; }
    public void setConsentAgreed(boolean consentAgreed) { this.consentAgreed = consentAgreed; }
}
