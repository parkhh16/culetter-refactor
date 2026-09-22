# --- 1. 빌드(Build) 스테이지 ---
# Gradle과 JDK 21이 포함된 이미지를 빌드 환경으로 사용
FROM gradle:8.5-jdk21-jammy AS build

# 작업 디렉토리 설정
WORKDIR /app

# 빌드 설정 파일들을 먼저 복사하여 의존성을 캐싱
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 소스코드 전체를 복사
COPY src ./src

# Gradle을 사용하여 애플리케이션을 빌드 (테스트는 생략)
RUN gradle build --no-daemon


# --- 2. 실행(Runtime) 스테이지 ---
# JDK가 아닌 가벼운 JRE 이미지를 최종 이미지로 사용
FROM eclipse-temurin:21-jre-jammy

# 작업 디렉토리 설정
WORKDIR /app

# 빌드 스테이지에서 생성된 .jar 파일을 실행 스테이지로 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 컨테이너 외부로 노출할 포트 지정
EXPOSE 8080

# 컨테이너가 시작될 때 애플리케이션 실행
# JVM 언어 한국어 설정 포함
ENTRYPOINT ["java", "-Duser.language=ko", "-Duser.country=KR", "-jar", "app.jar"]