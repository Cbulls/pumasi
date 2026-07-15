package com.pumasiform.events;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.Instant;

/**
 * 응답 세션 — 완료율의 분모. 상태 전이를 엔티티가 소유(풍부한 도메인 모델).
 * 측정인프라 V2 마이그레이션의 response_session 테이블에 대응.
 */
@Entity
@Table(name = "response_session",
       uniqueConstraints = @UniqueConstraint(columnNames = {"form_id", "anon_session_id"}))
public class ResponseSession {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "form_id", nullable = false)
    private String formId;

    @Column(name = "anon_session_id", nullable = false)
    private String anonSessionId;

    @Column(name = "experiment_arm")
    private String experimentArm;

    @Column(nullable = false)
    private boolean started = false;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "end_state")
    private String endState;        // submitted / abandoned / timeout

    @Column(name = "last_question_order")
    private Integer lastQuestionOrder;

    @Column(name = "last_event_at", nullable = false)
    private Instant lastEventAt = Instant.now();

    @Column(name = "duration_sec")
    private Integer durationSec;

    protected ResponseSession() { }

    public static ResponseSession start(String formId, String anonSessionId, String arm) {
        ResponseSession s = new ResponseSession();
        s.formId = formId;
        s.anonSessionId = anonSessionId;
        s.experimentArm = arm;
        return s;
    }

    public void markStarted() { this.started = true; }

    public void updateLastQuestionOrder(int order) {
        // R7: 순서 역전 방지 — 최대값만 유지
        if (this.lastQuestionOrder == null || order > this.lastQuestionOrder) {
            this.lastQuestionOrder = order;
        }
    }

    /**
     * last_event_at 갱신. LastEventAtTest로 검증한 E2/E3/E4 로직:
     *  - occurredMs 누락 → serverNow (E4)
     *  - 미래값(serverNow 초과) → serverNow로 클램프 (E3, 클라 시계 불신)
     *  - 더 최신일 때만 갱신 (E2, 순서 역전 방어)
     */
    public void touchLastEvent(Long occurredMs, Instant serverNow) {
        Instant ts;
        if (occurredMs == null) {
            ts = serverNow;                                   // E4
        } else {
            Instant occurred = Instant.ofEpochMilli(occurredMs);
            ts = occurred.isAfter(serverNow) ? serverNow : occurred;  // E3
        }
        if (this.lastEventAt == null || ts.isAfter(this.lastEventAt)) {  // E2
            this.lastEventAt = ts;
        }
    }

    public void markSubmitted() {
        this.endState = "submitted";
        this.endedAt = Instant.now();
        this.durationSec = (int) Duration.between(startedAt, endedAt).toSeconds();
    }

    public void markAbandoned(Integer lastOrder) {
        if (lastOrder != null) updateLastQuestionOrder(lastOrder);
        this.endState = "abandoned";
        this.endedAt = Instant.now();
    }

    public boolean isStarted() { return started; }
    public boolean isEnded() { return endState != null; }
    public Long getId() { return id; }
    public Integer getLastQuestionOrder() { return lastQuestionOrder; }
    public String getEndState() { return endState; }
    public java.time.Instant getLastEventAt() { return lastEventAt; }
}
