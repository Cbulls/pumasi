import java.util.*;

/**
 * 이벤트 수신 핵심 로직 TDD 테스트 (순수 Java, 무의존성).
 *
 * 검증 대상 규칙:
 *  R1. 5종 이벤트 타입만 허용. 그 외는 거부.
 *  R2. sessionId/formId/eventType 필수. 누락 시 거부.
 *  R3. survey_started는 세션당 1회만 기록 (완료율 분모 정확성). 두 번째는 무시(멱등).
 *  R4. survey_abandoned는 started 이후 & 미제출일 때만 유효. 제출 후 이탈은 무시.
 *  R5. question_answered는 last_question_order를 갱신.
 *  R6. survey_submitted는 세션을 종료 상태로.
 *
 * 이 테스트는 EventValidator / EventService 가 아직 없으므로 처음엔 컴파일 실패(RED)다.
 */
public class EventLogicTest {

    static int pass = 0, fail = 0;
    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("  PASS " + name); }
        else { fail++; System.out.println("  FAIL " + name); }
    }

    public static void main(String[] args) {
        System.out.println("== R1: 허용된 이벤트 타입만 ==");
        check("valid type 통과", EventValidator.isValidType("survey_started"));
        check("invalid type 거부", !EventValidator.isValidType("hacker_event"));

        System.out.println("== R2: 필수 필드 검증 ==");
        check("정상 페이로드 통과",
                EventValidator.validate(ev("s1","F1","survey_viewed")).isEmpty());
        check("sessionId 누락 거부",
                !EventValidator.validate(ev(null,"F1","survey_viewed")).isEmpty());
        check("formId 누락 거부",
                !EventValidator.validate(ev("s1",null,"survey_viewed")).isEmpty());
        check("eventType 누락 거부",
                !EventValidator.validate(ev("s1","F1",null)).isEmpty());

        System.out.println("== R3: survey_started 멱등 (세션당 1회) ==");
        EventService svc = new EventService();
        svc.record(ev("s1","F1","survey_started"));
        svc.record(ev("s1","F1","survey_started"));  // 두 번째
        check("started 한 번만 기록됨", svc.startedCount("s1") == 1);

        System.out.println("== R4: abandoned는 started 이후·미제출만 ==");
        EventService s2 = new EventService();
        s2.record(ev("s2","F1","survey_started"));
        boolean ab1 = s2.record(ev("s2","F1","survey_abandoned"));
        check("started 후 abandoned 유효", ab1);
        EventService s3 = new EventService();
        boolean ab2 = s3.record(ev("s3","F1","survey_abandoned")); // started 없이
        check("started 없는 abandoned 무시", !ab2);
        EventService s4 = new EventService();
        s4.record(ev("s4","F1","survey_started"));
        s4.record(ev("s4","F1","survey_submitted"));
        boolean ab3 = s4.record(ev("s4","F1","survey_abandoned")); // 제출 후
        check("제출 후 abandoned 무시", !ab3);

        System.out.println("== R5: question_answered가 last order 갱신 ==");
        EventService s5 = new EventService();
        s5.record(ev("s5","F1","survey_started"));
        s5.record(evq("s5","F1","question_answered", 3));
        s5.record(evq("s5","F1","question_answered", 7));
        check("last_question_order=7", s5.lastQuestionOrder("s5") == 7);

        System.out.println("== R6: submitted가 세션 종료 ==");
        EventService s6 = new EventService();
        s6.record(ev("s6","F1","survey_started"));
        s6.record(ev("s6","F1","survey_submitted"));
        check("세션 종료 상태", s6.isEnded("s6"));

        System.out.println("\n결과: " + pass + " pass / " + fail + " fail");
        if (fail > 0) System.exit(1);
    }

    static Map<String,Object> ev(String sid, String fid, String type) {
        Map<String,Object> m = new HashMap<>();
        m.put("sessionId", sid); m.put("formId", fid); m.put("eventType", type);
        return m;
    }
    static Map<String,Object> evq(String sid, String fid, String type, int order) {
        Map<String,Object> m = ev(sid, fid, type);
        m.put("questionOrder", order);
        return m;
    }
}
