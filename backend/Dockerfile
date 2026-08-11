# ===== 1단계: 빌드 스테이지 =====
FROM gradle:8.7-jdk21 AS builder
WORKDIR /app

# 1) 의존성 라이브러리 캐싱 (소스코드가 바뀌어도 라이브러리는 다시 받지 않음)
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon || true

# 2) 소스코드 복사 및 Jar 파일 빌드 (테스트 제외)
COPY src ./src
RUN gradle bootJar -x test --no-daemon

# ===== 2단계: 실행 스테이지 =====
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# 보안: root가 아닌 일반 사용자로 실행
RUN useradd -r -u 1001 appuser
USER appuser

# 빌드 스테이지에서 생성된 jar만 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 문서화 목적 포트 명시
EXPOSE 8080

# 운영 프로파일로 실행
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "/app/app.jar"]