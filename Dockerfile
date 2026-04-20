# 1단계: Gradle로 빌드
FROM gradle:8.14.2-jdk17 AS builder

WORKDIR /app
COPY . .

RUN gradle clean bootJar --no-daemon

# 2단계: JDK 17로 실행용 이미지 구성
FROM openjdk:17-jdk-slim

ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"

WORKDIR /app

# 빌드된 JAR 복사 (하나만 있는 경우 자동 복사 가능)
COPY --from=builder /app/build/libs/carecode-app.jar app.jar

RUN addgroup --system carecode && adduser --system --ingroup carecode carecode
RUN chown -R carecode:carecode /app
USER carecode

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8082/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
