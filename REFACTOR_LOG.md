# 리팩토링 로그

목표: 은행/금융 공기업 지원용 포트폴리오. 데이터 정합성·트랜잭션 안정성 개선을 중심으로 보여준다.
각 항목은 착수 전 코드로 실제 검증을 거쳤고(아래 "검증" 참고), 완료 시 커밋 해시와 전/후 수치를 기록한다.

상태 값: `todo` / `in-progress` / `done`

---

## #1 CalendarService N+1 쿼리 — `done`

- **위치**: `Backend/src/main/java/com/sim/backend/domain/calendar/CalendarService.java:52-53` (수정 전)
- **문제**: `getCalendarList()`가 사용자의 스토리 목록을 조회한 뒤, 스토리마다
  `retrospectRepository.findByStoryIdOrderByEntryDateAsc(story.getId())`를 스트림 `flatMap` 내부에서
  반복 호출. 스토리 N개 → 쿼리 N+1번. 기간 필터링도 DB가 아닌 메모리(stream filter)에서 수행.
- **검증**: 코드 직접 확인 완료 (2026-09-22).
- **후보 비교**:
  - A) `IN` 절 배치 조회 — 쿼리 2번, 구현 단순
  - B) QueryDSL 조인(fetch join) — 쿼리 2번, 스토리 개수와 무관하게 고정, 기존 `NftQueryRepositoryImpl` 패턴과 일관성 → **채택**
- **측정 방법**: `@DataJpaTest` + H2 인메모리 DB + Hibernate `Statistics.getQueryExecutionCount()`로
  회귀 테스트화 (`CalendarServiceQueryCountTest.java`). 스토리 3개 + 회고 3개 픽스처 기준.
- **전/후 수치**:
  - Before: **5 쿼리** (user 조회 1 + story 목록 1 + 스토리별 회고 조회 N=3) — 스토리 수에 비례해 증가
  - After: **2 쿼리** (user 조회 1 + story-retrospect fetch join 1) — 스토리 수와 무관하게 고정
- **부수 발견**: `build.gradle`에 `useJUnitPlatform()`이 없어 JUnit5 테스트가 전혀 실행되지 않던
  인프라 결함 발견 및 수정 (2026-09-24). 기존 `BackendApplicationTests`도 동일하게 영향받고 있었음.
- **커밋**: (아래 참고)

## #2 NftService/스케줄러 트랜잭션 경계 — `done`

- **위치**: `Backend/src/main/java/com/sim/backend/domain/nft/service/NftService.java` (전체 `@Transactional` 0개),
  `Backend/src/main/java/com/sim/backend/domain/nft/controller/NftController.java:112-130` (`mintToSmartContract` 스케줄러)
- **문제**: 스케줄러 메서드가 `@Transactional`로 감싼 while 루프 안에서 여러 NFT를 순차 처리하며, 그 안에서
  블록체인 트랜잭션 전송 + 최대 90초 폴링(`PollingTransactionReceiptProcessor`)을 수행함. 특정 NFT 처리 중
  예외가 나면 이미 성공적으로 민팅된(비가역, 외부 시스템) 이전 NFT들의 DB 상태 업데이트까지 롤백되어
  블록체인 상태와 DB 상태가 어긋날 수 있음. 원인은 "트랜잭션 누락"이 아니라 "외부 비가역 연산과 DB
  트랜잭션 경계를 분리하지 않음".
- **검증**: 코드 직접 확인 완료 (2026-09-22) — `NftController.java:112-130`, `NftService.java` 전체.
- **전제**: 스마트 컨트랙트의 `mint` 로직 자체는 정확하다고 전제한다. 백엔드가 책임지는 범위는
  "그 mint를 정확히 한 번만, 안전하게 호출하는가"이며, 컨트랙트 레벨 중복 방지는 스코프 밖이다.
- **후보 비교**:
  - A) `Propagation.REQUIRES_NEW`로 NFT 1건 처리를 독립 트랜잭션화 — 구현 단순, 상태값 추가 불필요.
    다만 온체인 성공 직후·DB 커밋 직전 서버 크래시 시 복구 불가(엣지케이스)
  - B) 중간 상태(`MINTED_ONCHAIN`) 추가 — 온체인 성공을 별도 트랜잭션으로 즉시 기록해 크래시 시에도
    재민팅 없이 복구 가능. 상태 머신이 하나 늘어남 → **채택** (금융 도메인 특성상 중복 민팅 리스크를
    엣지케이스까지 방어하는 쪽이 설득력 있다고 판단)
- **측정 방법**: `@DataJpaTest` + H2 + Mockito로 `NftService`를 목킹, NFT 3건 중 3번째만 예외를 던지도록
  구성해 회귀 테스트화 (`NftSchedulerTransactionBoundaryTest.java`).
- **전/후 수치**:
  - Before: 스케줄러 전체가 하나의 `@Transactional`. 3번째 실패 시 예외가 메서드 밖으로 던져지며
    1·2번째의 `COMPLETED` 갱신까지 전부 롤백 → **커밋 0/3**
  - After: NFT 1건 = 독립 트랜잭션 경계. 3번째가 실패해도 1·2번째는 `COMPLETED`로 커밋 유지 → **커밋 2/3**
