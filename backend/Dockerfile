# ===== 1단계: 빌드 스테이지 =====
# Gradle 8.7 및 JDK 21 기반 이미지 사용
FROM gradle:8.7-jdk21 AS builder
WORKDIR /app

# 의존성 캐시 최적화: 빌드 스크립트를 먼저 복사해 의존성만 미리 받는다
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon || true

# 나머지 소스를 복사하고 실행 가능한 jar를 빌드 (테스트는 CI에서 수행하므로 여기선 생략)
COPY src ./src

ARG DB_PASSWORD
ENV SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/cityfarm?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
ENV SPRING_DATASOURCE_USERNAME=root
ENV SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
ENV SPRING_DATASOURCE_DRIVER=com.mysql.cj.jdbc.Driver

RUN gradle bootJar --no-daemon

# ===== 2단계: 실행 스테이지 =====
# Light-weight JRE 21 (Ubuntu Jammy 기반)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# 보안: root가 아닌 일반 사용자로 실행
RUN useradd -r -u 1001 appuser
USER appuser

# 빌드 스테이지에서 만든 jar만 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 컨테이너가 사용하는 포트 (문서화 목적)
EXPOSE 8080

# 운영 프로파일로 실행 (application-prod.yml 사용)
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "/app/app.jar"]