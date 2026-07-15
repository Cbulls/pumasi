import java.util.*;

/**
 * 대용량 비동기 내보내기 작업(ExportJob) TDD.
 *
 * 대용량은 동기 다운로드 불가(OOM/타임아웃/동시성). 요청과 다운로드를 분리:
 *   요청 → 작업 생성(PENDING) → 백그라운드 생성(RUNNING) → COMPLETED(링크)/FAILED
 *   사용자는 작업 ID로 상태 폴링 후 완료 시 다운로드.
 *
 * 규칙:
 *  J1. 작업 생성 시 PENDING 상태 + 고유 jobId.
 *  J2. 상태 전이: PENDING→RUNNING→COMPLETED. 또는 RUNNING→FAILED.
 *  J3. ★중복 요청 합치기★ 같은 (form, user, 포맷, expand)로 진행 중(PENDING/RUNNING) 작업이
 *      있으면 새로 만들지 않고 기존 jobId 반환(중복 생성 방지 — 비싼 작업).
 *  J4. COMPLETED는 다운로드 URL + 만료시각을 가진다.
 *  J5. ★잘못된 전이 거부★ COMPLETED/FAILED에서 다른 상태로 못 감(종료 상태).
 *  J6. FAILED는 실패 사유를 가진다. 재요청은 새 작업 생성 허용(기존이 종료 상태이므로).
 *  J7. ★만료★ 완료 후 TTL 지나면 expired (다운로드 불가, 재생성 필요).
 */
public class ExportJobTest {
    static int pass = 0, fail = 0;
    static void check(String n, boolean c) {
        if (c) { pass++; System.out.println("  PASS " + n); }
        else { fail++; System.out.println("  FAIL " + n); }
    }

    public static void main(String[] args) {
        System.out.println("== J1: 작업 생성 → PENDING ==");
        {
            ExportJobManager mgr = new ExportJobManager(3600);  // TTL 1시간
            String jobId = mgr.requestExport("form1", "user1", "xlsx", false);
            check("jobId 발급", jobId != null && !jobId.isBlank());
            check("초기 상태 PENDING", mgr.getStatus(jobId).equals("PENDING"));
        }

        System.out.println("== J2: 상태 전이 ==");
        {
            ExportJobManager mgr = new ExportJobManager(3600);
            String jobId = mgr.requestExport("form1", "user1", "xlsx", false);
            mgr.markRunning(jobId);
            check("RUNNING", mgr.getStatus(jobId).equals("RUNNING"));
            mgr.markCompleted(jobId, "https://s3/exports/" + jobId + ".xlsx");
            check("COMPLETED", mgr.getStatus(jobId).equals("COMPLETED"));
        }

        System.out.println("== J3: 중복 요청 합치기 ==");
        {
            ExportJobManager mgr = new ExportJobManager(3600);
            String j1 = mgr.requestExport("form1", "user1", "xlsx", false);
            String j2 = mgr.requestExport("form1", "user1", "xlsx", false);  // 같은 조건
            check("진행 중 중복 → 같은 jobId", j1.equals(j2));
            // 다른 포맷은 별도 작업
            String j3 = mgr.requestExport("form1", "user1", "csv", false);
            check("다른 포맷 → 다른 jobId", !j1.equals(j3));
            // 다른 expand도 별도
            String j4 = mgr.requestExport("form1", "user1", "xlsx", true);
            check("다른 expand → 다른 jobId", !j1.equals(j4));
        }

        System.out.println("== J4: 완료 시 URL + 만료 ==");
        {
            ExportJobManager mgr = new ExportJobManager(3600);
            String jobId = mgr.requestExport("form1", "user1", "xlsx", false);
            mgr.markRunning(jobId);
            mgr.markCompleted(jobId, "https://s3/exports/x.xlsx");
            check("다운로드 URL 있음", mgr.getDownloadUrl(jobId) != null);
            check("만료시각 설정됨", mgr.getExpiresAt(jobId) > 0);
        }

        System.out.println("== J5: 잘못된 전이 거부 ==");
        {
            ExportJobManager mgr = new ExportJobManager(3600);
            String jobId = mgr.requestExport("form1", "user1", "xlsx", false);
            mgr.markRunning(jobId);
            mgr.markCompleted(jobId, "url");
            boolean rejected = false;
            try { mgr.markRunning(jobId); }   // COMPLETED → RUNNING 불가
            catch (IllegalStateException e) { rejected = true; }
            check("종료 상태에서 전이 거부", rejected);
        }

        System.out.println("== J6: 실패 + 재요청 ==");
        {
            ExportJobManager mgr = new ExportJobManager(3600);
            String j1 = mgr.requestExport("form1", "user1", "xlsx", false);
            mgr.markRunning(j1);
            mgr.markFailed(j1, "S3 upload error");
            check("FAILED 상태", mgr.getStatus(j1).equals("FAILED"));
            check("실패 사유 있음", mgr.getFailReason(j1).contains("S3"));
            // 종료된 작업이므로 재요청은 새 작업
            String j2 = mgr.requestExport("form1", "user1", "xlsx", false);
            check("실패 후 재요청 → 새 jobId", !j1.equals(j2));
        }

        System.out.println("== J7: 만료 ==");
        {
            ExportJobManager mgr = new ExportJobManager(0);  // TTL 0 = 즉시 만료
            String jobId = mgr.requestExport("form1", "user1", "xlsx", false);
            mgr.markRunning(jobId);
            mgr.markCompleted(jobId, "url", 0);  // 완료시각 0초 주입
            check("TTL 0 → 즉시 expired", mgr.isExpired(jobId, 1));  // now=1초
            check("만료된 작업 다운로드 불가", !mgr.isDownloadable(jobId, 1));
        }

        System.out.println("\n결과: " + pass + " pass / " + fail + " fail");
        if (fail > 0) System.exit(1);
    }
}
