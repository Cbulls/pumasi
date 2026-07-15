import java.util.*;

/**
 * 사전집계 배치 잡 TDD.
 *
 * 책임: response_session을 (날짜, form, arm)별로 굴려 form_metrics_daily를 채운다.
 *
 * 규칙:
 *  A1. started/submitted/pass/abandoned 카운트가 정확하다.
 *  A2. 완료율은 pass 기준(불성실 제외). submitted와 구분해 둘 다 저장.
 *  A3. 소요시간 중앙값은 pass 건만 대상.
 *  A4. ★재실행 멱등성★: 같은 날짜를 두 번 집계해도 결과가 두 배가 되지 않는다(upsert).
 *  A5. arm(실험군)별로 분리 집계한다.
 *  A6. 진행중(end_state=null) 세션은 started에는 포함, 종료 카운트엔 미포함.
 *
 * SessionRow는 response_session의 한 행을 모사. MetricsStore는 form_metrics_daily 대역
 * (PK=(date,form,arm) upsert).
 */
public class AggregationJobTest {
    static int pass = 0, fail = 0;
    static void check(String n, boolean c) {
        if (c) { pass++; System.out.println("  PASS " + n); }
        else { fail++; System.out.println("  FAIL " + n); }
    }

    static SessionRow row(String date, String arm, String endState, String flag, Integer dur) {
        return new SessionRow("F1", date, arm, endState, flag, dur);
    }

    public static void main(String[] args) {
        // 2026-06-01, arm C: 시작10 / 제출6(pass5,reject1) / 이탈2 / 진행중2
        List<SessionRow> sessions = new ArrayList<>();
        for (int i = 0; i < 5; i++) sessions.add(row("2026-06-01","C","submitted","pass", 100 + i*20));
        sessions.add(row("2026-06-01","C","submitted","reject", 8));
        sessions.add(row("2026-06-01","C","abandoned",null,null));
        sessions.add(row("2026-06-01","C","abandoned",null,null));
        sessions.add(row("2026-06-01","C",null,null,null));   // 진행중
        sessions.add(row("2026-06-01","C",null,null,null));   // 진행중

        MetricsStore store = new MetricsStore();
        AggregationJob job = new AggregationJob(store);

        System.out.println("== A1/A2/A6: 카운트 정확성 ==");
        job.aggregateDay("2026-06-01", sessions);
        MetricRow m = store.get("2026-06-01", "F1", "C");
        check("started=10", m.started == 10);
        check("submitted=6", m.submitted == 6);
        check("pass=5", m.pass == 5);
        check("abandoned=2", m.abandoned == 2);

        System.out.println("== A3: 소요시간 중앙값(pass만) ==");
        // pass 5건: 100,120,140,160,180 → 중앙값 140
        check("median=140", m.medianDuration != null && m.medianDuration == 140);

        System.out.println("== A4: 재실행 멱등성 (같은 날 두 번) ==");
        job.aggregateDay("2026-06-01", sessions);   // 두 번째 실행
        MetricRow m2 = store.get("2026-06-01", "F1", "C");
        check("started 여전히 10(두 배 아님)", m2.started == 10);
        check("저장 행 1개(중복 INSERT 안 함)", store.rowCount() == 1);

        System.out.println("== A5: arm별 분리 ==");
        List<SessionRow> mixed = new ArrayList<>();
        mixed.add(row("2026-06-02","C","submitted","pass",100));
        mixed.add(row("2026-06-02","T1","submitted","pass",100));
        mixed.add(row("2026-06-02","T1","submitted","pass",100));
        job.aggregateDay("2026-06-02", mixed);
        check("arm C started=1", store.get("2026-06-02","F1","C").started == 1);
        check("arm T1 started=2", store.get("2026-06-02","F1","T1").started == 2);

        System.out.println("\n결과: " + pass + " pass / " + fail + " fail");
        if (fail > 0) System.exit(1);
    }
}
