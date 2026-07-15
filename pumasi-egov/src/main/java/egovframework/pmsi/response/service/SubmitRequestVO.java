package egovframework.pmsi.response.service;

import java.util.List;

/**
 * 응답 제출 요청 본문.
 *
 * formId는 경로에서, respondentId는 X-User-Id 헤더에서 주입되므로 본문엔 없다.
 */
public class SubmitRequestVO {
    private int elapsedSeconds;             // 응답 소요시간(초) — 어뷰징(고속제출) 판정
    private List<AnswerVO> answers;
    private Boolean attentionPassed;        // 주의 검증 문항 통과 여부(있을 때만)

    public int getElapsedSeconds() { return elapsedSeconds; }
    public void setElapsedSeconds(int elapsedSeconds) { this.elapsedSeconds = elapsedSeconds; }
    public List<AnswerVO> getAnswers() { return answers; }
    public void setAnswers(List<AnswerVO> answers) { this.answers = answers; }
    public Boolean getAttentionPassed() { return attentionPassed; }
    public void setAttentionPassed(Boolean attentionPassed) { this.attentionPassed = attentionPassed; }
}
