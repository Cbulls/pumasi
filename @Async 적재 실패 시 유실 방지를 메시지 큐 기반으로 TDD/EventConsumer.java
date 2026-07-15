/**
 * 큐 컨슈머. 메시지를 받아 이벤트 스토어에 적재한다.
 *
 * 멱등 소비:
 *  at-least-once 큐에서는 같은 메시지가 두 번 올 수 있다. messageId로 이미 처리됐는지
 *  확인해 중복 적재를 막는다. (스토어의 saveIfAbsent가 DB 유니크 제약으로 최종 방어)
 *
 * 실패 시 예외를 던진다 = 브로커가 nack로 간주해 재시도/DLQ 처리.
 */
class EventConsumer {
    private final FakeEventStore store;

    EventConsumer(FakeEventStore store) { this.store = store; }

    void handle(EventMessage m) {
        // 멱등 1차: 이미 처리한 messageId면 skip (빠른 경로)
        if (store.alreadyProcessed(m.messageId())) {
            return;
        }
        // 적재. 실패하면 예외 → 브로커가 재시도. 성공하면 messageId 기록.
        store.save(m);   // 여기서 RuntimeException 가능 (DB 오류 모사)
    }
}
