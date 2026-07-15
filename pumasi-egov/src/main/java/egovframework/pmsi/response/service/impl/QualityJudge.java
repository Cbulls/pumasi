package egovframework.pmsi.response.service.impl;

import java.util.List;

/**
 * 응답 품질 판정 — 순수 로직(프레임워크 무관, 결정론적).
 *
 * 마스터 설계문서 §4.4: "받느냐(유효성)"와 "보상하느냐(어뷰징)"는 다른 질문이다.
 * 이 판정기는 후자(어뷰징)만 다룬다. 유효성(폼 규칙)은 이미 통과했다고 가정.
 *
 * 판정:
 *  - REJECT: 주의문항 실패 / 비정상 고속 제출 / straight-lining / 다수 쓰레기 텍스트
 *  - HOLD  : 쓰레기 텍스트 1건(경계 — 검토 여지)
 *  - PASS  : 그 외
 *
 * reject라도 데이터는 저장하되 크레딧만 안 준다(호출측 책임).
 */
public class QualityJudge {

    public static final int MIN_SECONDS_PER_QUESTION = 2;
    private static final int STRAIGHT_LINE_MIN = 3;   // 단일선택 문항이 이 이상일 때만 판정
    private static final int GARBAGE_MIN_LEN = 4;

    public enum Flag {
        PASS("pass"), HOLD("hold"), REJECT("reject");
        private final String value;
        Flag(String value) { this.value = value; }
        public String value() { return value; }
    }

    /**
     * @param elapsedSeconds        응답 소요시간(초)
     * @param questionCount         문항 수
     * @param singleChoicePositions 단일선택 문항에서 선택한 보기 위치(0-based) 목록
     * @param textValues            텍스트형 답변 목록
     * @param attentionPassed       주의 검증 문항 통과 여부(없으면 null)
     */
    public Flag judge(int elapsedSeconds, int questionCount,
                      List<Integer> singleChoicePositions, List<String> textValues,
                      Boolean attentionPassed) {

        // 1) 주의 검증 문항 실패 → 즉시 reject
        if (Boolean.FALSE.equals(attentionPassed)) {
            return Flag.REJECT;
        }

        // 2) 비정상 고속 제출 → reject
        int minSeconds = questionCount * MIN_SECONDS_PER_QUESTION;
        if (elapsedSeconds < minSeconds) {
            return Flag.REJECT;
        }

        // 3) straight-lining (단일선택을 전부 같은 위치로) → reject
        if (singleChoicePositions != null && singleChoicePositions.size() >= STRAIGHT_LINE_MIN) {
            int first = singleChoicePositions.get(0);
            boolean allSame = singleChoicePositions.stream().allMatch(p -> p == first);
            if (allSame) {
                return Flag.REJECT;
            }
        }

        // 4) 쓰레기 텍스트(같은 문자 반복) 개수로 hold/reject
        int garbage = 0;
        if (textValues != null) {
            for (String t : textValues) {
                if (isGarbage(t)) garbage++;
            }
        }
        if (garbage >= 2) return Flag.REJECT;
        if (garbage == 1) return Flag.HOLD;

        return Flag.PASS;
    }

    /** "aaaa", "ㅁㅁㅁㅁ" 처럼 같은 문자만 GARBAGE_MIN_LEN 이상 반복 */
    private boolean isGarbage(String text) {
        if (text == null) return false;
        String t = text.trim();
        if (t.length() < GARBAGE_MIN_LEN) return false;
        char c0 = t.charAt(0);
        for (int i = 1; i < t.length(); i++) {
            if (t.charAt(i) != c0) return false;
        }
        return true;
    }
}
