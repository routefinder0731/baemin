# baemin - 배달 주문 서비스

배달의민족과 같은 배달 주문 서비스를 백엔드 API로 구현하는 포트폴리오 프로젝트입니다.
고객·사장님·라이더 3가지 역할의 주문 흐름을 처음부터 끝까지 구현하고,
동시성 제어, 트랜잭션, 이벤트 기반 처리, 캐싱, DB 이중화를 다룹니다.

## 기술 스택

- Java 21, Spring Boot 4.1.2, Gradle
- MySQL 8 (master 1 + replica 1), Spring Data JPA
- Redis, Kafka, WebSocket(STOMP)
- JUnit 5, Testcontainers

## 진행 상황

- [ ] 1주 차: 기반 세팅, Snowflake ID
- [ ] 2주 차: 회원·인증, 가게·메뉴
- [ ] 3주 차: 장바구니·주문·재고 락
- [ ] 4주 차: 결제·주문 상태 머신 (MVP)
- [ ] 5주 차: Kafka·실시간 알림·배차
- [ ] 6주 차: Redis 캐싱·MySQL replication
- [ ] 7주 차: 정산 배치·부하 테스트
- [ ] 8주 차: 문서화

설계 내용은 [docs/design.md](docs/design.md)에 정리되어 있습니다.
