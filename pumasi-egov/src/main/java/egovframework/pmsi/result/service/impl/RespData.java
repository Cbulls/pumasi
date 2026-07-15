package egovframework.pmsi.result.service.impl;

import java.util.List;
import java.util.Map;

/** 응답 1건(집계 입력). answers: questionId → 값 목록(CHECKBOX는 여러 값). */
class RespData {
    String qualityFlag;
    Map<String, List<String>> answers;

    RespData(String qualityFlag, Map<String, List<String>> answers) {
        this.qualityFlag = qualityFlag;
        this.answers = answers;
    }
}
