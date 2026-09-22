# 리팩토링 로그

목표: 은행/금융 공기업 지원용 포트폴리오. 데이터 정합성·트랜잭션 안정성 개선을 중심으로 보여준다.
각 항목은 착수 전 코드로 실제 검증을 거쳤고(아래 "검증" 참고), 완료 시 커밋 해시와 전/후 수치를 기록한다.

상태 값: `todo` / `in-progress` / `done`

---

## #1 CalendarService N+1 쿼리 — `todo`

- **위치**: `Backend/src/main/java/com/sim/backend/domain/calendar/CalendarService.java:52-53`
- **문제**: `getCalendarList()`가 사용자의 스토리 목록을 조회한 뒤, 스토리마다
  `retrospectRepository.findByStoryIdOrderByEntryDateAsc(story.getId())`를 스트림 `flatMap` 내부에서
  반복 호출. 스토리 N개 → 쿼리 N+1번.
- **검증**: 코드 직접 확인 완료 (2026-09-22).
- **전/후 수치**: (착수 시 측정 예정 — 실제 호출 시 쿼리 로그 카운트)
- **커밋**: -

## #2 NftService/스케줄러 트랜잭션 경계 — `todo`

- **위치**: `Backend/src/main/java/com/sim/backend/domain/nft/service/NftService.java` (전체 `@Transactional` 0개),
  `Backend/src/main/java/com/sim/backend/domain/nft/controller/NftController.java:112-130` (`mintToSmartContract` 스케줄러)
- **문제**: 스케줄러 메서드가 `@Transactional`로 감싼 while 루프 안에서 여러 NFT를 순차 처리하며, 그 안에서
  블록체인 트랜잭션 전송 + 최대 90초 폴링(`PollingTransactionReceiptProcessor`)을 수행함. 특정 NFT 처리 중
  예외가 나면 이미 성공적으로 민팅된(비가역, 외부 시스템) 이전 NFT들의 DB 상태 업데이트까지 롤백되어
  블록체인 상태와 DB 상태가 어긋날 수 있음. 원인은 "트랜잭션 누락"이 아니라 "외부 비가역 연산과 DB
  트랜잭션 경계를 분리하지 않음".
- **검증**: 코드 직접 확인 완료 (2026-09-22) — `NftController.java:112-130`, `NftService.java` 전체.
- **전/후 수치**: (착수 시 정리 예정)
- **커밋**: -

## #3 NFT 스케줄러 중복 처리 방지 락 부재 — `todo`

- **위치**: `Backend/src/main/java/com/sim/backend/domain/nft/repository/NftQueryRepositoryImpl.java:50-58` (`pickOneNft`)
- **문제**: `pickOneNft()`가 `SELECT ... LIMIT 1`이며 `FOR UPDATE`/`SKIP LOCKED` 등 락이 없음. 현재는 기본
  스케줄러가 싱글 인스턴스·싱글 스레드라 안전하지만, 인스턴스를 2대 이상으로 확장하면 같은 NFT를
  동시에 집어 블록체인에 중복 민팅될 수 있음. #2와 함께 다루는 것을 권장(같은 스케줄러 메서드).
- **검증**: 코드 직접 확인 완료 (2026-09-22).
- **전/후 수치**: (착수 시 정리 예정)
- **커밋**: -

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

## 진행 우선순위 (합의 필요)

원래 제시된 순서는 1 → 2 → 3 → 4 → 5였으나, 코드 검증 과정에서 #2와 #3이 같은 스케줄러 메서드를
공유하고 서로 강하게 엮여 있음을 확인함(외부 비가역 연산 + 동시성 문제가 한 메서드 안에 같이 있음).
**1(N+1) → 2+3(트랜잭션 경계 + 동시성, 함께) → 4 → 5** 순서를 제안하되, 최종 순서는 다음 대화에서 확정.
