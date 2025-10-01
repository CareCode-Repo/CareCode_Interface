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
- **웹 애플리케이션**: http://13.209.36.209:8081
- **MySQL**: localhost:3306
- **Redis**: localhost:6379

## 🛠️ 개발 환경

### 필수 요구사항
- Java 17
- Docker & Docker Compose
- Gradle

### 로컬 개발
```bash
# 데이터베이스만 실행
docker-compose up mysql redis -d

# 애플리케이션 실행
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

# 로그 확인
docker-compose logs -f app

# 컨테이너 중지
docker-compose down

# 컨테이너 재시작
docker-compose restart
```

## 📝 환경 변수

주요 환경 변수는 `docker-compose.yml`에서 설정됩니다:
- `SPRING_DATASOURCE_URL`: MySQL 연결 정보
- `SPRING_REDIS_HOST`: Redis 호스트
- `SPRING_PROFILES_ACTIVE`: 프로파일 설정