- **부수 발견 (구현 중 실제로 잡은 버그)**:
  1. 실패한 NFT를 즉시 `READY_TO_MINT`로 되돌리는 초기 구현은 같은 스케줄러 실행의 while 루프 안에서
     곧바로 재선택되어 **무한루프**에 빠짐. `MINT_FAILED` 상태를 추가해 "이번 실행에서는 재시도 제외,
     다음 실행 시작 시 일괄 복구"로 수정 (`resetFailedForRetry()`).
  2. 이 무한루프를 디버깅하는 과정에서 Logback의 예외 스택트레이스 처리(jar 출처 계산, 문자열 변환)가
     이 개발 환경에서 비정상적으로 느려 증상을 진단하기 어려웠음 — `jstack`으로 스레드 덤프를 떠서
     정확한 병목 지점을 확인. `logback-spring.xml`로 관련 기능 비활성화.
- **커밋**: `0442d0d`(1차 구현), `b033014`(테스트 초안), 이후 무한루프 수정 + 테스트 통과 확인 커밋

## #3 NFT 스케줄러 중복 처리 방지 락 부재 — `done`

- **위치**: `Backend/src/main/java/com/sim/backend/domain/nft/repository/NftQueryRepositoryImpl.java:50-58` (`pickOneNft`)
- **문제**: `pickOneNft()`가 `SELECT ... LIMIT 1`이며 `FOR UPDATE`/`SKIP LOCKED` 등 락이 없음. 현재는 기본
  스케줄러가 싱글 인스턴스·싱글 스레드라 안전하지만, 인스턴스를 2대 이상으로 확장하면 같은 NFT를
  동시에 집어 블록체인에 중복 민팅될 수 있음. #2와 함께 다룸(같은 스케줄러 메서드).
- **검증**: 코드 직접 확인 완료 (2026-09-22).
- **후보 비교**:
  - A) `SELECT ... FOR UPDATE SKIP LOCKED` — DB 레벨, 추가 인프라 없음 → **채택**
  - B) 분산 락(ShedLock 등) — 더 명시적이지만 별도 라이브러리 + 락 관리용 테이블/Redis 등 인프라 추가 필요
- **구현 참고**: 처음엔 Hibernate의 `lock.timeout=-2` 힌트로 SKIP LOCKED를 흉내냈으나, 방언(dialect)에 따라
  실제로 SKIP LOCKED SQL로 번역되는지 불확실해 native query로 `FOR UPDATE SKIP LOCKED`를 직접 명시하는
  방식으로 교체(H2 2.3 / MySQL 8.0+ 모두 지원 확인). pick과 상태 전환(`IN_PROGRESS`)도 한 트랜잭션에서
  원자적으로 처리해 그 사이 다른 인스턴스가 같은 행을 집는 걸 방지.
- **측정 방법**: `@DataJpaTest` + 별도 원시 JDBC 커넥션으로 NFT 행 하나를 `FOR UPDATE`로 잠그고 커밋하지
  않은 채로 둔 뒤 `pickAndClaimNft()`를 호출해 어떤 행을 집는지 확인 (`NftConcurrentPickTest.java`).
  스레드 2개로 타이밍을 맞추는 방식은 실제로 멈추는 문제가 있어(스레드 없이) 이 방식으로 단순화함.
- **전/후 수치**:
  - Before: 락 없는 단순 `SELECT` — 다른 커넥션이 잠근 행이어도 커밋 전이면 그대로 읽혀 중복 픽 가능
  - After: `FOR UPDATE SKIP LOCKED` — 잠긴 행을 건너뛰고 다른 NFT를 집음 → **중복 픽 0건**
- **커밋**: `0442d0d`(1차 구현), 이후 native query 전환 + 테스트 통과 확인 커밋

## #4 서비스 전반 @Transactional 커버리지 — `todo`

- **위치**: `Backend/src/main/java/com/sim/backend/domain/**/service` 전반
- **문제**: 서비스 클래스 7개 중 6개는 `@Transactional` 사용, `NftService`만 0개. 전역 예외 처리기(`@ControllerAdvice`)도 없음.
- **검증**: grep으로 서비스별 사용 여부 확인 완료 (2026-09-22).
- **전/후 수치**: (착수 시 정리 예정)
- **커밋**: -

## #5 테스트 커버리지 — `todo`

- **위치**: `Backend/src/test/java/com/sim/backend/BackendApplicationTests.java`
- **문제**: 프로젝트 생성 시 기본으로 만들어진 컨텍스트 로딩 테스트 1개뿐, 그 외 테스트 없음.
- **검증**: `src/test` 전체 탐색 완료 (2026-09-22) — 파일 1개만 존재 확인.
- **전/후 수치**: (착수 시 정리 예정)
- **커밋**: -

---

## 진행 우선순위

원래 제시된 순서는 1 → 2 → 3 → 4 → 5였으나, 코드 검증 과정에서 #2와 #3이 같은 스케줄러 메서드를
공유하고 서로 강하게 엮여 있음을 확인함(외부 비가역 연산 + 동시성 문제가 한 메서드 안에 같이 있음).
**1(N+1) → 2+3(트랜잭션 경계 + 동시성, 함께) → 4 → 5** 순서로 확정, 1·2·3 완료. 다음은 #4.
