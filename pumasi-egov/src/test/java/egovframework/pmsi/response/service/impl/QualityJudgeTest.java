package egovframework.pmsi.response.service.impl;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 응답 품질 판정 순수 로직 테스트(무DB). */
class QualityJudgeTest {

    private final QualityJudge judge = new QualityJudge();

    @Test
    void normalResponse_pass() {
        QualityJudge.Flag flag = judge.judge(
                100, 3, List.of(0, 1, 2), List.of("좋은 의견입니다"), null);
        assertEquals(QualityJudge.Flag.PASS, flag);
    }

    @Test
    void tooFast_reject() {
        // 문항 3개 → 최소 6초. 1초 제출은 고속 어뷰징.
        QualityJudge.Flag flag = judge.judge(1, 3, List.of(0, 1, 2), List.of(), null);
        assertEquals(QualityJudge.Flag.REJECT, flag);
    }

    @Test
    void attentionCheckFailed_reject() {
        QualityJudge.Flag flag = judge.judge(100, 3, List.of(0, 1, 2), List.of(), Boolean.FALSE);
        assertEquals(QualityJudge.Flag.REJECT, flag);
    }

    @Test
    void straightLining_reject() {
        // 단일선택을 전부 같은 위치(1)로 → 불성실
        QualityJudge.Flag flag = judge.judge(100, 4, List.of(1, 1, 1, 1), List.of(), null);
        assertEquals(QualityJudge.Flag.REJECT, flag);
    }

    @Test
    void oneGarbageText_hold() {
        QualityJudge.Flag flag = judge.judge(100, 1, List.of(), List.of("aaaa"), null);
        assertEquals(QualityJudge.Flag.HOLD, flag);
    }

    @Test
    void twoGarbageTexts_reject() {
        QualityJudge.Flag flag = judge.judge(100, 2, List.of(), List.of("aaaa", "ㅁㅁㅁㅁ"), null);
        assertEquals(QualityJudge.Flag.REJECT, flag);
    }
}
