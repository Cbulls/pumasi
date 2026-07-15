import java.util.*;

/**
 * 인메모리 큐 브로커 모사. Kafka/RabbitMQ를 띄우지 않고 컨슈머 로직을 검증하기 위함.
 *
 * 모사하는 큐 의미론:
 *  - at-least-once: ack 전까지 메시지는 살아있고, nack되면 재배달.
 *  - 재시도 카운트: 메시지별 시도 횟수 추적. maxAttempts 초과 시 DLQ로 이동.
 *  - DLQ: 독약 메시지(영구 실패)를 격리. 메인 큐는 계속 흐른다.
 *
 * 실제 브로커와의 차이(의도적 단순화):
 *  - 가시성 타임아웃/오프셋 커밋 같은 세부는 생략. 핵심인 재시도·DLQ·전달보장만 모사.
 */
class InMemoryBroker {
    private final Deque<EventMessage> main = new ArrayDeque<>();
    private final List<EventMessage> dlq = new ArrayList<>();
    private final Map<String, Integer> attempts = new HashMap<>();
    private final int maxAttempts;

    InMemoryBroker(int maxAttempts) { this.maxAttempts = maxAttempts; }

    void publish(EventMessage m) { main.addLast(m); }

    int mainQueueSize() { return main.size(); }
    int dlqSize() { return dlq.size(); }
    List<EventMessage> dlq() { return dlq; }

    /**
     * 큐를 끝까지 소비. 컨슈머가 던지면 nack로 간주해 재시도.
     * 무한 루프 방지를 위해 안전 상한을 둔다.
     */
    void drain(EventConsumer consumer) {
        int safety = 0;
        while (!main.isEmpty()) {
            if (++safety > 100_000) throw new IllegalStateException("drain runaway");
            EventMessage m = main.pollFirst();
            try {
                consumer.handle(m);          // 성공 = ack (그냥 다음으로)
            } catch (RuntimeException ex) {
                // 실패 = nack. 재시도 카운트 증가 후 한도 비교
                int n = attempts.merge(m.messageId(), 1, Integer::sum);
                if (n >= maxAttempts) {
                    dlq.add(m);              // 한도 초과 → DLQ 격리
                } else {
                    main.addLast(m);         // 재배달(뒤로 보내 다른 메시지도 흐르게)
                }
            }
        }
    }
}
