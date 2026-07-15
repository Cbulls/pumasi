/**
 * 순진한 find-or-create. 현재 EventIngestService.ingest의 구조와 동일:
 *   세션을 찾고(find), 없으면 만든다(create).
 *
 * 문제: find와 insert 사이에 다른 스레드가 끼어들면(check-then-act),
 *   둘 다 "없음"을 보고 둘 다 insert를 시도 → 한쪽은 UniqueViolation으로 실패.
 *   = 크레딧에서 본 lost update와 같은 뿌리의 경합.
 */
class NaiveResolver implements SessionResolver {
    private final FakeSessionTable table;
    NaiveResolver(FakeSessionTable table) { this.table = table; }

    @Override
    public long resolve(String formId, String sessionId) {
        Long existing = table.findId(formId, sessionId);   // CHECK
        if (existing != null) return existing;
        // ── race window: 실제 DB는 find와 insert 사이에 네트워크 왕복 + 트랜잭션
        //    처리 지연이 있다. 그 지연을 모사해야 경합이 재현된다. ──
        try { Thread.sleep(0, 1); } catch (InterruptedException ignored) {}
        Thread.yield();
        return table.insert(formId, sessionId);            // ACT (UniqueViolation 가능)
    }
}
