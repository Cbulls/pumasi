package com.pumasiform.result.async;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 비동기 내보내기 서비스.
 *
 * 흐름(다이어그램과 일치):
 *   requestExport → 작업 생성(PENDING, 중복 합치기) → @Async generate() 트리거 → 즉시 jobId 반환
 *   generate(): markRunning → 스트리밍 생성 + 저장소 업로드 → markCompleted(URL) / markFailed
 *   클라이언트: statusDetail 폴링 → COMPLETED면 다운로드 URL 사용
 *
 * 검증된 순수 로직(ExportJobManager, StreamingExporter)을 DB/저장소 위에서 조율.
 * 작업 상태는 실제로는 DB(export_job) 또는 Redis에 둔다(여러 인스턴스가 공유).
 */
@Service
public class AsyncExportService {

    private final ExportJobStore jobStore;        // 작업 상태(ExportJobManager의 DB판)
    private final StoragePort storage;            // S3 등 저장소
    private final ResponsePagerFactory pagerFactory;  // 폼별 응답 페이저
    private final AsyncExportService self;        // @Async 자기호출 우회용(프록시)

    public AsyncExportService(ExportJobStore jobStore, StoragePort storage,
                              ResponsePagerFactory pagerFactory,
                              @org.springframework.context.annotation.Lazy AsyncExportService self) {
        this.jobStore = jobStore;
        this.storage = storage;
        this.pagerFactory = pagerFactory;
        this.self = self;
    }

    /** 요청. 중복 합치기 후 비동기 생성 트리거. */
    public String requestExport(String formId, String userId, String format, boolean expand) {
        String jobId = jobStore.findOrCreate(formId, userId, format, expand);
        if ("PENDING".equals(jobStore.getStatus(jobId))) {
            self.generate(jobId);   // @Async — 프록시 경유 자기호출(직접 호출하면 비동기 안 됨)
        }
        return jobId;
    }

    /**
     * 백그라운드 생성. 스트리밍으로 메모리 절약 + 저장소 업로드.
     * 실패 시 markFailed로 사유 기록(조용한 유실 방지 — 큐 유실방지와 같은 원칙).
     */
    @Async
    public void generate(String jobId) {
        try {
            jobStore.markRunning(jobId);
            ExportJobStore.JobInfo info = jobStore.info(jobId);

            // 스트리밍 싱크: 저장소 멀티파트 업로드로 흘려보냄(메모리 상한)
            try (StorageStreamingSink sink = storage.openStreamingSink(
                    "exports/" + jobId + "." + info.format())) {
                StreamingExporter exporter = new StreamingExporter();
                ResponsePager pager = pagerFactory.create(info.formId(), info.format(), info.expand());
                exporter.export(pager.header(), pager, sink, 1000);   // pageSize 1000
                String url = sink.publishedUrl();
                jobStore.markCompleted(jobId, url);
            }
        } catch (Exception e) {
            jobStore.markFailed(jobId, e.getClass().getSimpleName() + ": " + e.getMessage());
            // 실패는 조용히 묻지 않는다 — 상태로 노출되어 클라이언트가 재요청 가능.
        }
    }

    public String status(String jobId) { return jobStore.getStatus(jobId); }

    /** 폴링 응답 상세. 완료 시 URL, 실패 시 사유. */
    public Map<String, Object> statusDetail(String jobId, String userId) {
        jobStore.assertOwner(jobId, userId);
        String status = jobStore.getStatus(jobId);
        Map<String, Object> body = new HashMap<>();
        body.put("jobId", jobId);
        body.put("status", status);
        switch (status) {
            case "COMPLETED" -> {
                body.put("downloadUrl", jobStore.getDownloadUrl(jobId));
                body.put("expiresAt", jobStore.getExpiresAt(jobId));
            }
            case "FAILED" -> body.put("failReason", jobStore.getFailReason(jobId));
            default -> { /* PENDING/RUNNING: 상태만 */ }
        }
        return body;
    }
}
