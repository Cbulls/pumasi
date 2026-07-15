import java.util.*;

/**
 * 결과 그래프 집계 TDD. 질문 유형마다 집계 방식이 다르다(폼 빌더 유형 체계와 맞물림).
 *
 * 규칙:
 *  G1. RADIO/DROPDOWN: 보기별 응답 수. 분모 = 응답자 수. 비율 합 = 100%.
 *  G2. ★CHECKBOX★: 보기별 응답 수. 분모 = 응답자 수(선택 총횟수 아님).
 *      복수 선택이므로 비율 합이 100% 초과 가능.
 *  G3. ★불성실 제외★: quality_flag가 pass인 응답만 집계(reject/hold 제외).
 *  G4. LINEAR_SCALE/RATING: 값별 분포(히스토그램) + 평균.
 *  G5. SHORT_TEXT/LONG_TEXT: 집계 불가 → 응답 목록(text 타입, 카운트 없음).
 *  G6. 무응답(건너뜀)은 분모에서 제외(해당 질문에 답한 사람만).
 *  G7. 선택된 옵션 ID가 보기에 있어야 카운트(위조/삭제된 옵션 무시).
 */
public class ResultAggregationTest {
    static int pass = 0, fail = 0;
    static void check(String n, boolean c) {
        if (c) { pass++; System.out.println("  PASS " + n); }
        else { fail++; System.out.println("  FAIL " + n); }
    }
    static boolean near(double a, double b) { return Math.abs(a - b) < 0.01; }

