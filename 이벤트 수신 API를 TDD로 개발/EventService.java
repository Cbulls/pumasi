import java.util.*;

/**
 * 이벤트 처리 서비스 — 세션 상태 관리와 전이 규칙.
 * 순수 로직(인메모리 세션맵)으로, Spring 서비스는 이걸 DB 위에서 재현한다.
 */
public class EventService {

    /** 세션 상태 (DB의 response_session 한 행에 대응) */
    static class Session {
        boolean started = false;
        boolean ended = false;          // submitted 또는 abandoned
        int startedCount = 0;           // 멱등 검증용
        Integer lastQuestionOrder = null;
    }

    private final Map<String, Session> sessions = new HashMap<>();

    private Session sess(String id) {
        return sessions.computeIfAbsent(id, k -> new Session());
    }

    /**
     * 이벤트 1건 처리. 기록되면 true, 무시되면 false.
     * 검증 실패한 이벤트는 호출 전에 걸러진다고 가정(여기선 전이 규칙만).
     */
    boolean record(Map<String, Object> ev) {
        String sessionId = (String) ev.get("sessionId");
        String type = (String) ev.get("eventType");
        Session s = sess(sessionId);

        switch (type) {
            case "survey_viewed":
                return true; // 노출은 항상 기록

            case "survey_started":
                // R3: 세션당 1회만
                if (s.started) return false;   // 멱등: 두 번째 무시
                s.started = true;
                s.startedCount++;
                return true;

            case "question_answered":
                // R5: last order 갱신 (started 안 됐으면 시작 처리)
                if (!s.started) { s.started = true; s.startedCount++; }
                // R7: 이벤트가 순서 뒤바뀌어 도착할 수 있으므로 최대값만 유지(역행 방지)
                Object order = ev.get("questionOrder");
                if (order != null) {
                    int o = ((Number) order).intValue();
                    if (s.lastQuestionOrder == null || o > s.lastQuestionOrder) {
                        s.lastQuestionOrder = o;
                    }
                }
                return true;

            case "survey_submitted":
                // R6: 세션 종료
                s.ended = true;
                return true;

            case "survey_abandoned":
                // R4: started 이후 & 미제출일 때만 유효
                if (!s.started || s.ended) return false;
                s.ended = true;
                return true;

            default:
                return false;
        }
    }

    int startedCount(String sessionId) { return sess(sessionId).startedCount; }
    Integer lastQuestionOrder(String sessionId) { return sess(sessionId).lastQuestionOrder; }
    boolean isEnded(String sessionId) { return sess(sessionId).ended; }
}
