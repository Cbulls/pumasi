import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 비동기 내보내기 작업 관리자. ExportJob의 생애주기와 정책을 담는다.
 *
 * 상태 기계:
 *   PENDING → RUNNING → COMPLETED   (정상)
 *                     → FAILED      (실패)
 *   COMPLETED/FAILED = 종료 상태(더 이상 전이 불가).
 *
 * 정책:
 *   - 중복 합치기: 같은 (form,user,format,expand)로 진행 중 작업이 있으면 재사용.
 *   - 만료: 완료 후 TTL 지나면 다운로드 불가.
 *
 * 실제 구현에선 이 상태를 DB(export_job 테이블) 또는 Redis에 둔다. 여기선 인메모리.
 */
public class ExportJobManager {

    enum State { PENDING, RUNNING, COMPLETED, FAILED }

    static class Job {
        String id;
        String formId, userId, format;
        boolean expand;
        State state = State.PENDING;
        String downloadUrl;
        String failReason;
        long completedAtSec;    // 완료 시각(만료 계산용)
    }

    private final long ttlSec;
    private final Map<String, Job> jobs = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(1000);

    public ExportJobManager(long ttlSec) { this.ttlSec = ttlSec; }

    private String dedupeKey(String form, String user, String fmt, boolean expand) {
        return form + "|" + user + "|" + fmt + "|" + expand;
    }

    /**
     * 내보내기 요청. J3: 같은 조건으로 진행 중(PENDING/RUNNING) 작업이 있으면 그 jobId 반환.
     */
    public synchronized String requestExport(String formId, String userId,
                                             String format, boolean expand) {
        String key = dedupeKey(formId, userId, format, expand);
        // 진행 중 작업 찾기
        for (Job j : jobs.values()) {
            if (dedupeKey(j.formId, j.userId, j.format, j.expand).equals(key)
                    && (j.state == State.PENDING || j.state == State.RUNNING)) {
                return j.id;   // 중복 합치기
            }
        }
        Job job = new Job();
        job.id = "job-" + seq.incrementAndGet();
        job.formId = formId; job.userId = userId; job.format = format; job.expand = expand;
        job.state = State.PENDING;
        jobs.put(job.id, job);
        return job.id;
    }

    public void markRunning(String jobId) { transition(jobId, State.RUNNING); }

    public void markCompleted(String jobId, String downloadUrl) {
        markCompleted(jobId, downloadUrl, nowSec());
    }

    /** 완료 시각 주입 버전(테스트 결정론). 만료 계산이 시스템 시계에 의존하지 않게. */
    public void markCompleted(String jobId, String downloadUrl, long completedAtSec) {
        Job j = require(jobId);
        transition(jobId, State.COMPLETED);
        j.downloadUrl = downloadUrl;
        j.completedAtSec = completedAtSec;
    }

    public void markFailed(String jobId, String reason) {
        Job j = require(jobId);
        transition(jobId, State.FAILED);
        j.failReason = reason;
    }

    /** 상태 전이 검증. J2/J5: 종료 상태에서는 전이 불가. */
    private void transition(String jobId, State to) {
        Job j = require(jobId);
        State from = j.state;
        boolean ok = switch (from) {
            case PENDING -> to == State.RUNNING;
            case RUNNING -> to == State.COMPLETED || to == State.FAILED;
            case COMPLETED, FAILED -> false;   // 종료 상태
        };
        if (!ok) throw new IllegalStateException("invalid transition " + from + " → " + to);
        j.state = to;
    }

    public String getStatus(String jobId) { return require(jobId).state.name(); }
    public String getDownloadUrl(String jobId) { return require(jobId).downloadUrl; }
    public String getFailReason(String jobId) { return require(jobId).failReason; }
    public long getExpiresAt(String jobId) {
        Job j = require(jobId);
        return j.completedAtSec > 0 ? j.completedAtSec + ttlSec : 0;
    }

    /** J7: 만료 판정. nowSec 기준. */
    public boolean isExpired(String jobId, long nowSec) {
        Job j = require(jobId);
        if (j.state != State.COMPLETED) return false;
        return nowSec >= j.completedAtSec + ttlSec;
    }

    /** 다운로드 가능 = COMPLETED && 미만료 */
    public boolean isDownloadable(String jobId, long nowSec) {
        Job j = require(jobId);
        return j.state == State.COMPLETED && !isExpired(jobId, nowSec);
    }

    private Job require(String jobId) {
        Job j = jobs.get(jobId);
        if (j == null) throw new NoSuchElementException("job not found: " + jobId);
        return j;
    }
    private long nowSec() { return System.currentTimeMillis() / 1000; }
}
