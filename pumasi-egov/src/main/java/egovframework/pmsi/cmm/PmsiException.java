package egovframework.pmsi.cmm;

import org.springframework.http.HttpStatus;

/**
 * 품앗이폼 업무 예외.
 *
 * 전자정부표준프레임워크의 EgovBizException(체크 예외) 대신, 스켈레톤에서는 HTTP 상태를
 * 함께 담는 언체크 예외를 사용해 GlobalExceptionHandler에서 일관되게 변환한다.
 * (표준 관례로 가려면 EgovAbstractServiceImpl.processException 으로 대체 가능)
 */
public class PmsiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public PmsiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static PmsiException badRequest(String code, String message) {
        return new PmsiException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static PmsiException notFound(String code, String message) {
        return new PmsiException(HttpStatus.NOT_FOUND, code, message);
    }

    public static PmsiException forbidden(String code, String message) {
        return new PmsiException(HttpStatus.FORBIDDEN, code, message);
    }

    public static PmsiException conflict(String code, String message) {
        return new PmsiException(HttpStatus.CONFLICT, code, message);
    }

    public static PmsiException paymentRequired(String code, String message) {
        return new PmsiException(HttpStatus.PAYMENT_REQUIRED, code, message);
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
}
