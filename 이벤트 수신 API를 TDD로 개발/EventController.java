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

    private final EventIngestService service;

    public EventController(EventIngestService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Void> receive(@Valid @RequestBody EventRequest req) {
        service.ingest(req);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
