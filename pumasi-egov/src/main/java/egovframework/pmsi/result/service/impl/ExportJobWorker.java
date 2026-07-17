package egovframework.pmsi.result.service.impl;

import egovframework.pmsi.cmm.storage.StorageClient;
import egovframework.pmsi.result.service.CsvExport;
import egovframework.pmsi.result.service.ResultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;

/**
 * 비동기 내보내기 워커 — 큰 폼도 요청 스레드를 붙잡지 않고 CSV를 생성해 스토리지에 저장.
 *
 * self-invocation 프록시 문제를 피하려고 컨트롤러와 분리된 빈으로 둔다.
 */
@Component("exportJobWorker")
public class ExportJobWorker {

    private static final Logger log = LoggerFactory.getLogger(ExportJobWorker.class);

    @Resource(name = "exportJobDAO")
    private ExportJobDAO exportJobDAO;

    @Resource(name = "resultService")
    private ResultService resultService;

    @Resource(name = "storageClient")
    private StorageClient storage;

    @Async
    public void run(String jobId, String formId, String ownerId) {
        try {
            exportJobDAO.markRunning(jobId);
            CsvExport export = resultService.exportCsv(formId, ownerId);
            byte[] bytes = export.body().getBytes(StandardCharsets.UTF_8);
            String key = formId + "/exports/" + jobId + ".csv";
            storage.put(key, bytes, "text/csv");
            exportJobDAO.markDone(jobId, key, export.fileBaseName() + ".csv");
        } catch (Exception e) {
            log.error("export job 실패: jobId={}", jobId, e);
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            exportJobDAO.markFailed(jobId, msg.length() > 480 ? msg.substring(0, 480) : msg);
        }
    }
}
