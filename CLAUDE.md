# CLAUDE.md

씨유레터(SSAFY 팀 프로젝트) 백엔드 포트폴리오 리팩토링 저장소. 금융/은행 공기업 지원을 목표로
데이터 정합성·트랜잭션 안정성 개선을 보여주는 것이 목적.

## 빌드/실행

- 백엔드 경로: `Backend/`
- Java 21 필요. `Backend/gradle.properties`에 `org.gradle.java.home`으로 JDK 21 경로 지정되어 있음
  (환경변수 `JAVA_HOME`이 다른 버전이어도 이 설정이 우선 적용됨 — Gradle daemon 기준. gradlew 부트스트랩
  런처 자체는 PATH의 java를 쓰므로 `Launcher JVM` 표시는 달라도 정상).
- 실행 전 준비물 (둘 다 `.gitignore` 대상, 로컬에만 존재):
  - `Backend/.env` — DB_HOST/PORT/USERNAME/PASSWORD, ETH_*, PINATA_*, MINIO_*, CONTRACT_ADDRESS
  - `Backend/src/main/resources/firebase-service-account.json`
- 실행: `cd Backend && ./gradlew.bat bootRun`
- 알려진 이슈: `application.properties`의 JDBC URL이 DB명 자리에 `${DB_USERNAME}`을 재사용함
  (`jdbc:mysql://host:port/${DB_USERNAME}`). 로컬 계정명이 우연히 DB명(`culetter`)과 같아서 동작하는
  구조이니, 계정명을 바꾸면 깨질 수 있음을 인지할 것.

## 작업 방식 (이 세션에서 합의된 규칙)

리팩토링 작업은 포트폴리오/면접용으로, 사용자가 각 변경을 직접 설명할 수 있어야 함. 다음을 반드시 지킬 것:

- **작업 단위별로 멈춘다.** 하나의 리팩토링 항목이 끝나면 무엇을/왜 했는지(대안 비교 포함) 설명하고,
  사용자가 "다음"이라고 할 때까지 다음 항목으로 넘어가지 않는다.
- **기술 선택은 항상 2개 이상 후보를 제시**하고 사용자가 직접 고르게 한다.
- **커밋은 작업 단위별로 잘게 나누고**, 커밋 메시지에 전/후 수치(쿼리 횟수, 커버리지 등)를 포함한다.
- 진행 상황과 각 항목의 상태·전/후 수치는 [`REFACTOR_LOG.md`](./REFACTOR_LOG.md)에 기록한다. 새 작업을
  시작하기 전 이 파일을 먼저 읽고 현재 상태를 파악할 것.
- 리팩토링 후보로 제시된 이슈는 실제 코드를 읽어 검증 후에만 착수한다(추측 금지).
