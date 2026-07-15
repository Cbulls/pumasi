package com.pumasiform.events.batch;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

/**
 * 타임아웃 마감 리포지토리.
 *
 * 핵심: 조건부 UPDATE 한 방으로 "후보 선정 + 마감 + 경합 방어"를 원자적으로.
 *   WHERE end_state IS NULL  ← 미종료만(T3). 마감 직전 제출되면 이 조건에 안 걸려 스킵(T6).
 *   AND last_event_at < cutoff ← 임계 경계(T1/T2)
 *   SET ended_at = last_event_at ← 실제 이탈 시점 근사(T4)
 *
 * 별도 SELECT 후 개별 UPDATE 하지 않는 이유: 그 사이 상태가 바뀌는 경합(T6)을 피하려면
 * 단일 조건부 UPDATE가 가장 안전하고 효율적이다(크레딧/세션 경합과 같은 원칙).
 *
 * 주: last_event_at은 세션의 마지막 이벤트 시각. 이벤트 적재 시 함께 갱신해 두거나
 *     survey_event의 max(occurred_at)로 산출. 여기선 컬럼으로 둔다고 가정.
 */
public interface ResponseSessionTimeoutRepository
        extends JpaRepository<Object, Long> {

    @Modifying
    @Query(value = """
        UPDATE response_session
        SET end_state = 'timeout',
            ended_at  = last_event_at,
            duration_sec = CAST(EXTRACT(EPOCH FROM (last_event_at - started_at)) AS int)
        WHERE end_state IS NULL
          AND last_event_at < :cutoff
        """, nativeQuery = true)
    int closeTimedOut(@Param("cutoff") Instant cutoff);
}
