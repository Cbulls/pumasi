package egovframework.pmsi.cmm.web;

import egovframework.pmsi.cmm.PmsiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 전역 예외 → HTTP 응답 변환.
 *
 * - PmsiException            : 업무 예외의 지정 상태 코드/메시지
 * - MethodArgumentNotValid   : Bean Validation 실패 → 400 (필드 메시지 요약)
 * - DuplicateKeyException    : DB 유니크 위반(1인 1회 / 멱등 원장) → 409
 * - IllegalArgumentException : 잘못된 입력 → 400
 * - 그 외                    : 500. 내부 예외 메시지는 응답에 노출하지 않고 서버 로그로만 남긴다
 *                              (스택/SQL/PII 등 민감 정보 유출 방지).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PmsiException.class)
    public ResponseEntity<Map<String, Object>> handlePmsi(PmsiException e) {
        return body(e.getStatus(), e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(" / "));
        return body(HttpStatus.BAD_REQUEST, "validation", msg.isBlank() ? "입력값이 올바르지 않습니다." : msg);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> handleMissingHeader(MissingRequestHeaderException e) {
        return body(HttpStatus.BAD_REQUEST, "missing_header", "필수 헤더가 없습니다: " + e.getHeaderName());
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
        // 내부 상세는 서버 로그로만. 클라이언트에는 고정 메시지만 반환.
        log.error("Unhandled exception", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "internal", "서버 오류가 발생했습니다.");
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
