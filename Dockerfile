# 1단계: Gradle로 빌드
FROM gradle:7.6.2-jdk17 AS builder

WORKDIR /app
COPY . .

RUN gradle clean build --no-daemon

# 2단계: JDK 17로 실행용 이미지 구성
FROM openjdk:17-jdk-slim

ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /app

# 빌드된 JAR 복사 (하나만 있는 경우 자동 복사 가능)
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
