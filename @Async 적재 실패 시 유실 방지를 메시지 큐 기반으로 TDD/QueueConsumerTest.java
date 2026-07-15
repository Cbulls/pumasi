import java.util.*;

/**
 * 큐 기반 이벤트 적재의 유실 방지 TDD.
 *
 * 보장할 속성:
 *  Q1. 정상 메시지는 적재되고 ack 된다.
 *  Q2. 적재 중 일시적 오류(DB 끊김 등)면 nack → 재시도된다. 결국 성공하면 유실 없음.
 *  Q3. 재시도 한도(maxAttempts)를 넘으면 DLQ로 보낸다(독약 메시지 격리). 메인 큐는 막히지 않는다.
 *  Q4. 같은 메시지가 두 번 전달돼도(at-least-once) 멱등 소비로 한 번만 적재된다.
 *
 * 큐 브로커는 InMemoryBroker로 모사: at-least-once, ack/nack, 가시성 타임아웃 없이
 * 재시도 카운트와 DLQ를 충실히 흉내낸다. (Kafka/RabbitMQ를 띄우지 않고 컨슈머 로직 검증)
 */
public class QueueConsumerTest {
    static int pass = 0, fail = 0;
    static void check(String n, boolean c) {
        if (c) { pass++; System.out.println("  PASS " + n); }
        else { fail++; System.out.println("  FAIL " + n); }
    }

    static EventMessage msg(String id, String sid, String type) {
        return new EventMessage(id, "F1", sid, type, null);
    }

    public static void main(String[] args) {
        System.out.println("== Q1: 정상 메시지 적재 + ack ==");
        {
            InMemoryBroker broker = new InMemoryBroker(3);    // maxAttempts=3
            FakeEventStore store = new FakeEventStore();      // 항상 성공
            EventConsumer consumer = new EventConsumer(store);
            broker.publish(msg("m1", "s1", "survey_started"));
            broker.drain(consumer);                            // 큐 비울 때까지 소비
            check("적재 1건", store.count() == 1);
            check("메인 큐 비움", broker.mainQueueSize() == 0);
            check("DLQ 비어 있음", broker.dlqSize() == 0);
        }

        System.out.println("== Q2: 일시 오류 후 재시도 성공 → 유실 없음 ==");
        {
            InMemoryBroker broker = new InMemoryBroker(3);
            FakeEventStore store = new FakeEventStore();
            store.failNextN(2);                                // 처음 2번은 실패, 3번째 성공
            EventConsumer consumer = new EventConsumer(store);
            broker.publish(msg("m2", "s2", "survey_started"));
            broker.drain(consumer);
            check("재시도 끝에 적재 1건", store.count() == 1);
            check("DLQ 비어 있음(결국 성공)", broker.dlqSize() == 0);
            check("시도 3회 발생", store.attempts() == 3);
        }

        System.out.println("== Q3: 한도 초과 → DLQ 격리, 메인 큐 안 막힘 ==");
        {
            InMemoryBroker broker = new InMemoryBroker(3);
            FakeEventStore store = new FakeEventStore();
            store.failAlways(true);                            // 영구 실패(독약 메시지)
            EventConsumer consumer = new EventConsumer(store);
            broker.publish(msg("poison", "s3", "survey_started"));
            broker.publish(msg("good", "s4", "survey_started"));
            // good는 store가 항상 실패라 같이 실패... → good만 성공하도록 분리
            store.failOnlyMessageId("poison");
            broker.drain(consumer);
            check("poison은 DLQ로", broker.dlqSize() == 1);
            check("good은 정상 적재", store.count() == 1);
            check("메인 큐 비움(막히지 않음)", broker.mainQueueSize() == 0);
        }

        System.out.println("== Q4: 중복 전달도 멱등 소비로 1건만 적재 ==");
        {
            InMemoryBroker broker = new InMemoryBroker(3);
            FakeEventStore store = new FakeEventStore();
            EventConsumer consumer = new EventConsumer(store);
            EventMessage same = msg("dup-1", "s5", "survey_started");
            broker.publish(same);
            broker.publish(same);                              // at-least-once: 같은 메시지 2번
            broker.drain(consumer);
            check("멱등: 1건만 적재", store.count() == 1);
        }

        System.out.println("\n결과: " + pass + " pass / " + fail + " fail");
        if (fail > 0) System.exit(1);
    }
}
