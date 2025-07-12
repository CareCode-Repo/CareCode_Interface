# ===========================================
# Build Stage
# ===========================================
FROM gradle:8.5-jdk17 AS build

# 작업 디렉토리 설정
WORKDIR /app

# Gradle 캐시를 위한 설정
COPY gradle gradle
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .

# Gradle 의존성 다운로드 (캐시 레이어 분리)
RUN gradle dependencies --no-daemon

# 소스 코드 복사
COPY src src

# 애플리케이션 빌드
RUN gradle build -x test --no-daemon

# ===========================================
# Runtime Stage
# ===========================================
FROM openjdk:17-jre-slim

# 메타데이터 설정
LABEL maintainer="CareCode Team"
LABEL version="1.0.0"
LABEL description="CareCode Backend Application"

# 애플리케이션 사용자 생성 (보안 강화)
RUN groupadd -r carecode && useradd -r -g carecode carecode

# 작업 디렉토리 설정
WORKDIR /app

# JVM 옵션 설정
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseContainerSupport"

# 애플리케이션 JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 파일 소유권 변경
RUN chown -R carecode:carecode /app

# 사용자 전환
USER carecode

# 헬스체크를 위한 포트 노출
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
