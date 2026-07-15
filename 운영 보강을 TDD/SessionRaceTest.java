import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 세션 생성 경합 TDD.
 *
 * 시나리오: 같은 (formId, sessionId)로 N개의 최초 이벤트가 동시에 도착.
 *   - 순진한 find-or-create: 여러 스레드가 "없음"을 보고 동시에 INSERT → 경합.
 *   - 기대(올바른 구현): 세션은 정확히 1개만 생성되고, 모든 이벤트가 유실 없이 처리.
 *
 * DB를 모사하는 FakeSessionTable:
 *   - (formId, sessionId) 유니크 제약을 실제로 강제 (중복 INSERT는 예외)
 *   - insert는 원자적(putIfAbsent)
 * 이렇게 해야 "제약 위반 시 재조회" 같은 해법을 진짜로 검증할 수 있다.
 */
public class SessionRaceTest {

    static int pass = 0, fail = 0;
    static void check(String n, boolean c) {
        if (c) { pass++; System.out.println("  PASS " + n); }
        else { fail++; System.out.println("  FAIL " + n); }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("== 경합: 같은 세션키로 64개 최초 이벤트 동시 도착 ==");

        for (String impl : new String[]{"NAIVE", "SAFE"}) {
            FakeSessionTable table = new FakeSessionTable();
            SessionResolver resolver = impl.equals("NAIVE")
                    ? new NaiveResolver(table)
                    : new SafeResolver(table);

            int n = 64;
            ExecutorService pool = Executors.newFixedThreadPool(32);
            CountDownLatch start = new CountDownLatch(1);
            AtomicInteger failures = new AtomicInteger();
            List<Future<Long>> futures = new ArrayList<>();

            for (int i = 0; i < n; i++) {
                futures.add(pool.submit(() -> {
                    try { start.await(); } catch (InterruptedException ignored) {}
                    try {
                        return resolver.resolve("F1", "sess-A");
                    } catch (Exception e) {
                        failures.incrementAndGet();   // 처리 실패 = 이벤트 유실
                        return -1L;
                    }
                }));
            }
            start.countDown();

            Set<Long> ids = new HashSet<>();
            for (Future<Long> f : futures) {
                long id = f.get();
                if (id > 0) ids.add(id);
            }
            pool.shutdown();

            int sessionsCreated = table.rowCount();
            System.out.printf("  [%s] 생성된 세션=%d, 처리실패=%d, 고유ID수=%d%n",
                    impl, sessionsCreated, failures.get(), ids.size());

            if (impl.equals("NAIVE")) {
                // 순진한 구현은 깨져야 정상(중복 생성 또는 처리 실패 발생)
                check("NAIVE는 경합으로 깨진다(세션>1 또는 실패>0)",
                        sessionsCreated > 1 || failures.get() > 0);
            } else {
                // 올바른 구현: 세션 정확히 1개, 처리 실패 0, 모두 같은 ID
                check("SAFE: 세션 정확히 1개", sessionsCreated == 1);
                check("SAFE: 처리 실패 0건", failures.get() == 0);
                check("SAFE: 모든 요청이 같은 세션 ID 획득", ids.size() == 1);
            }
        }

        System.out.println("\n결과: " + pass + " pass / " + fail + " fail");
        if (fail > 0) System.exit(1);
    }
}
