/**
 * 품앗이폼 측정 인프라 — 클라이언트 이벤트 트래커
 *
 * 핵심 주의점:
 *  - 이탈(abandoned)은 페이지가 닫히는 순간에 보내야 한다. 일반 fetch()는 페이지 unload 시
 *    취소되므로, navigator.sendBeacon()을 써야 전송이 보장된다.
 *  - survey_started는 "첫 상호작용" 시점에 한 번만. 완료율의 분모이므로 정확해야 한다.
 *  - 중복 전송 방지를 위해 started/submitted는 1회성 플래그로 가드.
 */
class SurveyTracker {
  constructor(formId, sessionId, apiBase = '/api/events') {
    this.formId = formId;
    this.sessionId = sessionId;
    this.apiBase = apiBase;
    this.started = false;
    this.submitted = false;
    this.lastQuestionOrder = null;
    this._bindAbandonment();
  }

  _post(payload) {
    // 일반 이벤트는 fetch (keepalive로 약간의 보장 추가)
    fetch(this.apiBase, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
      keepalive: true,
    }).catch(() => {});
  }

  _beacon(payload) {
    // 이탈 전송: sendBeacon은 페이지가 닫혀도 전송을 보장
    const blob = new Blob([JSON.stringify(payload)], { type: 'application/json' });
    const ok = navigator.sendBeacon(this.apiBase, blob);
    if (!ok) this._post(payload); // 폴백
  }

  viewed() {
    this._post({ sessionId: this.sessionId, formId: this.formId,
                 eventType: 'survey_viewed', occurredAt: Date.now() });
  }

  /** 첫 상호작용 시 1회만 — 완료율의 분모 */
  started_once() {
    if (this.started) return;
    this.started = true;
    this._post({ sessionId: this.sessionId, formId: this.formId,
                 eventType: 'survey_started', occurredAt: Date.now() });
  }

  questionAnswered(questionId, order) {
    this.started_once(); // 응답이 곧 시작
    this.lastQuestionOrder = order;
    this._post({ sessionId: this.sessionId, formId: this.formId,
                 eventType: 'question_answered',
                 questionId, questionOrder: order, occurredAt: Date.now() });
  }

  submitted_done() {
    this.submitted = true;
    this._post({ sessionId: this.sessionId, formId: this.formId,
                 eventType: 'survey_submitted', occurredAt: Date.now() });
  }

  /** 페이지 이탈 감지: 시작했지만 제출 안 한 채 떠나면 abandoned 비콘 */
  _bindAbandonment() {
    const onLeave = () => {
      if (this.started && !this.submitted) {
        this._beacon({ sessionId: this.sessionId, formId: this.formId,
                       eventType: 'survey_abandoned',
                       questionOrder: this.lastQuestionOrder, occurredAt: Date.now() });
        this.submitted = true; // 중복 비콘 방지
      }
    };
    // visibilitychange(hidden)가 모바일에서 더 신뢰도 높음. pagehide와 함께 사용.
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'hidden') onLeave();
    });
    window.addEventListener('pagehide', onLeave);
  }
}

// 사용 예:
//   const t = new SurveyTracker(formId, sessionId);
//   t.viewed();
//   input.addEventListener('focus', () => t.started_once());
//   onAnswer((qid, order) => t.questionAnswered(qid, order));
//   onSubmit(() => t.submitted_done());
