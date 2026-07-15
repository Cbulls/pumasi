package egovframework.pmsi.cmm.web;

import egovframework.pmsi.cmm.PmsiException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 전역 예외 → HTTP 응답 변환.
 *
 * - PmsiException        : 업무 예외의 지정 상태 코드
 * - DuplicateKeyException: DB 유니크 위반(1인 1회 / 멱등 원장) → 409
 * - IllegalArgumentException: 잘못된 입력 → 400
 * - 그 외                : 500
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PmsiException.class)
    public ResponseEntity<Map<String, Object>> handlePmsi(PmsiException e) {
        return body(e.getStatus(), e.getCode(), e.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateKeyException e) {
        return body(HttpStatus.CONFLICT, "duplicate", "이미 처리된 요청입니다.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegal(IllegalArgumentException e) {
        return body(HttpStatus.BAD_REQUEST, "invalid", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception e) {
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "internal", e.getMessage());
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", OffsetDateTime.now().toString(),
                "status", status.value(),
                "code", code,
                "message", message == null ? "" : message
        ));
    }
}
