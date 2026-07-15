import java.util.*;
import java.util.stream.Collectors;

/** response_session 한 행 (집계 입력) */
class SessionRow {
    final String formId, date, arm, endState, qualityFlag;
    final Integer durationSec;
    SessionRow(String formId, String date, String arm, String endState,
               String qualityFlag, Integer durationSec) {
        this.formId = formId; this.date = date; this.arm = arm;
        this.endState = endState; this.qualityFlag = qualityFlag; this.durationSec = durationSec;
    }
}

/** form_metrics_daily 한 행 (집계 결과) */
class MetricRow {
    int started, submitted, pass, abandoned;
    Integer medianDuration;
}

/**
 * form_metrics_daily 대역. PK=(date, form, arm).
 * upsert: 같은 키로 다시 쓰면 덮어쓴다(append 아님) → 재실행 멱등성의 토대.
 */
class MetricsStore {
    private final Map<String, MetricRow> rows = new HashMap<>();
    private String key(String d, String f, String a) { return d + "|" + f + "|" + a; }

    void upsert(String d, String f, String a, MetricRow row) {
        rows.put(key(d, f, a), row);   // put = 덮어쓰기. 키가 있으면 교체, 없으면 삽입.
    }
    MetricRow get(String d, String f, String a) { return rows.get(key(d, f, a)); }
    int rowCount() { return rows.size(); }
}

/**
 * 사전집계 배치 잡.
 *
 * 멱등성의 핵심: 집계는 "원본 세션을 다시 세어 결과를 덮어쓴다". 증분 누적이 아니라
 * 전체 재계산이므로 몇 번 돌려도 같은 값. (A4)
 */
class AggregationJob {
    private final MetricsStore store;
    AggregationJob(MetricsStore store) { this.store = store; }

    void aggregateDay(String date, List<SessionRow> sessions) {
        // 해당 날짜의 세션을 (form, arm)으로 그룹핑
        Map<String, List<SessionRow>> groups = sessions.stream()
            .filter(s -> s.date.equals(date))
            .collect(Collectors.groupingBy(s -> s.formId + "|" + s.arm));

        for (var entry : groups.entrySet()) {
            String[] fa = entry.getKey().split("\\|");
            String formId = fa[0], arm = fa[1];
            List<SessionRow> g = entry.getValue();

            MetricRow m = new MetricRow();
            m.started   = g.size();                                    // A1, A6: 진행중 포함
            m.submitted = (int) g.stream().filter(s -> "submitted".equals(s.endState)).count();
            m.pass      = (int) g.stream().filter(s -> "pass".equals(s.qualityFlag)).count();
            m.abandoned = (int) g.stream().filter(s -> "abandoned".equals(s.endState)).count();
            m.medianDuration = median(
                g.stream().filter(s -> "pass".equals(s.qualityFlag))   // A3: pass만
                          .map(s -> s.durationSec).filter(Objects::nonNull)
                          .sorted().collect(Collectors.toList()));

            store.upsert(date, formId, arm, m);   // A4: 덮어쓰기
        }
    }

    private Integer median(List<Integer> sorted) {
        if (sorted.isEmpty()) return null;
        int n = sorted.size();
        if (n % 2 == 1) return sorted.get(n / 2);
        return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2;
    }
}
