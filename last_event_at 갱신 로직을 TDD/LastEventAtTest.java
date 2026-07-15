import java.util.*;

/**
 * last_event_at 갱신 로직 TDD.
 *
 * last_event_at은 타임아웃 잡이 "마지막 활동 시각"으로 쓰는 값. 기준은 클라이언트 occurredAt.
 *
 * 규칙:
 *  E1. 이벤트 처리 시 occurredAt으로 last_event_at 갱신.
 *  E2. ★순서 역전 방어★ 늦게 도착한 옛 이벤트(작은 occurredAt)는 last_event_at을 되돌리지 않음(MAX 유지).
 *      — last_question_order의 R7과 같은 원리. 클라 시각은 도착 순서와 무관하므로 필수.
 *  E3. ★미래 시각 방어★ 클라 시계가 안 맞아 서버 now보다 한참 미래인 occurredAt은
 *      신뢰하지 않고 서버 now로 클램프(clamp). 미래값이 박혀 타임아웃이 영영 안 걸리는 것 방지.
 *  E4. occurredAt이 없으면(누락) 서버 now를 사용.
 *  E5. 멱등 무시된 이벤트(중복 started)도 last_event_at은 갱신해야 함
 *      — 활동은 실제로 있었으므로(타임아웃 판정에 반영돼야).
 */
public class LastEventAtTest {
    static int pass = 0, fail = 0;
    static void check(String n, boolean c) {
        if (c) { pass++; System.out.println("  PASS " + n); }
        else { fail++; System.out.println("  FAIL " + n); }
    }

    // 시각을 epoch millis로 단순화
    static Map<String,Object> ev(String sid, String type, Long occurredAt) {
        Map<String,Object> m = new HashMap<>();
        m.put("sessionId", sid); m.put("formId", "F1"); m.put("eventType", type);
        if (occurredAt != null) m.put("occurredAt", occurredAt);
        return m;
    }

    public static void main(String[] args) {
        System.out.println("== E1: occurredAt으로 갱신 ==");
        {
            EventService s = new EventService();
            long serverNow = 1000_000L;
            s.recordAt(ev("s1","survey_started", 900_000L), serverNow);
            check("last_event_at = 900000", s.lastEventAt("s1") == 900_000L);
        }

        System.out.println("== E2: 순서 역전 방어(MAX 유지) ==");
        {
            EventService s = new EventService();
            long now = 2000_000L;
            s.recordAt(ev("s2","survey_started", 1000_000L), now);
            s.recordAt(ev("s2","question_answered", 1500_000L), now);  // 더 최신
            s.recordAt(ev("s2","question_answered", 1200_000L), now);  // 늦게 도착한 옛 이벤트
            check("되돌아가지 않고 1500000 유지", s.lastEventAt("s2") == 1500_000L);
        }

        System.out.println("== E3: 미래 시각 방어(서버 now로 클램프) ==");
        {
            EventService s = new EventService();
            long now = 1000_000L;
            // 클라 시계가 1시간 미래(3,600,000ms 더)로 어긋남
            s.recordAt(ev("s3","survey_started", now + 3_600_000L), now);
            check("미래값 거부, 서버 now로 클램프", s.lastEventAt("s3") == now);
        }

        System.out.println("== E4: occurredAt 누락 시 서버 now ==");
        {
            EventService s = new EventService();
            long now = 1234_567L;
            s.recordAt(ev("s4","survey_started", null), now);
            check("누락 시 now 사용", s.lastEventAt("s4") == now);
        }

        System.out.println("== E5: 멱등 무시된 이벤트도 last_event_at 갱신 ==");
        {
            EventService s = new EventService();
            long now = 3000_000L;
            s.recordAt(ev("s5","survey_started", 1000_000L), now);
            s.recordAt(ev("s5","survey_started", 1500_000L), now);  // 중복 started(무시되지만 활동은 있었음)
            check("중복 started여도 last=1500000", s.lastEventAt("s5") == 1500_000L);
        }

        System.out.println("\n결과: " + pass + " pass / " + fail + " fail");
        if (fail > 0) System.exit(1);
    }
}
