/**
 * 안전한 세션 확보 — "insert-first, recover-on-conflict" 패턴.
 *
 * 전략:
 *   1) 먼저 조회 (대부분의 경우 이미 존재하므로 빠른 경로)
 *   2) 없으면 insert 시도
 *   3) insert가 UniqueViolation으로 실패 = 동시에 다른 스레드가 먼저 만듦
 *      → 재조회하면 이긴 쪽이 만든 행이 반드시 있다. 그걸 반환.
 *
 * 왜 이게 옳은가:
 *   - DB 유니크 제약이 "동시에 둘 다 INSERT 성공"을 원천 차단(직렬화)한다.
 *   - 진 쪽은 예외를 잡고 재조회 → 유실 없이 동일 세션 ID를 얻는다.
 *   - 락을 잡지 않으므로(낙관적) 빠르다. 경합은 최초 1회뿐이라 재시도 비용도 1회.
 *
 * 크레딧 동시성과의 대비:
 *   - 크레딧 예치 차감 = 핫 계정 반복 경쟁 → 비관적 락이 유리했다.
 *   - 세션 생성 = 단 한 번의 생성 경쟁 → insert-recover(낙관적)가 적합.
 *   같은 "동시성" 문제라도 경쟁의 성격이 다르면 해법도 다르다.
 */
class SafeResolver implements SessionResolver {
    private final FakeSessionTable table;
    SafeResolver(FakeSessionTable table) { this.table = table; }

    @Override
    public long resolve(String formId, String sessionId) {
        // 1) 빠른 경로: 이미 있으면 그대로
        Long existing = table.findId(formId, sessionId);
        if (existing != null) return existing;

        // 2) 없으면 insert 시도
        try {
            // NaiveResolver와 동일한 race window (공정 비교)
            try { Thread.sleep(0, 1); } catch (InterruptedException ignored) {}
            Thread.yield();
            return table.insert(formId, sessionId);
        } catch (UniqueViolationException conflict) {
            // 3) 경합에서 졌다 → 이긴 쪽이 만든 행을 재조회 (반드시 존재)
            Long winner = table.findId(formId, sessionId);
            if (winner != null) return winner;
            // 이론상 도달 불가(제약 위반인데 행이 없음). 방어적 재던짐.
            throw conflict;
        }
    }
}
