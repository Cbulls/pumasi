package com.pumasiform.events.queue;

import com.pumasiform.events.EventIngestService;
import com.pumasiform.events.EventRequest;
import org.springframework.stereotype.Component;

/**
 * 큐 컨슈머. 검증된 순수 로직(QueueConsumerTest의 EventConsumer)을 Spring으로 옮긴 형태.
 *
 * 유실 방지 보장(테스트로 검증한 속성):
 *  - Q2 재시도: 적재 실패 시 예외를 던지면 브로커가 nack→재배달(maxAttempts까지).
 *  - Q3 DLQ: 한도 초과 메시지는 브로커 설정의 dead-letter-exchange로 자동 이동.
 *  - Q4 멱등: EventIngestService.ingest 자체가 멱등(세션 전이 R3/R4 + survey_event 적재).
 *            같은 메시지가 at-least-once로 두 번 와도 결과는 1건.
 *
 * 아래는 RabbitMQ(@RabbitListener) 기준 예시. Kafka면 @KafkaListener +
 * SeekToCurrentErrorHandler/DefaultErrorHandler로 동일한 재시도·DLQ를 구성한다.
 * 브로커 설정(재시도 횟수, DLQ 바인딩)은 application.yml/RabbitConfig에 둔다.
 */
@Component
public class EventQueueConsumer {

    private final EventIngestService ingestService;

    public EventQueueConsumer(EventIngestService ingestService) {
        this.ingestService = ingestService;
    }

    /**
     * 예시 시그니처(RabbitMQ):
     *   @RabbitListener(queues = "events.ingest",
     *                   ackMode = "AUTO")   // 예외 없으면 ack, 던지면 nack
     * 실제 어노테이션은 spring-boot-starter-amqp 의존성에서 온다.
     */
    public void onMessage(EventMessage msg) {
        // EventIngestService가 멱등하므로 중복 전달도 안전(Q4).
        // 적재 실패 시 예외 전파 → 브로커가 재시도/DLQ 처리(Q2/Q3).
        ingestService.ingest(toRequest(msg));
    }

    private EventRequest toRequest(EventMessage m) {
        return new EventRequest(
            m.sessionId(), m.formId(), m.eventType(),
            m.questionId(), m.questionOrder(), m.experimentArm(), m.occurredAt());
    }
}
