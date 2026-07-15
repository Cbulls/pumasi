package com.pumasiform.events;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이벤트 적재 서비스.
 *
 * 컨트롤러가 검증을 통과시킨 이벤트를 받아 세션 상태를 갱신하고 원본 이벤트를 append한다.
 * 앞서 순수 Java로 TDD 검증한 전이 규칙(R3~R7)을 DB 위에서 재현한다:
 *  - survey_started: 세션이 없으면 생성, 있으면 무시(멱등) — 완료율 분모 정확성
 *  - question_answered: last_question_order를 MAX로만 갱신(순서 역전 방지, R7)
 *  - survey_submitted: 세션 종료(end_state=submitted)
 *  - survey_abandoned: started & 미종료일 때만 종료 처리
 *
 * @Async: 응답자 페이지를 막지 않도록 비동기. 컨트롤러는 이미 202를 반환한 뒤다.
 */
@Service
public class EventIngestService {

    private final ResponseSessionRepository sessionRepo;
    private final SurveyEventRepository eventRepo;

    public EventIngestService(ResponseSessionRepository sessionRepo,
                              SurveyEventRepository eventRepo) {
        this.sessionRepo = sessionRepo;
        this.eventRepo = eventRepo;
    }

    @Async
    @Transactional
    public void ingest(EventRequest req) {
        // 1) 세션 확보 (없으면 생성). (form_id, session_id) 유니크로 동시성 안전.
        ResponseSession session = sessionRepo
            .findByFormIdAndAnonSessionId(req.formId(), req.sessionId())
            .orElseGet(() -> sessionRepo.save(
                ResponseSession.start(req.formId(), req.sessionId(), req.experimentArm())));

        // 2) 전이 규칙 적용
        boolean recordEvent = applyTransition(session, req);

        // 3) 세션 상태 변경 영속화
        sessionRepo.save(session);

        // 4) 원본 이벤트 append (recordEvent=false면 멱등 무시된 이벤트라 기록 생략)
        if (recordEvent) {
            eventRepo.save(SurveyEvent.of(session.getId(), req));
        }
    }

    /** 전이 규칙. 기록 대상이면 true. (EventService TDD 로직과 동일 의미) */
    private boolean applyTransition(ResponseSession s, EventRequest req) {
        switch (req.eventType()) {
            case "survey_viewed":
                return true;

            case "survey_started":
                if (s.isStarted()) return false;     // R3 멱등
                s.markStarted();
                return true;

            case "question_answered":
                if (!s.isStarted()) s.markStarted();
                if (req.questionOrder() != null) {
                    s.updateLastQuestionOrder(req.questionOrder());  // R7 MAX 유지
                }
                return true;

            case "survey_submitted":
                s.markSubmitted();
                return true;

            case "survey_abandoned":
                if (!s.isStarted() || s.isEnded()) return false;   // R4
                s.markAbandoned(req.questionOrder());
                return true;

            default:
                return false;   // @Pattern이 막지만 방어적으로
        }
    }
}
