package egovframework.pmsi.credit.service.impl;

/**
 * 정산 금액 계산 — 순수 로직(프레임워크 무관, 결정론적).
 *
 * 설계 결정(마스터 문서 D5): 지급률 80% 지급 / 20% 소각.
 *   - reward = floor(cost * payoutRate)
 *   - burn   = cost - reward
 *
 * 최소비용(cost=1)이면 reward=0? → 지급이 0이면 응답 동기가 사라지므로,
 * cost>=1 이면 reward는 최소 1을 보장한다(문서 예: cost=1 → reward=1, burn=0).
 */
public final class SettlementCalc {

    public static final double DEFAULT_PAYOUT_RATE = 0.8;

    public record Settlement(long cost, long reward, long burn) {}

    private SettlementCalc() {}

    public static Settlement compute(long cost) {
        return compute(cost, DEFAULT_PAYOUT_RATE);
    }

    public static Settlement compute(long cost, double payoutRate) {
        if (cost < 0) throw new IllegalArgumentException("cost must be >= 0: " + cost);
        if (payoutRate < 0 || payoutRate > 1) {
            throw new IllegalArgumentException("payoutRate must be in [0,1]: " + payoutRate);
        }
        long reward = (long) Math.floor(cost * payoutRate);
        if (cost >= 1 && reward < 1) {
            reward = 1;   // 최소 지급 보장
        }
        long burn = cost - reward;
        return new Settlement(cost, reward, burn);
    }
}
