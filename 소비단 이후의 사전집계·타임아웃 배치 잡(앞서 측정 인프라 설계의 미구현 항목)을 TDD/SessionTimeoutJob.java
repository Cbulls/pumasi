package com.pumasiform.events.batch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * 타임아웃 이탈 마감 잡. sendBeacon 유실 대비 백업(측정인프라 3-2).
 *
 * TimeoutJobTest로 검증한 보장:
 *  - T1/T2 경계: 마지막 이벤트가 (now - threshold) 이전인 미종료 세션만 마감.
 *  - T3: 이미 종료된 세션은 조건(end_state IS NULL)에서 제외 → 안 건드림.
 *  - T4: ended_at = 마지막 이벤트 시각(실제 이탈 시점 근사).
 *  - T5 멱등: timeout된 세션은 더 이상 미종료가 아니므로 다음 실행에서 제외.
 *  - T6 경합: 조건부 UPDATE(WHERE end_state IS NULL)가 마감 직전 제출을 막는다.
 *           UPDATE 영향행 0이면 그 사이 상태가 바뀐 것 → 자동 스킵.
 */
@Component
public class SessionTimeoutJob {

    private final ResponseSessionTimeoutRepository repo;

    /** 무활동 임계(분). 설문 길이에 따라 조정. 기본 30분. */
    @Value("${pumasiform.session.timeout-minutes:30}")
    private long timeoutMinutes;

    public SessionTimeoutJob(ResponseSessionTimeoutRepository repo) {
        this.repo = repo;
    }

    /** 5분마다 실행. 너무 잦으면 부하, 너무 뜸하면 이탈 집계가 늦어짐. */
    @Scheduled(fixedDelayString = "PT5M")
    @Transactional
    public int closeStaleSessions() {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(timeoutMinutes));
        // 조건부 UPDATE 한 방으로 후보 선정 + 마감 + 경합 방어를 동시에 처리.
        // ended_at은 last_event_at(= 마지막 이벤트 시각)으로 설정(T4).
        return repo.closeTimedOut(cutoff);
    }
}
