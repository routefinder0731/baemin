# 설계 요약

## 범위
- 역할: 고객, 사장님, 라이더 (관리자 제외)
- 흐름: 회원가입·로그인 → 가게·메뉴 조회 → 장바구니 → 주문 → 결제 → 조리 → 배차 → 배달 완료
- 결제는 Mock PG(성공·실패·타임아웃 재현), 배달 지역은 동 단위, 프론트엔드 없음(Swagger로 대체)

## 주문 상태
PAYMENT_PENDING → ORDERED → COOKING → RIDER_ASSIGNED → DELIVERING → DELIVERED
- PAYMENT_PENDING, ORDERED 에서만 CANCELED 로 이동 가능

## 핵심 기술 포인트
1. Snowflake ID: 분산 환경에서 겹치지 않고 시간순으로 정렬되는 PK
2. 재고 차감 동시성: 비관적 락 / 낙관적 락 / 조건부 UPDATE 비교
3. 트랜잭션 + MySQL replication: readOnly 트랜잭션은 replica로 라우팅
4. Redis 캐싱: 인기 가게·메뉴, TTL과 캐시 무효화 시점
5. 실시간 알림: Transactional Outbox → Kafka → WebSocket(STOMP)
6. 결제 멱등성: Idempotency-Key + UNIQUE 제약, 타임아웃 시 결과 재조회
7. 정산: @Scheduled 배치 + ShedLock, 중복 정산 방지

설계 결정의 이유는 docs/adr/ 에 하나씩 기록합니다.
