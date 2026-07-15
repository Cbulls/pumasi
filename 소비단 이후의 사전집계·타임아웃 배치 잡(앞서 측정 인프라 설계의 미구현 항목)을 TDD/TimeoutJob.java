import java.util.*;
import java.util.stream.Collectors;

/** response_session 한 행 (타임아웃 잡 대상). 가변 — 마감 시 상태 변경. */
class Sess {
    final String id;
    String endState;          // null=진행중, submitted/abandoned/timeout
    final long lastEventTs;   // 마지막 이벤트 시각(초)
    Long endedAt;
    Sess(String id, String endState, long lastEventTs, Long endedAt) {
        this.id = id; this.endState = endState; this.lastEventTs = lastEventTs; this.endedAt = endedAt;
    }
}

/**
 * response_session 대역. 핵심은 closeIfStillOpen — 조건부 마감.
 *
 * 실제 SQL:
 *   UPDATE response_session SET end_state='timeout', ended_at=?
 *   WHERE id=? AND end_state IS NULL      ← 이 조건이 경합을 막는다(T6)
 * 영향 행수 0이면 그 사이 누가 상태를 바꾼 것(제출 등) → 마감 스킵.
 */
class FakeSessionStore {
    private final Map<String, Sess> rows = new LinkedHashMap<>();
    private final Set<String> concurrentSubmit = new HashSet<>();

    void add(Sess s) { rows.put(s.id, s); }
    Sess get(String id) { return rows.get(id); }

    /** 마감 후보 조회: 미종료 & 마지막 이벤트가 cutoff 이전 */
    List<Sess> findTimeoutCandidates(long cutoffTs) {
        return rows.values().stream()
            .filter(s -> s.endState == null)
            .filter(s -> s.lastEventTs < cutoffTs)
            .collect(Collectors.toList());
    }

    /** 테스트용: 조건부 마감 '직전'에 제출이 끼어드는 상황 모사(T6) */
    void simulateConcurrentSubmit(String id) { concurrentSubmit.add(id); }

    /**
     * 조건부 마감. 여전히 미종료일 때만 timeout 적용. 적용했으면 true.
     * @return false = 그 사이 상태가 바뀜(경합) → 마감 스킵
     */
    boolean closeIfStillOpen(String id, long endedAt) {
        Sess s = rows.get(id);
        // 경합 모사: closeIfStillOpen 시점에 제출이 먼저 반영됨
        if (concurrentSubmit.remove(id)) {
            s.endState = "submitted";
            s.endedAt = endedAt;
        }
        if (s.endState != null) return false;   // 이미 종료(제출/이탈/이전 timeout) → 스킵
        s.endState = "timeout";
        s.endedAt = endedAt;
        return true;
    }
}

/**
 * 타임아웃 이탈 마감 잡.
 *
 * 1) 미종료 & 마지막 이벤트가 (now - threshold) 이전인 세션을 후보로 조회.
 * 2) 각 후보를 조건부 마감(closeIfStillOpen) → 경합 시 자동 스킵(T6).
 * 3) ended_at은 now가 아니라 마지막 이벤트 시각(실제 이탈 시점 근사, T4).
 * 멱등(T5): 이미 timeout이면 미종료 조건에 안 걸려 다시 처리 안 됨.
 */
class TimeoutJob {
    private final long thresholdSec;
    TimeoutJob(long thresholdSec) { this.thresholdSec = thresholdSec; }

    int run(FakeSessionStore store, long nowTs) {
        long cutoff = nowTs - thresholdSec;
        int closed = 0;
        for (Sess s : store.findTimeoutCandidates(cutoff)) {
            if (store.closeIfStillOpen(s.id, s.lastEventTs)) {   // T4: lastEventTs를 ended_at으로
                closed++;
            }
        }
        return closed;
    }
}
