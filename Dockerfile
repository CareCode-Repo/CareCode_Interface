# 1단계: Gradle로 빌드
FROM gradle:8.14.2-jdk17 AS builder

WORKDIR /app
COPY . .

RUN gradle clean bootJar --no-daemon

# 스프링 부트 레이어로 쪼갠다. 의존성(156MB 중 대부분)은 거의 바뀌지 않으므로 별도 레이어로 두면
# 재배포 때 애플리케이션 레이어(수 MB)만 내려받는다. 1GB 인스턴스에서 배포 시간이 크게 줄어든다.
RUN java -Djarmode=tools -jar build/libs/carecode-app.jar extract --layers --launcher --destination build/extracted

# 2단계: 실행용 이미지
#
# openjdk 공식 이미지는 폐기되어 Docker Hub 에서 태그가 내려갔다. Docker 가 후속으로 안내하는
# eclipse-temurin 으로 옮겼다.
#
# JDK 가 아니라 JRE 를 쓴다. 이미지가 1.26GB → 300MB 대로 줄어, 1GB 인스턴스에서 배포마다
# 받는 양과 디스크 사용이 크게 줄어든다. 예전에 JDK 를 둔 이유는 운영 중 jcmd·jstack 이었는데,
# 그건 필요할 때 JDK 컨테이너를 같은 PID 공간에 붙여 쓰면 된다(운영 문서에 명령을 적어 두었다).
#   docker run --rm --pid=container:carecode eclipse-temurin:17-jdk-jammy jcmd 1 VM.native_memory
FROM eclipse-temurin:17-jre-jammy

ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 작은 인스턴스(1GB)를 기준으로 잡은 기본값. 컨테이너에 --memory 가 걸려 있어야 의미가 있다
# (제한이 없으면 JVM 이 호스트 전체를 기준으로 계산해 858MB 까지 썼다).
#
# - MaxRAMPercentage=55: --memory=512m 에서 힙 약 280MB. 힙 밖(메타스페이스·스레드·코드캐시·
#   다이렉트 버퍼)이 150MB 가까이 되므로 70% 로 두면 컨테이너 한도를 넘겨 OOM 으로 죽는다.
# - SerialGC: vCPU 1~2개에서는 G1 의 백그라운드 스레드가 오히려 부담이다.
# - MaxMetaspaceSize: 상한이 없으면 메타스페이스가 조용히 늘어 컨테이너 한도를 밀어낸다.
# - ExitOnOutOfMemoryError: 반쯤 죽은 상태로 버티는 대신 죽는다. 그래야 --restart 가 살린다.
#
# 여유 있는 인스턴스라면 배포 시 JAVA_OPTS 로 덮어쓴다 (예: -XX:+UseG1GC -XX:MaxRAMPercentage=75).
ENV JAVA_OPTS="-XX:MaxRAMPercentage=55.0 -XX:MaxMetaspaceSize=192m -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"

WORKDIR /app

RUN addgroup --system carecode && adduser --system --ingroup carecode carecode

# 업로드 저장소. 이미지에 폴더가 있어야 볼륨을 처음 붙일 때 소유권이 이어진다
# (없으면 볼륨이 root 소유로 생겨 carecode 사용자가 파일을 쓰지 못한다).
RUN mkdir -p /app/uploads && chown carecode:carecode /app /app/uploads

# 바뀌지 않는 것부터 복사해 레이어 캐시를 살린다. --chown 으로 복사하는 이유는
# 나중에 chown -R 을 걸면 156MB JAR 이 레이어에 한 번 더 복사돼 이미지가 두 배가 되기 때문이다.
COPY --from=builder --chown=carecode:carecode /app/build/extracted/dependencies/ ./
COPY --from=builder --chown=carecode:carecode /app/build/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=carecode:carecode /app/build/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=carecode:carecode /app/build/extracted/application/ ./

USER carecode

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=5s --start-period=180s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8082/actuator/health | grep -q '"status":"UP"' || exit 1

# 레이어로 쪼갠 실행 파일은 JarLauncher 로 띄운다 (app.jar 이 그대로 있지 않다).
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
