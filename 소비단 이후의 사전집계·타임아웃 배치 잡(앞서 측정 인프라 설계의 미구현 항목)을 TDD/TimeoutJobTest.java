import java.util.*;

/**
 * 타임아웃 이탈 마감 배치 잡 TDD.
 *
 * 책임: sendBeacon 유실 대비 백업. "마지막 이벤트 후 N분 무활동 & 미제출" 세션을
 *       end_state=timeout으로 마감한다. (측정인프라 3-2 추정 이탈)
 *
 * 규칙:
 *  T1. 마지막 이벤트 후 임계(30분) 초과 + 미종료 → timeout 마감.
 *  T2. ★경계★ 임계 '미만'(29분59초)은 마감 안 함(아직 응답 중일 수 있음).
 *  T3. 이미 종료된 세션(submitted/abandoned)은 절대 건드리지 않음.
 *  T4. timeout 마감 시 마지막 이벤트 시각을 ended_at으로(now가 아니라 — 실제 이탈 시점 근사).
 *  T5. ★멱등★ 잡을 두 번 돌려도 이미 timeout된 건 다시 처리 안 함.
 *  T6. ★경합 방어★ 마감 직전 그 세션이 제출되면(상태가 바뀌면) timeout 적용 안 함.
 */
public class TimeoutJobTest {
    static int pass = 0, fail = 0;
    static void check(String n, boolean c) {
        if (c) { pass++; System.out.println("  PASS " + n); }
        else { fail++; System.out.println("  FAIL " + n); }
    }

    // now=10:00:00 기준. 임계 30분.
    static final long NOW = ts("10:00:00");
    static long ts(String hms) {  // 같은 날 가정, 초 단위 epoch 모사
        String[] p = hms.split(":");
        return Long.parseLong(p[0])*3600 + Long.parseLong(p[1])*60 + Long.parseLong(p[2]);
    }

    public static void main(String[] args) {
        System.out.println("== T1/T2: 임계 경계 ==");
        {
            TimeoutJob job = new TimeoutJob(30 * 60);   // 30분(초)
            FakeSessionStore store = new FakeSessionStore();
            store.add(new Sess("s1", null, ts("09:29:00"), null));  // 31분 전 → 마감
            store.add(new Sess("s2", null, ts("09:31:00"), null));  // 29분 전 → 유지
            int closed = job.run(store, NOW);
            check("31분 무활동 → timeout 마감", "timeout".equals(store.get("s1").endState));
            check("29분 무활동 → 유지(미마감)", store.get("s2").endState == null);
            check("마감 건수 1", closed == 1);
        }

        System.out.println("== T3: 이미 종료된 세션은 안 건드림 ==");
        {
            TimeoutJob job = new TimeoutJob(30 * 60);
            FakeSessionStore store = new FakeSessionStore();
            store.add(new Sess("done", "submitted", ts("08:00:00"), ts("08:01:00")));
            job.run(store, NOW);
            check("submitted 유지(timeout 덮어쓰지 않음)", "submitted".equals(store.get("done").endState));
        }

        System.out.println("== T4: ended_at = 마지막 이벤트 시각 ==");
        {
            TimeoutJob job = new TimeoutJob(30 * 60);
            FakeSessionStore store = new FakeSessionStore();
            store.add(new Sess("s3", null, ts("09:00:00"), null));
            job.run(store, NOW);
            check("ended_at = 마지막 이벤트(09:00)", store.get("s3").endedAt == ts("09:00:00"));
        }

        System.out.println("== T5: 멱등(두 번 실행) ==");
        {
            TimeoutJob job = new TimeoutJob(30 * 60);
            FakeSessionStore store = new FakeSessionStore();
            store.add(new Sess("s4", null, ts("09:00:00"), null));
            int first = job.run(store, NOW);
            int second = job.run(store, NOW);   // 두 번째
            check("1차 마감 1건", first == 1);
            check("2차 마감 0건(이미 timeout)", second == 0);
        }

        System.out.println("== T6: 경합 방어(마감 직전 제출됨) ==");
        {
            TimeoutJob job = new TimeoutJob(30 * 60);
            // 잡이 후보를 고른 뒤, 마감 직전 그 세션이 submitted로 바뀐 상황을 모사
            FakeSessionStore store = new FakeSessionStore();
            Sess s5 = new Sess("s5", null, ts("09:00:00"), null);
            store.add(s5);
            store.simulateConcurrentSubmit("s5");   // 조건부 업데이트 직전에 제출됨
            int closed = job.run(store, NOW);
            check("제출된 세션은 timeout 안 됨", "submitted".equals(store.get("s5").endState));
            check("마감 0건", closed == 0);
        }

        System.out.println("\n결과: " + pass + " pass / " + fail + " fail");
        if (fail > 0) System.exit(1);
    }
}
