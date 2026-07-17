package egovframework.pmsi.result.service;

/** CSV 본문 + 다운로드용 파일명(확장자 제외). */
public record CsvExport(String body, String fileBaseName) {}
