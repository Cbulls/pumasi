package egovframework.pmsi.cmm.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 보안 응답 헤더 + 요청 크기 가드.
 *
 *  - X-Content-Type-Options: nosniff  (MIME 스니핑 차단)
 *  - X-Frame-Options: DENY            (클릭재킹 차단)
 *  - Referrer-Policy: no-referrer     (레퍼러로 인한 정보 유출 방지)
 *  - Cache-Control: no-store          (API 응답이 캐시에 남지 않도록 — PII 보호)
 *
 * 과도한 요청 본문(Content-Length > 256KB)은 413으로 조기 차단한다.
 */
@Component
@Order(1)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private static final long MAX_BODY_BYTES = 256 * 1024L;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Cache-Control", "no-store");

        long len = request.getContentLengthLong();
        if (len > MAX_BODY_BYTES) {
            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE); // 413
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"status\":413,\"code\":\"payload_too_large\",\"message\":\"요청 본문이 너무 큽니다.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
