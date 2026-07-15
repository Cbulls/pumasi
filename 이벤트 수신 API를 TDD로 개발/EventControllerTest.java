package com.pumasiform.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 이벤트 수신 API의 HTTP 계약 테스트 (TDD: 컨트롤러 구현 전에 작성).
 *
 * 검증:
 *  C1. 정상 이벤트 → 202 Accepted (수신 확인, 비동기 처리 의미)
 *  C2. 필수 필드 누락 → 400 Bad Request
 *  C3. 알 수 없는 eventType → 400
 *  C4. 익명 세션(인증 헤더 없음)도 허용 → 202
 *  C5. 수신된 이벤트는 서비스로 위임된다 (verify)
 */
@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean EventIngestService service;

    @Test
    void valid_event_returns_202() throws Exception {
        var payload = Map.of("sessionId", "s1", "formId",
                "11111111-1111-1111-1111-111111111111", "eventType", "survey_started");
        mvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(payload)))
            .andExpect(status().isAccepted());
        verify(service, times(1)).ingest(any());   // C5
    }

    @Test
    void missing_sessionId_returns_400() throws Exception {
        var payload = Map.of("formId",
                "11111111-1111-1111-1111-111111111111", "eventType", "survey_started");
        mvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(payload)))
            .andExpect(status().isBadRequest());
        verify(service, never()).ingest(any());
    }

    @Test
    void unknown_event_type_returns_400() throws Exception {
        var payload = Map.of("sessionId", "s1", "formId",
                "11111111-1111-1111-1111-111111111111", "eventType", "hacker_event");
        mvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(payload)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void anonymous_session_allowed() throws Exception {
        var payload = Map.of("sessionId", "anon-xyz", "formId",
                "11111111-1111-1111-1111-111111111111", "eventType", "survey_viewed");
        mvc.perform(post("/api/events")   // 인증 헤더 없음
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(payload)))
            .andExpect(status().isAccepted());
    }
}
