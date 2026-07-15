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
        Long lastEventAt = null;        // 마지막 활동 시각(타임아웃 판정용). MAX로만 갱신.
    }

    private final Map<String, Session> sessions = new HashMap<>();

    private Session sess(String id) {
        return sessions.computeIfAbsent(id, k -> new Session());
    }

    /**
     * 이벤트 1건 처리(시각 없는 버전 — 기존 테스트 하위호환). 서버 now를 현재로 가정.
     */
    boolean record(Map<String, Object> ev) {
        return recordAt(ev, System.currentTimeMillis());
    }

    /**
     * 이벤트 1건 처리. 기록되면 true, 멱등 무시면 false.
     * @param serverNow 서버 수신 시각(ms). 미래 시각 클램프와 occurredAt 누락 시 폴백에 사용.
     */
    boolean recordAt(Map<String, Object> ev, long serverNow) {
        String sessionId = (String) ev.get("sessionId");
        String type = (String) ev.get("eventType");
        Session s = sess(sessionId);

        boolean recorded = applyTransition(s, ev, type);

        // E1~E5: 전이 결과(멱등 무시 포함)와 무관하게 last_event_at 갱신.
        //   활동은 실제로 있었으므로 타임아웃 판정에 반영돼야 한다(E5).
        updateLastEventAt(s, eventTimestamp(ev, serverNow));

        return recorded;
    }

    /** occurredAt을 신뢰 가능한 시각으로 정규화. E3 미래방어 + E4 누락폴백. */
    private long eventTimestamp(Map<String, Object> ev, long serverNow) {
        Object occurred = ev.get("occurredAt");
        if (occurred == null) return serverNow;          // E4: 누락 → 서버 now
        long t = ((Number) occurred).longValue();
        if (t > serverNow) return serverNow;             // E3: 미래값 → now로 클램프
        return t;
    }

    /** E2: 순서 역전 방어 — 더 최신일 때만 갱신(MAX 유지). */
    private void updateLastEventAt(Session s, long ts) {
        if (s.lastEventAt == null || ts > s.lastEventAt) {
            s.lastEventAt = ts;
        }
    }

    /** 전이 규칙(R3~R7). 기록 대상이면 true. */
    private boolean applyTransition(Session s, Map<String, Object> ev, String type) {
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
    Long lastEventAt(String sessionId) { return sess(sessionId).lastEventAt; }
    boolean isEnded(String sessionId) { return sess(sessionId).ended; }
}
