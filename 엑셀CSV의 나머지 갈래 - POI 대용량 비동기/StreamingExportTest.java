import java.util.*;

/**
 * 스트리밍 내보내기 TDD. 전체를 메모리에 안 올리고 청크 단위로 흘려보낸다.
 *
 * 규칙:
 *  S1. 응답을 페이지(청크) 단위로 가져와 받은 만큼 즉시 쓰고 버린다(메모리 상한).
 *  S2. ★전체를 한 번에 메모리에 올리지 않음★ — 가장 큰 청크도 pageSize 이하.
 *  S3. 헤더는 한 번만(첫 청크 전에).
 *  S4. 모든 응답이 빠짐없이 출력(청크 경계에서 누락 없음).
 *  S5. 빈 결과도 헤더는 출력(0건이어도 파일은 유효).
 *  S6. 진행률 추적(처리한 행 수 / 전체) — 폴링 시 사용자에게 보여줄 수 있게.
 */
public class StreamingExportTest {
    static int pass=0, fail=0;
    static void check(String n, boolean c){ if(c){pass++;System.out.println("  PASS "+n);}else{fail++;System.out.println("  FAIL "+n);} }

    public static void main(String[] a){
        System.out.println("== S1/S2/S4: 청크 스트리밍 + 누락 없음 ==");
        {
            // 전체 250건, 페이지 100 → 3청크(100,100,50)
            FakeResponsePager pager = new FakeResponsePager(250, 100);
            CountingSink sink = new CountingSink();
            StreamingExporter ex = new StreamingExporter();
            ex.export(List.of("제출시각","답"), pager, sink, 100);
            check("전체 250건 출력", sink.dataRows == 250);   // S4
            check("최대 청크 ≤ 100", pager.maxChunkSeen <= 100); // S2
            check("3개 청크로 처리", pager.chunkCount == 3);   // S1
        }

        System.out.println("== S3: 헤더 한 번만 ==");
        {
            FakeResponsePager pager = new FakeResponsePager(250, 100);
            CountingSink sink = new CountingSink();
            new StreamingExporter().export(List.of("h1","h2"), pager, sink, 100);
            check("헤더 1회만 출력", sink.headerCount == 1);
        }

        System.out.println("== S5: 빈 결과도 헤더 ==");
        {
            FakeResponsePager pager = new FakeResponsePager(0, 100);
            CountingSink sink = new CountingSink();
            new StreamingExporter().export(List.of("h1"), pager, sink, 100);
            check("0건이어도 헤더 출력", sink.headerCount == 1);
            check("데이터 0행", sink.dataRows == 0);
        }

        System.out.println("== S6: 진행률 추적 ==");
        {
            FakeResponsePager pager = new FakeResponsePager(250, 100);
            ProgressSink sink = new ProgressSink();
            new StreamingExporter().export(List.of("h"), pager, sink, 100);
            check("최종 진행 250", sink.lastProgress == 250);
            check("진행률 단조 증가", sink.monotonic);
        }

        System.out.println("\n결과: "+pass+" pass / "+fail+" fail");
        if(fail>0) System.exit(1);
    }
}

/** 페이지 단위로 응답을 주는 페이저(DB 페이지네이션 모사) */
class FakeResponsePager {
    private final int total, pageSize;
    private int offset = 0;
    int chunkCount = 0;
    int maxChunkSeen = 0;
    FakeResponsePager(int total, int pageSize){ this.total=total; this.pageSize=pageSize; }
    /** 다음 페이지 반환. 빈 리스트면 끝. */
    List<String> nextPage(){
        if (offset >= total) return List.of();
        int n = Math.min(pageSize, total - offset);
        List<String> page = new ArrayList<>();
        for (int i=0;i<n;i++) page.add("row"+(offset+i));
        offset += n;
        chunkCount++;
        maxChunkSeen = Math.max(maxChunkSeen, n);
        return page;
    }
}

/** 출력 싱크(행 수만 카운트) */
class CountingSink implements ExportSink {
    int headerCount=0, dataRows=0;
    public void writeHeader(List<String> h){ headerCount++; }
    public void writeRow(String row){ dataRows++; }
    public void onProgress(int processed, int total){}
    public void finish(){}
}

/** 진행률 추적 싱크 */
class ProgressSink implements ExportSink {
    int lastProgress=0; boolean monotonic=true;
    public void writeHeader(List<String> h){}
    public void writeRow(String row){}
    public void onProgress(int processed, int total){
        if (processed < lastProgress) monotonic=false;
        lastProgress = processed;
    }
    public void finish(){}
}
