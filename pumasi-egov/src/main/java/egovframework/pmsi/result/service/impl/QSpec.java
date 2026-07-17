package egovframework.pmsi.result.service.impl;

import java.util.List;

/** 질문 스펙(집계 기준). 폼의 QuestionVO에서 필요한 필드만 추린 값 객체. */
class QSpec {
    String id;
    String type;
    List<String> optionIds = List.of();   // 선택형 보기 라벨(순서 보존)
    List<String> rowLabels = List.of();   // 그리드 행 라벨
    Integer scaleMin;
    Integer scaleMax;
}
