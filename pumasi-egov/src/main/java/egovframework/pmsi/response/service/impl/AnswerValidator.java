package egovframework.pmsi.response.service.impl;

import egovframework.pmsi.form.service.QuestionVO;
import egovframework.pmsi.response.service.AnswerVO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 답변 값 유효성 검증 — 순수 로직(프레임워크 무관, 결정론적).
 *
 * FormValidator가 "질문 정의"를 검증한다면, 이 클래스는 "제출된 답변 값"이
 * 질문 정의를 지키는지 검증한다. 필수 여부는 호출측(ResponseService)이 먼저 검사하므로,
 * 여기서는 값이 존재하는 답변만 유형별 규칙을 적용한다.
 *
 *  - 공통       : 존재하지 않는 questionId 거부, 같은 문항 중복 답변 거부
 *  - RADIO/DROPDOWN : 값 1개, 보기 중 하나여야 함
 *  - CHECKBOX   : 모든 값이 보기에 속함, 값 중복 금지, min/maxSelect 준수
 *  - 텍스트     : 값 1개, min/maxLength·정규식(regex) 준수
 *  - LINEAR_SCALE/RATING: 값 1개, 정수, scaleMin~scaleMax 범위
 *  - DATE       : 값 1개, ISO 날짜(yyyy-MM-dd)
 */
public class AnswerValidator {

    /** @return 오류 메시지 목록(비어 있으면 유효) */
    public List<String> validate(List<QuestionVO> questions, List<AnswerVO> answers) {
        List<String> errors = new ArrayList<>();
        Map<String, QuestionVO> byId = new HashMap<>();
        for (QuestionVO q : questions) byId.put(q.getQuestionId(), q);

        Set<String> seen = new HashSet<>();
        for (AnswerVO a : answers) {
            QuestionVO q = byId.get(a.getQuestionId());
            if (q == null) {
                errors.add("존재하지 않는 문항에 대한 답변입니다: " + a.getQuestionId());
                continue;
            }
            if (!seen.add(a.getQuestionId())) {
                errors.add("같은 문항에 답변이 중복되었습니다: " + q.getTitle());
                continue;
            }
            List<String> values = nonBlank(a.getValues());
            if (values.isEmpty()) continue;   // 미응답 — 필수 여부는 호출측이 검사

            switch (q.getType()) {
                case "RADIO", "DROPDOWN" -> validateRadio(q, values, errors);
                case "CHECKBOX"     -> validateCheckbox(q, values, errors);
                case "SHORT_TEXT", "LONG_TEXT" -> validateText(q, values, errors);
                case "LINEAR_SCALE", "RATING" -> validateScale(q, values, errors);
                case "DATE"         -> validateDate(q, values, errors);
                case "FILE"         -> validateFile(q, values, errors);
                case "DESCRIPTION", "IMAGE" ->
                        errors.add("안내 문항에는 답변을 제출할 수 없습니다: " + q.getTitle());
                default -> errors.add("지원하지 않는 질문 유형입니다: " + q.getType());
            }
        }
        return errors;
    }

    private void validateRadio(QuestionVO q, List<String> values, List<String> errors) {
        if (values.size() != 1) {
            errors.add("단일선택 문항에는 값이 1개여야 합니다: " + q.getTitle());
            return;
        }
        if (q.getOptions() == null || !q.getOptions().contains(values.get(0))) {
            errors.add("보기에 없는 값입니다: " + q.getTitle());
        }
    }

    private void validateCheckbox(QuestionVO q, List<String> values, List<String> errors) {
        if (new HashSet<>(values).size() != values.size()) {
            errors.add("같은 보기를 중복 선택할 수 없습니다: " + q.getTitle());
        }
        if (q.getOptions() == null || !q.getOptions().containsAll(values)) {
            errors.add("보기에 없는 값이 포함되어 있습니다: " + q.getTitle());
        }
        if (q.getMinSelect() != null && values.size() < q.getMinSelect()) {
            errors.add("최소 " + q.getMinSelect() + "개를 선택해야 합니다: " + q.getTitle());
        }
        if (q.getMaxSelect() != null && values.size() > q.getMaxSelect()) {
            errors.add("최대 " + q.getMaxSelect() + "개까지 선택할 수 있습니다: " + q.getTitle());
        }
    }

    private void validateText(QuestionVO q, List<String> values, List<String> errors) {
        if (values.size() != 1) {
            errors.add("텍스트 문항에는 값이 1개여야 합니다: " + q.getTitle());
            return;
        }
        String text = values.get(0);
        if (q.getMinLength() != null && text.length() < q.getMinLength()) {
            errors.add("최소 " + q.getMinLength() + "자 이상 입력해야 합니다: " + q.getTitle());
        }
        if (q.getMaxLength() != null && text.length() > q.getMaxLength()) {
            errors.add("최대 " + q.getMaxLength() + "자까지 입력할 수 있습니다: " + q.getTitle());
        }
        if (q.getRegex() != null && !q.getRegex().isBlank()) {
            try {
                if (!Pattern.matches(q.getRegex(), text)) {
                    errors.add("입력 형식이 올바르지 않습니다: " + q.getTitle());
                }
            } catch (PatternSyntaxException ignored) {
                // 질문 정의의 정규식이 잘못된 경우 답변자를 막지 않는다
            }
        }
    }

    private void validateFile(QuestionVO q, List<String> values, List<String> errors) {
        if (values.size() != 1) {
            errors.add("파일 문항에는 값이 1개여야 합니다: " + q.getTitle());
        }
    }

    private void validateDate(QuestionVO q, List<String> values, List<String> errors) {
        if (values.size() != 1) {
            errors.add("날짜 문항에는 값이 1개여야 합니다: " + q.getTitle());
            return;
        }
        try {
            java.time.LocalDate.parse(values.get(0).trim());
        } catch (java.time.format.DateTimeParseException e) {
            errors.add("날짜 형식(yyyy-MM-dd)이 올바르지 않습니다: " + q.getTitle());
        }
    }

    private void validateScale(QuestionVO q, List<String> values, List<String> errors) {
        if (values.size() != 1) {
            errors.add("척도 문항에는 값이 1개여야 합니다: " + q.getTitle());
            return;
        }
        int v;
        try {
            v = Integer.parseInt(values.get(0).trim());
        } catch (NumberFormatException e) {
            errors.add("척도 값은 정수여야 합니다: " + q.getTitle());
            return;
        }
        Integer min = q.getScaleMin();
        Integer max = q.getScaleMax();
        if ((min != null && v < min) || (max != null && v > max)) {
            errors.add("척도 값이 범위(" + min + "~" + max + ")를 벗어났습니다: " + q.getTitle());
        }
    }

    private List<String> nonBlank(List<String> values) {
        if (values == null) return List.of();
        List<String> out = new ArrayList<>();
        for (String v : values) {
            if (v != null && !v.isBlank()) out.add(v);
        }
        return out;
    }
}
