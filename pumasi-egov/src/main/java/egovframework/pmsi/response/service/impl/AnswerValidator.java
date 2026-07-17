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
 *  - TIME       : 값 1개, HH:mm 또는 HH:mm:ss
 *  - GRID       : "행=열" 쌍. MC 그리드는 행당 1개, 체크 그리드는 (행,열) 유일 + 행당 min/max
 *  - allowOther : RADIO/CHECKBOX에서 "기타: ..." 접두 값 허용
 */
public class AnswerValidator {

    static final String OTHER_PREFIX = "기타:";
    static final String GRID_SEP = "=";

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
                case "TIME"         -> validateTime(q, values, errors);
                case "FILE"         -> validateFile(q, values, errors);
                case "MULTIPLE_CHOICE_GRID" -> validateMultipleChoiceGrid(q, values, errors);
                case "CHECKBOX_GRID" -> validateCheckboxGrid(q, values, errors);
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
        String v = values.get(0);
        if (q.getOptions() != null && q.getOptions().contains(v)) return;
        if (Boolean.TRUE.equals(q.getAllowOther()) && isOtherValue(v)) {
            if (otherText(v).isBlank()) {
                errors.add("기타 내용을 입력하세요: " + q.getTitle());
            }
            return;
        }
        errors.add("보기에 없는 값입니다: " + q.getTitle());
    }

    private void validateCheckbox(QuestionVO q, List<String> values, List<String> errors) {
        if (new HashSet<>(values).size() != values.size()) {
            errors.add("같은 보기를 중복 선택할 수 없습니다: " + q.getTitle());
        }
        int otherCount = 0;
        for (String v : values) {
            if (q.getOptions() != null && q.getOptions().contains(v)) continue;
            if (Boolean.TRUE.equals(q.getAllowOther()) && isOtherValue(v)) {
                otherCount++;
                if (otherText(v).isBlank()) {
                    errors.add("기타 내용을 입력하세요: " + q.getTitle());
                }
            } else {
                errors.add("보기에 없는 값이 포함되어 있습니다: " + q.getTitle());
                break;
            }
        }
        if (otherCount > 1) {
            errors.add("기타는 하나만 선택할 수 있습니다: " + q.getTitle());
        }
        if (q.getMinSelect() != null && values.size() < q.getMinSelect()) {
            errors.add("최소 " + q.getMinSelect() + "개를 선택해야 합니다: " + q.getTitle());
        }
        if (q.getMaxSelect() != null && values.size() > q.getMaxSelect()) {
            errors.add("최대 " + q.getMaxSelect() + "개까지 선택할 수 있습니다: " + q.getTitle());
        }
    }

    private static boolean isOtherValue(String v) {
        return v != null && v.startsWith(OTHER_PREFIX);
    }

    private static String otherText(String v) {
        return v.substring(OTHER_PREFIX.length()).trim();
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

    private void validateTime(QuestionVO q, List<String> values, List<String> errors) {
        if (values.size() != 1) {
            errors.add("시간 문항에는 값이 1개여야 합니다: " + q.getTitle());
            return;
        }
        String raw = values.get(0).trim();
        try {
            if (raw.length() == 5) {
                java.time.LocalTime.parse(raw, java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            } else {
                java.time.LocalTime.parse(raw);
            }
        } catch (java.time.format.DateTimeParseException e) {
            errors.add("시간 형식(HH:mm)이 올바르지 않습니다: " + q.getTitle());
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

    /** 객관식 그리드: 필수면 모든 행에 정확히 1개. 행 중복 금지. */
    private void validateMultipleChoiceGrid(QuestionVO q, List<String> values, List<String> errors) {
        List<String[]> pairs = parseGridPairs(q, values, errors);
        if (pairs == null) return;

        Set<String> rowsSeen = new HashSet<>();
        for (String[] p : pairs) {
            if (!rowsSeen.add(p[0])) {
                errors.add("같은 행을 중복 선택할 수 없습니다: " + q.getTitle());
                return;
            }
        }
        if (q.isRequired()) {
            List<String> rows = q.getRowLabels() == null ? List.of() : q.getRowLabels();
            if (rowsSeen.size() != rows.size()) {
                errors.add("모든 행에 응답해야 합니다: " + q.getTitle());
            }
        }
    }

    /** 체크박스 그리드: (행,열) 유일. min/maxSelect는 행당 선택 수. */
    private void validateCheckboxGrid(QuestionVO q, List<String> values, List<String> errors) {
        List<String[]> pairs = parseGridPairs(q, values, errors);
        if (pairs == null) return;

        Set<String> cellKeys = new HashSet<>();
        Map<String, Integer> perRow = new HashMap<>();
        for (String[] p : pairs) {
            String key = p[0] + GRID_SEP + p[1];
            if (!cellKeys.add(key)) {
                errors.add("같은 칸을 중복 선택할 수 없습니다: " + q.getTitle());
                return;
            }
            perRow.merge(p[0], 1, Integer::sum);
        }
        Integer min = q.getMinSelect();
        Integer max = q.getMaxSelect();
        List<String> rows = q.getRowLabels() == null ? List.of() : q.getRowLabels();
        for (String row : rows) {
            int n = perRow.getOrDefault(row, 0);
            if (q.isRequired() && min == null && n < 1) {
                // 필수인데 행당 min 미설정이면 행마다 1개 이상
                errors.add("모든 행에 응답해야 합니다: " + q.getTitle());
                break;
            }
            if (min != null && n < min) {
                errors.add("행마다 최소 " + min + "개를 선택해야 합니다: " + q.getTitle());
                break;
            }
            if (max != null && n > max) {
                errors.add("행마다 최대 " + max + "개까지 선택할 수 있습니다: " + q.getTitle());
                break;
            }
        }
        // 정의에 없는 행은 parse에서 이미 거부. 선택적(비필수)이면 일부 행만 채워도 됨.
        if (!q.isRequired() && min != null) {
            for (Map.Entry<String, Integer> e : perRow.entrySet()) {
                if (e.getValue() < min) {
                    errors.add("행마다 최소 " + min + "개를 선택해야 합니다: " + q.getTitle());
                    break;
                }
            }
        }
    }

    /**
     * "행=열" 파싱. 형식·소속 오류 시 errors에 추가하고 null 반환.
     * 성공 시 [row, col] 목록.
     */
    private List<String[]> parseGridPairs(QuestionVO q, List<String> values, List<String> errors) {
        Set<String> rows = new HashSet<>(q.getRowLabels() == null ? List.of() : q.getRowLabels());
        Set<String> cols = new HashSet<>(q.getOptions() == null ? List.of() : q.getOptions());
        List<String[]> pairs = new ArrayList<>();
        for (String v : values) {
            int idx = v.indexOf(GRID_SEP);
            if (idx <= 0 || idx != v.lastIndexOf(GRID_SEP) || idx == v.length() - 1) {
                errors.add("그리드 답변 형식(행=열)이 올바르지 않습니다: " + q.getTitle());
                return null;
            }
            String row = v.substring(0, idx);
            String col = v.substring(idx + 1);
            if (!rows.contains(row) || !cols.contains(col)) {
                errors.add("그리드에 없는 행/열입니다: " + q.getTitle());
                return null;
            }
            pairs.add(new String[]{row, col});
        }
        return pairs;
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
