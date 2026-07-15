import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * 토큰 버킷 rate limiter. 키(IP 또는 세션)별로 독립 버킷.
 *
 * 설계 포인트:
 *  - 시간을 LongSupplier로 주입 → 테스트에서 결정론적으로 시간을 조작(실제 시계 비의존).
 *  - 키별 버킷은 ConcurrentHashMap. 버킷 내부 갱신은 synchronized로 원자적.
 *  - 분산 환경에선 이 인메모리 대신 Redis 토큰버킷으로 교체(인터페이스 동일).
 */
public class TokenBucketRateLimiter {

    private final double capacity;       // 최대 토큰 수
    private final double refillPerSec;   // 초당 보충량
    private final LongSupplier clockMs;  // 현재 시각(ms) 공급자
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public TokenBucketRateLimiter(double capacity, double refillPerSec, LongSupplier clockMs) {
        this.capacity = capacity;
        this.refillPerSec = refillPerSec;
        this.clockMs = clockMs;
    }

    private static class Bucket {
        double tokens;
        long lastRefillMs;
        Bucket(double tokens, long now) { this.tokens = tokens; this.lastRefillMs = now; }
    }

    /** 토큰 1개 획득 시도. 성공 true / 한도 초과 false */
    public boolean tryAcquire(String key) {
        long now = clockMs.getAsLong();
        Bucket b = buckets.computeIfAbsent(key, k -> new Bucket(capacity, now));
        synchronized (b) {
            // 경과 시간만큼 보충 (capacity 상한)
            double elapsedSec = (now - b.lastRefillMs) / 1000.0;
            if (elapsedSec > 0) {
                b.tokens = Math.min(capacity, b.tokens + elapsedSec * refillPerSec);
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
