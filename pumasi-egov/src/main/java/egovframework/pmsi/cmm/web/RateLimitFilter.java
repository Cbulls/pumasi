package egovframework.pmsi.cmm.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 쓰기 요청(POST) 남용 방지 — 인메모리 토큰버킷 rate limit.
 *
 * "운영 보강" 모듈의 TokenBucketRateLimiter 로직을 필터로 포팅.
 *  - 키: 로그인 등 인증 경로(/pmsi/auth/**)는 원격 IP — 재로그인으로 새 토큰을 받아
 *        버킷을 우회하는 것을 차단한다. 그 외는 Authorization 토큰, 없으면 원격 IP.
 *        (위조 가능한 X-User-Id 헤더는 키로 쓰지 않는다)
 *  - 버킷: 용량 20, 초당 2개 보충(순간 버스트 허용 + 지속 남용 차단)
 *  - 초과 시 429.
 *
 * 분산 환경에서는 이 인메모리 버킷 대신 Redis 토큰버킷으로 교체한다(범위 밖).
 */
@Component
@Order(2)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final double CAPACITY = 20.0;
    private static final double REFILL_PER_SEC = 2.0;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private static final class Bucket {
        double tokens;
        long lastRefillMs;
        Bucket(double tokens, long now) { this.tokens = tokens; this.lastRefillMs = now; }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // 조회(GET 등)는 제한하지 않고 상태 변경(POST/PUT/DELETE)만 제한
        if (!isWrite(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        if (!tryAcquire(keyOf(request))) {
            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"status\":429,\"code\":\"rate_limited\",\"message\":\"요청이 너무 잦습니다. 잠시 후 다시 시도하세요.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isWrite(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }

    private String keyOf(HttpServletRequest req) {
        // 인증 경로는 IP 기준: 로그인을 반복해 새 토큰 버킷을 만드는 우회를 차단
        if (req.getRequestURI().startsWith("/pmsi/auth/")) {
            return "ip:" + req.getRemoteAddr();
        }
        String auth = req.getHeader("Authorization");
        if (auth != null && !auth.isBlank()) return "tok:" + auth;
        return "ip:" + req.getRemoteAddr();
    }

    private boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        Bucket b = buckets.computeIfAbsent(key, k -> new Bucket(CAPACITY, now));
        synchronized (b) {
            double elapsedSec = (now - b.lastRefillMs) / 1000.0;
            if (elapsedSec > 0) {
                b.tokens = Math.min(CAPACITY, b.tokens + elapsedSec * REFILL_PER_SEC);
                b.lastRefillMs = now;
            }
            if (b.tokens >= 1.0) {
                b.tokens -= 1.0;
                return true;
            }
            return false;
        }
    }
}