    public static void main(String[] args) {
        ResultAggregator agg = new ResultAggregator();

        System.out.println("== G1: 단일선택 보기별 카운트 ==");
        {
            var q = QSpec.choice("q1", "RADIO", List.of("a", "b", "c"));
            // 응답: a,a,a,b,c (5명, pass)
            var responses = List.of(
                resp("pass", Map.of("q1", List.of("a"))),
                resp("pass", Map.of("q1", List.of("a"))),
                resp("pass", Map.of("q1", List.of("a"))),
                resp("pass", Map.of("q1", List.of("b"))),
                resp("pass", Map.of("q1", List.of("c"))));
            ChartData cd = agg.aggregate(q, responses);
            check("타입 = pie(보기 3개)", cd.chartType.equals("pie"));
            check("a=3", cd.counts.get("a") == 3);
            check("b=1", cd.counts.get("b") == 1);
            check("응답자 5", cd.respondentCount == 5);
            check("a 비율 60%", near(cd.ratios.get("a"), 60.0));
        }

        System.out.println("== G2: 체크박스 분모=응답자 수 ==");
        {
            var q = QSpec.choice("cb", "CHECKBOX", List.of("a", "b", "c"));
            // 3명이 각각 [a,b], [a,c], [a] 선택 → a=3,b=1,c=1, 응답자=3
            var responses = List.of(
                resp("pass", Map.of("cb", List.of("a", "b"))),
                resp("pass", Map.of("cb", List.of("a", "c"))),
                resp("pass", Map.of("cb", List.of("a"))));
            ChartData cd = agg.aggregate(q, responses);
            check("a=3", cd.counts.get("a") == 3);
            check("응답자 3(선택총횟수5 아님)", cd.respondentCount == 3);
            check("a 비율 100%(3/3)", near(cd.ratios.get("a"), 100.0));
            check("b 비율 33.33%(1/3)", near(cd.ratios.get("b"), 33.33));
            // 비율 합 = 100+33.33+33.33 = 166.67 > 100 (정상)
            double sum = cd.ratios.values().stream().mapToDouble(Double::doubleValue).sum();
            check("비율 합 > 100 허용", sum > 100.0);
        }

        System.out.println("== G3: 불성실 제외 ==");
        {
            var q = QSpec.choice("q1", "RADIO", List.of("a", "b"));
            var responses = List.of(
                resp("pass", Map.of("q1", List.of("a"))),
                resp("reject", Map.of("q1", List.of("a"))),   // 제외
                resp("hold", Map.of("q1", List.of("b"))),     // 제외
                resp("pass", Map.of("q1", List.of("b"))));
            ChartData cd = agg.aggregate(q, responses);
            check("pass만 집계 → 응답자 2", cd.respondentCount == 2);
            check("a=1, b=1", cd.counts.get("a") == 1 && cd.counts.get("b") == 1);
        }

        System.out.println("== G4: 선형배율 분포+평균 ==");
        {
            var q = QSpec.scale("sc", 1, 5);
            // 값: 5,4,5,3,5 → 평균 4.4
            var responses = List.of(
                resp("pass", Map.of("sc", List.of("5"))),
                resp("pass", Map.of("sc", List.of("4"))),
                resp("pass", Map.of("sc", List.of("5"))),
                resp("pass", Map.of("sc", List.of("3"))),
                resp("pass", Map.of("sc", List.of("5"))));
            ChartData cd = agg.aggregate(q, responses);
            check("타입 = histogram", cd.chartType.equals("histogram"));
            check("값5 = 3개", cd.counts.get("5") == 3);
            check("평균 4.4", near(cd.average, 4.4));
        }

        System.out.println("== G5: 주관식 집계 불가 → 목록 ==");
        {
            var q = QSpec.text("txt", "LONG_TEXT");
            var responses = List.of(
                resp("pass", Map.of("txt", List.of("좋아요"))),
                resp("pass", Map.of("txt", List.of("별로"))));
            ChartData cd = agg.aggregate(q, responses);
            check("타입 = text_list", cd.chartType.equals("text_list"));
            check("목록 2개", cd.textResponses.size() == 2);
            check("counts 비어있음", cd.counts.isEmpty());
        }

        System.out.println("== G6/G7: 무응답·위조 옵션 ==");
        {
            var q = QSpec.choice("q1", "RADIO", List.of("a", "b"));
            var responses = List.of(
                resp("pass", Map.of("q1", List.of("a"))),
                resp("pass", Map.of()),                       // 무응답(건너뜀)
                resp("pass", Map.of("q1", List.of("zzz"))));  // 위조 옵션
            ChartData cd = agg.aggregate(q, responses);
            check("답한 사람만 분모 → 응답자 1", cd.respondentCount == 1);
            check("위조 옵션 무시 → a=1만", cd.counts.get("a") == 1 && !cd.counts.containsKey("zzz"));
        }

        System.out.println("== G8: 단일선택 보기 적으면 pie, 많으면 bar ==");
        {
            // 보기 3개(≤5) → pie
            var qFew = QSpec.choice("qf", "RADIO", List.of("a", "b", "c"));
            var cdFew = agg.aggregate(qFew, List.of(resp("pass", Map.of("qf", List.of("a")))));
            check("보기 3개 → pie", cdFew.chartType.equals("pie"));
            // 보기 6개(>5) → bar
            var qMany = QSpec.choice("qm", "RADIO", List.of("a","b","c","d","e","f"));
            var cdMany = agg.aggregate(qMany, List.of(resp("pass", Map.of("qm", List.of("a")))));
            check("보기 6개 → bar", cdMany.chartType.equals("bar"));
            // 체크박스는 항상 bar(복수 선택은 pie 부적합)
            var qCb = QSpec.choice("qc", "CHECKBOX", List.of("a", "b"));
            var cdCb = agg.aggregate(qCb, List.of(resp("pass", Map.of("qc", List.of("a")))));
            check("체크박스 → bar", cdCb.chartType.equals("bar"));
        }

        System.out.println("== G9: 선형배율 중앙값 ==");
        {
            var q = QSpec.scale("sc", 1, 5);
            // 값: 1,2,3,4,5 → 중앙값 3
            var responses = List.of(
                resp("pass", Map.of("sc", List.of("1"))),
                resp("pass", Map.of("sc", List.of("2"))),
                resp("pass", Map.of("sc", List.of("3"))),
                resp("pass", Map.of("sc", List.of("4"))),
                resp("pass", Map.of("sc", List.of("5"))));
            ChartData cd = agg.aggregate(q, responses);
            check("중앙값 3", near(cd.median, 3.0));
            // 짝수개: 2,4 → 중앙값 3
            var even = List.of(
                resp("pass", Map.of("sc", List.of("2"))),
                resp("pass", Map.of("sc", List.of("4"))));
            check("짝수개 중앙값 (2,4)→3", near(agg.aggregate(q, even).median, 3.0));
        }

        System.out.println("\n결과: " + pass + " pass / " + fail + " fail");
        if (fail > 0) System.exit(1);
    }

    static RespData resp(String flag, Map<String, List<String>> answers) {
        return new RespData(flag, answers);
    }
}
