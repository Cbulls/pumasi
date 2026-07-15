import java.util.*;

/**
 * 출력 싱크. 구현체가 CSV writer / POI SXSSFWorkbook / S3 멀티파트 업로드 등.
 * 스트리밍 내보내기는 이 인터페이스에만 의존(출력 대상 추상화).
 */
interface ExportSink {
    void writeHeader(List<String> header);
    void writeRow(String row);
    void onProgress(int processed, int total);
    void finish();
}

/**
 * 스트리밍 내보내기. 전체를 메모리에 올리지 않고 페이지 단위로 흘려보낸다.
 *
 * 핵심: 페이저에서 한 페이지(≤pageSize)를 받아 즉시 싱크에 쓰고 버린다. 다음 페이지를
 * 받을 때 이전 페이지는 GC 대상. → 메모리는 pageSize 한 페이지 분량으로 상한.
 *
 * 실제 구현 매핑:
 *   - 페이저: DB 페이지네이션(LIMIT/OFFSET 또는 keyset). WHERE로 필터.
 *   - 싱크(CSV): BufferedWriter로 한 줄씩 → S3 멀티파트 업로드.
 *   - 싱크(XLSX): SXSSFWorkbook(윈도 100행만 메모리 유지, 나머지 디스크 flush).
 */
public class StreamingExporter {

    /**
     * @param pager  nextPage()가 빈 리스트를 줄 때까지 반복(DB 페이지네이션)
     * @param pageSize 한 청크 최대 행(메모리 상한 기준)
     */
    void export(List<String> header, FakeResponsePager pager, ExportSink sink, int pageSize) {
        sink.writeHeader(header);   // S3: 헤더 한 번만(첫 청크 전)

        int processed = 0;
        int total = -1;   // 전체 수를 모르면 -1(진행률은 처리량으로만)
        while (true) {
            List<String> page = pager.nextPage();   // S1: 한 페이지씩
            if (page.isEmpty()) break;               // S5: 빈 결과면 루프 진입 안 함
            for (String row : page) {
                sink.writeRow(row);                  // S4: 받은 만큼 즉시 쓰고 버림
                processed++;
            }
            sink.onProgress(processed, total);       // S6: 청크마다 진행률
            // page는 다음 반복에서 새 리스트로 교체 → 이전 page는 GC 대상(S2)
        }
        sink.finish();
    }
}
