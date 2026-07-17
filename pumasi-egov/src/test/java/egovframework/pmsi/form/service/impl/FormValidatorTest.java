package egovframework.pmsi.form.service.impl;

import egovframework.pmsi.form.service.QuestionVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 질문 정의 검증 — 신규 유형(DROPDOWN/RATING/DATE)·주의 문항 규칙(무DB). */
class FormValidatorTest {

    private final FormValidator validator = new FormValidator();

    private QuestionVO q(String type) {
        QuestionVO v = new QuestionVO();
        v.setType(type);
        v.setTitle("제목");
        return v;
    }

    @Test
    void dropdown_requiresTwoOptions() {
        QuestionVO one = q("DROPDOWN");
        one.setOptions(List.of("하나"));
        assertFalse(validator.validate(one).isEmpty());

        QuestionVO ok = q("DROPDOWN");
        ok.setOptions(List.of("하나", "둘"));
        assertTrue(validator.validate(ok).isEmpty());
    }

    @Test
    void rating_requiresScaleRange() {
        QuestionVO missing = q("RATING");
        assertFalse(validator.validate(missing).isEmpty());

        QuestionVO ok = q("RATING");
        ok.setScaleMin(1);
        ok.setScaleMax(5);
        assertTrue(validator.validate(ok).isEmpty());
    }

    @Test
    void date_isSupportedWithoutExtras() {
        assertTrue(validator.validate(q("DATE")).isEmpty());
        assertTrue(validator.validate(q("TIME")).isEmpty());
    }

    @Test
    void attentionAnswer_onlyOnRadioAndMustBeAnOption() {
        QuestionVO radio = q("RADIO");
        radio.setOptions(List.of("A", "B"));
        radio.setAttentionAnswer("B");
        assertTrue(validator.validate(radio).isEmpty());

        QuestionVO wrongOption = q("RADIO");
        wrongOption.setOptions(List.of("A", "B"));
        wrongOption.setAttentionAnswer("C");
        assertFalse(validator.validate(wrongOption).isEmpty());

        QuestionVO onText = q("SHORT_TEXT");
        onText.setAttentionAnswer("A");
        assertFalse(validator.validate(onText).isEmpty());
    }

    @Test
    void multipleChoiceGrid_requiresRowsAndColumns() {
        QuestionVO missing = q("MULTIPLE_CHOICE_GRID");
        missing.setOptions(List.of("좋음", "보통"));
        assertFalse(validator.validate(missing).isEmpty());

        QuestionVO oneCol = q("MULTIPLE_CHOICE_GRID");
        oneCol.setRowLabels(List.of("맛", "양"));
        oneCol.setOptions(List.of("좋음"));
        assertFalse(validator.validate(oneCol).isEmpty());

        QuestionVO ok = q("MULTIPLE_CHOICE_GRID");
        ok.setRowLabels(List.of("맛", "양"));
        ok.setOptions(List.of("좋음", "보통", "나쁨"));
        assertTrue(validator.validate(ok).isEmpty());
    }

    @Test
    void grid_rejectsEqualsInLabelsAndDuplicates() {
        QuestionVO eq = q("CHECKBOX_GRID");
        eq.setRowLabels(List.of("행=1"));
        eq.setOptions(List.of("A", "B"));
        assertFalse(validator.validate(eq).isEmpty());

        QuestionVO dup = q("CHECKBOX_GRID");
        dup.setRowLabels(List.of("맛", "맛"));
        dup.setOptions(List.of("A", "B"));
        assertFalse(validator.validate(dup).isEmpty());
    }

    @Test
    void checkboxGrid_allowsPerRowMinMax() {
        QuestionVO ok = q("CHECKBOX_GRID");
        ok.setRowLabels(List.of("맛", "양"));
        ok.setOptions(List.of("A", "B", "C"));
        ok.setMinSelect(1);
        ok.setMaxSelect(2);
        assertTrue(validator.validate(ok).isEmpty());

        QuestionVO badMax = q("CHECKBOX_GRID");
        badMax.setRowLabels(List.of("맛"));
        badMax.setOptions(List.of("A", "B"));
        badMax.setMaxSelect(5);
        assertFalse(validator.validate(badMax).isEmpty());
    }
}
