package egovframework.pmsi.response.service.impl;

import egovframework.pmsi.form.service.QuestionVO;
import egovframework.pmsi.response.service.AnswerVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 답변 값 유효성 검증 순수 로직 테스트(무DB). */
class AnswerValidatorTest {

    private final AnswerValidator validator = new AnswerValidator();

    private QuestionVO question(String id, String type) {
        QuestionVO q = new QuestionVO();
        q.setQuestionId(id);
        q.setType(type);
        q.setTitle("문항-" + id);
        return q;
    }

    private AnswerVO answer(String questionId, String... values) {
        AnswerVO a = new AnswerVO();
        a.setQuestionId(questionId);
        a.setValues(List.of(values));
        return a;
    }

    @Test
    void validAnswers_noErrors() {
        QuestionVO radio = question("q1", "RADIO");
        radio.setOptions(List.of("빨강", "파랑"));
        QuestionVO scale = question("q2", "LINEAR_SCALE");
        scale.setScaleMin(1);
        scale.setScaleMax(5);
        QuestionVO text = question("q3", "SHORT_TEXT");

        List<String> errors = validator.validate(
                List.of(radio, scale, text),
                List.of(answer("q1", "파랑"), answer("q2", "4"), answer("q3", "좋아요")));
        assertTrue(errors.isEmpty());
    }

    @Test
    void unknownQuestionId_error() {
        List<String> errors = validator.validate(
                List.of(question("q1", "SHORT_TEXT")),
                List.of(answer("q-unknown", "값")));
        assertEquals(1, errors.size());
    }

    @Test
    void duplicateAnswerForSameQuestion_error() {
        QuestionVO q = question("q1", "SHORT_TEXT");
        List<String> errors = validator.validate(
                List.of(q), List.of(answer("q1", "a"), answer("q1", "b")));
        assertFalse(errors.isEmpty());
    }

    @Test
    void radio_valueNotInOptions_error() {
        QuestionVO radio = question("q1", "RADIO");
        radio.setOptions(List.of("빨강", "파랑"));
        List<String> errors = validator.validate(
                List.of(radio), List.of(answer("q1", "보라")));
        assertFalse(errors.isEmpty());
    }

    @Test
    void radio_multipleValues_error() {
        QuestionVO radio = question("q1", "RADIO");
        radio.setOptions(List.of("빨강", "파랑"));
        List<String> errors = validator.validate(
                List.of(radio), List.of(answer("q1", "빨강", "파랑")));
        assertFalse(errors.isEmpty());
    }

    @Test
    void checkbox_respectsMinMaxSelectAndMembership() {
        QuestionVO cb = question("q1", "CHECKBOX");
        cb.setOptions(List.of("a", "b", "c"));
        cb.setMinSelect(2);
        cb.setMaxSelect(2);

        assertTrue(validator.validate(List.of(cb), List.of(answer("q1", "a", "b"))).isEmpty());
        // 1개 선택 → minSelect 위반
        assertFalse(validator.validate(List.of(cb), List.of(answer("q1", "a"))).isEmpty());
        // 3개 선택 → maxSelect 위반
        assertFalse(validator.validate(List.of(cb), List.of(answer("q1", "a", "b", "c"))).isEmpty());
        // 보기에 없는 값
        assertFalse(validator.validate(List.of(cb), List.of(answer("q1", "a", "x"))).isEmpty());
        // 같은 보기 중복 선택
        assertFalse(validator.validate(List.of(cb), List.of(answer("q1", "a", "a"))).isEmpty());
    }

    @Test
    void text_lengthAndRegex() {
        QuestionVO text = question("q1", "SHORT_TEXT");
        text.setMinLength(2);
        text.setMaxLength(5);

        assertTrue(validator.validate(List.of(text), List.of(answer("q1", "안녕"))).isEmpty());
        assertFalse(validator.validate(List.of(text), List.of(answer("q1", "a"))).isEmpty());
        assertFalse(validator.validate(List.of(text), List.of(answer("q1", "여섯글자입니다"))).isEmpty());

        QuestionVO email = question("q2", "SHORT_TEXT");
        email.setRegex("^[^@\\s]+@[^@\\s]+$");
        assertTrue(validator.validate(List.of(email), List.of(answer("q2", "a@b.com"))).isEmpty());
        assertFalse(validator.validate(List.of(email), List.of(answer("q2", "not-an-email"))).isEmpty());
    }

    @Test
    void scale_integerWithinRange() {
        QuestionVO scale = question("q1", "LINEAR_SCALE");
        scale.setScaleMin(1);
        scale.setScaleMax(5);

        assertTrue(validator.validate(List.of(scale), List.of(answer("q1", "3"))).isEmpty());
        assertFalse(validator.validate(List.of(scale), List.of(answer("q1", "0"))).isEmpty());
        assertFalse(validator.validate(List.of(scale), List.of(answer("q1", "6"))).isEmpty());
        assertFalse(validator.validate(List.of(scale), List.of(answer("q1", "셋"))).isEmpty());
    }

    @Test
    void blankAnswer_skipped() {
        // 미응답(빈 값)은 여기서 검증하지 않는다 — 필수 여부는 호출측 책임
        QuestionVO text = question("q1", "SHORT_TEXT");
        text.setMinLength(2);
        List<String> errors = validator.validate(List.of(text), List.of(answer("q1", "")));
        assertTrue(errors.isEmpty());
    }
}
