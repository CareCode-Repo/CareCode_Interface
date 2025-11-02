# CareCode Interface

CareCode Spring Boot 애플리케이션입니다.

## 🚀 빠른 시작

### 1. 프로젝트 빌드
```bash
./gradlew clean build
```

### 2. Docker로 배포
```bash
# 배포 스크립트 실행
./deploy.sh

# 또는 수동으로 실행
docker-compose up -d
```

### 3. 접속 정보
- **웹 애플리케이션 (Nginx)**: http://localhost
- **API 컨테이너**: http://localhost (Nginx 경유) → 백엔드는 `carecode-api:8080`
- **MariaDB**: localhost:3307 (컨테이너 3306)
- **Redis**: localhost:6380 (컨테이너 6379)

## 🛠️ 개발 환경

### 필수 요구사항
- Java 17
- Docker & Docker Compose
- Gradle

### 로컬 개발
```bash
# 데이터베이스/레디스만 실행
docker-compose up carecode-mariadb carecode-redis -d

# 애플리케이션(로컬) 실행
./gradlew bootRun
```

## 📁 프로젝트 구조

```
src/
├── main/
│   ├── java/com/carecode/
│   │   ├── domain/          # 도메인별 패키지
│   │   ├── core/            # 공통 컴포넌트
│   │   └── CareCodeApplication.java
│   └── resources/
│       ├── templates/       # Thymeleaf 템플릿
│       └── static/         # 정적 리소스
└── test/                   # 테스트 코드
```

## 🔧 Docker 명령어

```bash
# 컨테이너 상태 확인
docker-compose ps

# 로그 확인 (기본: API)
./scripts/deploy.sh logs carecode-api

# 전체 로그 확인
docker-compose logs -f --tail=100

# 컨테이너 중지
docker-compose down

# 컨테이너 재시작
docker-compose restart
```

## 📝 환경 변수

주요 환경 변수는 `docker-compose.yml` 및 애플리케이션 프로퍼티에서 환경변수로 주입됩니다:
- `SPRING_PROFILES_ACTIVE`: 프로파일 설정
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `SPRING_REDIS_HOST`, `SPRING_REDIS_PORT`
- `JWT_SECRET`, `MAIL_*`, `KAKAO_CLIENT_*`, `GOOGLE_CLIENT_*`, `PUBLIC_DATA_API_KEY`

보안상 민감정보는 코드에 하드코딩하지 않고 환경 변수로 주입하세요.
