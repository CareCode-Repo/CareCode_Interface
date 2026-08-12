# 1단계: Gradle로 빌드
FROM gradle:8.14.2-jdk17 AS builder

WORKDIR /app
COPY . .

RUN gradle clean bootJar --no-daemon

# 2단계: JDK 17로 실행용 이미지 구성
#
# openjdk 공식 이미지는 폐기되어 Docker Hub 에서 태그가 내려갔다. openjdk:17-jdk-slim 은
# 더 이상 존재하지 않아 이미지 빌드가 "not found" 로 실패한다. Docker 가 후속으로 안내하는
# eclipse-temurin 으로 옮긴다.
#
# JRE 가 아니라 JDK 를 쓰는 건 이전과 같다. 운영 중 jcmd·jstack 으로 들여다보던 걸
# 이 교체 때문에 잃지 않도록 한다. (이미지 크기를 줄이려면 -jre-jammy 로 바꿀 수 있는데,
# 아래 HEALTHCHECK 의 wget 과 addgroup/adduser 는 그쪽에도 모두 있다.)
FROM eclipse-temurin:17-jdk-jammy

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
