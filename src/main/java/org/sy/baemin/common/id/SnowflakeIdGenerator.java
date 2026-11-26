package org.sy.baemin.common.id;

import org.hibernate.annotations.Synchronize;

import java.util.function.LongSupplier;

/**
 * Snowflake 방식 64비트 ID 생성기.
 * | 0 (부호 1비트) | 시간 41비트 | 서버 번호 10비트 | 순번 12비트 |
 * TODO: 테스트(SnowflakeIdGeneratorTest)가 모두 통과하도록 구현하세요. / 완료
 */
public class SnowflakeIdGenerator {

    /**
     * 기준 시각: 2026-01-01T00:00:00Z. ID의 시간 칸에는 이 시각부터 지난 밀리초가 들어간다.
     */
    public static final long EPOCH = 1767225600000L;

    // TODO: 각 칸의 비트 수, 최댓값, 시프트(밀어 넣을 자리) 상수를 정의하세요.  / 완료
    private static final int NODE_ID_BITS = 10;
    private static final int SEQUENCE_BITS = 12;

    private static final long MAX_NODE_ID = (1L << NODE_ID_BITS) - 1;
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;

    private static final int NODE_ID_SHIFT = SEQUENCE_BITS;
    private static final int TIMESTAMP_SHIFT = SEQUENCE_BITS + NODE_ID_BITS;
    // 0000000000000000000000000000000000000000010000000001000000000001

    // TODO: 서버 번호, 시계, 마지막으로 ID를 만든 시각, 현재 순번을 담을 필드를 정의하세요. / 완료
    private final long nodeId;
    private final LongSupplier clock;

    private long lastTimestamp = -1L;
    private long sequence = 0L;

    /**
     * 운영용: 실제 시계(System.currentTimeMillis)를 사용한다.
     */
    public SnowflakeIdGenerator(long nodeId) {
        this(nodeId, System::currentTimeMillis);
    }

    /**
     * 테스트용: 시계를 바꿔 끼울 수 있다.
     * 실제 시계로는 "같은 밀리초에 여러 개 생성"이나 "시계가 뒤로 감" 같은 상황을 재현할 수 없기 때문이다.
     */
    SnowflakeIdGenerator(long nodeId, LongSupplier clock) {
        // TODO: nodeId 범위 검사, 필드 초기화 /완료

        if (clock == null) {
            throw new NullPointerException("clock은 null일 수 없습니다.");
        }

        if (nodeId < 0 || nodeId > MAX_NODE_ID) {
            throw new IllegalArgumentException("노드 아이디는 0과 " + MAX_NODE_ID + "사이여야 합니다. 입력값: " + nodeId);
        }
        this.nodeId = nodeId;
        this.clock = clock;
    }

    public synchronized long nextId() {

        // 시계에서 현재 시각을 가져온다 / 완료
        long now = clock.getAsLong();

        // ← 여기: now가 lastTimestamp보다 작으면 예외 / 완료
        // 시간 역전현상 예외처리 로직 추가
        if (now < lastTimestamp) {
            throw new IllegalStateException("현재 시간이 마지막Timestamp보다 작음: " + (lastTimestamp - now) + "ms");
        }

        // ← 여기서 now와 lastTimestamp를 비교해서 sequence를 정한다 / 완료
        // 4. 동타임 시 시퀀스 증가 로직 추가
        if (now == lastTimestamp){
            sequence++;
            if (sequence > MAX_SEQUENCE) {
                // 순번을 다썼으면 다음 밀리초가 될 때까지 대기
                while (now <= lastTimestamp) {
                    now = clock.getAsLong();
                }
                sequence = 0;
            }
        } else {
            sequence = 0;
        }

        // EPOCH를 빼서 시간 칸에 넣을 값을 구한다 / 완료
        long timestamp = now - EPOCH;
        lastTimestamp = now;

        // 세 칸을 밀어서 합친다 (순번은 일단 0) / 완료
        return (timestamp << TIMESTAMP_SHIFT) | (nodeId << NODE_ID_SHIFT) | sequence;
    }
}
