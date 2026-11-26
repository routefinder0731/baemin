package org.sy.baemin.common.id;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SnowflakeIdGeneratorTest {

    // ID에서 각 칸을 꺼내는 도우미. 구현과 별개로 "비트 배치 규칙"을 테스트에서 직접 확인한다.
    private static long timestampOf(long id) {
        return (id >> 22) + SnowflakeIdGenerator.EPOCH;
    }

    private static long nodeIdOf(long id) {
        return (id >> 12) & 0x3FF;   // 0x3FF = 10비트가 모두 1 = 1023
    }

    private static long sequenceOf(long id) {
        return id & 0xFFF;           // 0xFFF = 12비트가 모두 1 = 4095
    }

    // ---------- 1단계: 기본 ----------

    @Test
    @DisplayName("생성된 ID는 양수다")
    void idIsPositive() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1);

        assertThat(generator.nextId()).isPositive();
    }

    @Test
    @DisplayName("ID에 서버 번호가 들어간다")
    void idContainsNodeId() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(7);

        assertThat(nodeIdOf(generator.nextId())).isEqualTo(7);
    }

    @Test
    @DisplayName("ID에 생성 시각이 들어간다")
    void idContainsTimestamp() {
        long now = SnowflakeIdGenerator.EPOCH + 1_000_000;
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, () -> now);

        assertThat(timestampOf(generator.nextId())).isEqualTo(now);
    }

    @Test
    @DisplayName("서버 번호가 0~1023 범위를 벗어나면 예외가 발생한다")
    void invalidNodeId() {
        assertThatThrownBy(() -> new SnowflakeIdGenerator(-1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SnowflakeIdGenerator(1024))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- 2단계: 순번 ----------

    @Test
    @DisplayName("같은 밀리초에 여러 개를 만들면 순번이 0, 1, 2로 늘어난다")
    void sequenceIncreasesInSameMillisecond() {
        long now = SnowflakeIdGenerator.EPOCH + 1_000;
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, () -> now);

        long first = generator.nextId();
        long second = generator.nextId();
        long third = generator.nextId();

        assertThat(sequenceOf(first)).isEqualTo(0);
        assertThat(sequenceOf(second)).isEqualTo(1);
        assertThat(sequenceOf(third)).isEqualTo(2);
    }

    @Test
    @DisplayName("밀리초가 바뀌면 순번이 0부터 다시 시작한다")
    void sequenceResetsInNextMillisecond() {
        AtomicLong clock = new AtomicLong(SnowflakeIdGenerator.EPOCH + 1_000);
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, clock::get);

        generator.nextId();
        generator.nextId();
        clock.incrementAndGet();          // 1밀리초 지남
        long id = generator.nextId();

        assertThat(sequenceOf(id)).isEqualTo(0);
    }

    @Test
    @DisplayName("연속으로 만든 ID는 항상 이전보다 크다")
    void idsAreIncreasing() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1);

        long previous = generator.nextId();
        for (int i = 0; i < 10_000; i++) {
            long current = generator.nextId();
            assertThat(current).isGreaterThan(previous);
            previous = current;
        }
    }

    // ---------- 3단계: 경계 상황 ----------

    @Test
    @DisplayName("한 밀리초의 순번 4096개를 다 쓰면 다음 밀리초까지 기다렸다가 만든다")
    void waitsForNextMillisecondWhenSequenceExhausted() {
        long now = SnowflakeIdGenerator.EPOCH + 1_000;
        AtomicInteger calls = new AtomicInteger();
        // 처음 4,100번은 같은 시각을 돌려주고, 그 뒤로는 1밀리초 뒤를 돌려주는 가짜 시계
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1,
                () -> calls.incrementAndGet() <= 4_100 ? now : now + 1);

        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 4_097; i++) {
            ids.add(generator.nextId());
        }

        assertThat(new HashSet<>(ids)).hasSize(4_097);                  // 중복 없음
        assertThat(ids).isSorted();                                      // 계속 증가
        assertThat(ids).allSatisfy(id -> assertThat(nodeIdOf(id)).isEqualTo(1)); // 순번이 서버 번호 칸을 침범하지 않음
        assertThat(timestampOf(ids.getLast())).isEqualTo(now + 1);       // 마지막 하나는 다음 밀리초
    }

    @Test
    @DisplayName("시계가 뒤로 가면 중복 ID를 막기 위해 예외가 발생한다")
    void clockMovedBackwards() {
        AtomicLong clock = new AtomicLong(SnowflakeIdGenerator.EPOCH + 1_000);
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, clock::get);

        generator.nextId();
        clock.addAndGet(-5);              // 시계가 5밀리초 뒤로 감 (서버 시간 동기화 등)

        assertThatThrownBy(generator::nextId)
                .isInstanceOf(IllegalStateException.class);
    }

    // ---------- 4단계: 동시성 ----------

    @Test
    @DisplayName("여러 스레드가 동시에 만들어도 ID가 겹치지 않는다")
    void uniqueUnderConcurrency() throws InterruptedException {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1);
        int threadCount = 10;
        int idsPerThread = 1_000;
        Set<Long> ids = ConcurrentHashMap.newKeySet();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch done = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < idsPerThread; i++) {
                        ids.add(generator.nextId());
                    }
                } finally {
                    done.countDown();
                }
            });
        }
        done.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(ids).hasSize(threadCount * idsPerThread);
    }
}
