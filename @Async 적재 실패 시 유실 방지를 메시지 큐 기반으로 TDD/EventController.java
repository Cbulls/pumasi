package com.pumasiform.events;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 이벤트 수신 API.
 *
 * POST /api/events
 *  - 정상: 202 Accepted (수신 확인. 실제 적재는 비동기 — 응답자 페이지를 막지 않음)
 *  - 검증 실패: 400 (GlobalExceptionHandler가 MethodArgumentNotValidException 처리)
 *
 * 설계 의도:
 *  - 202를 쓰는 이유: 이벤트 로깅은 응답자 경험을 절대 지연시키면 안 된다.
 *    수신만 확인하고 적재는 큐/비동기로 넘긴다. 200(처리완료)이 아니라 202(수신).
 *  - 익명 허용: 인증 인터셉터를 태우지 않는다(비로그인 응답자 추적).
 *  - sendBeacon이 보내는 Content-Type은 가변적이므로 consumes를 강제하지 않는다.
 */
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final com.pumasiform.events.queue.EventPublisher publisher;
    private final EventRateLimiter rateLimiter;

    public EventController(com.pumasiform.events.queue.EventPublisher publisher,
                           EventRateLimiter rateLimiter) {
        this.publisher = publisher;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping
    public ResponseEntity<Void> receive(@Valid @RequestBody EventRequest req) {
        // rate limit: 세션 키 기준(익명 허용 엔드포인트 보호). 초과 시 429.
        if (!rateLimiter.tryAcquire(req.sessionId())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        try {
            // Q5: 큐 publish가 성공한 뒤에만 202. 실패하면 거짓 수신확인 대신 503 →
            //     클라이언트(sendBeacon/재시도)가 다시 보낸다. 적재는 컨슈머가 비동기로.
            publisher.publish(toMessage(req));
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        } catch (RuntimeException brokerError) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    private com.pumasiform.events.queue.EventMessage toMessage(EventRequest r) {
        // messageId: 멱등 키. 클라이언트 idempotency key가 없으면 서버가 생성.
        String messageId = java.util.UUID.randomUUID().toString();
        return new com.pumasiform.events.queue.EventMessage(
            messageId, r.formId(), r.sessionId(), r.eventType(),
            r.questionId(), r.questionOrder(), r.experimentArm(), r.occurredAt());
    }
}
