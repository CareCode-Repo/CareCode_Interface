# CareCode Interface (맘편한)

> 부모를 위한 통합 육아 지원 플랫폼
>
> **오픈소스 개발자 경진대회 프로젝트**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![MariaDB](https://img.shields.io/badge/MariaDB-10.x-blue.svg)](https://mariadb.org/)
[![Redis](https://img.shields.io/badge/Redis-7.x-red.svg)](https://redis.io/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 목차

1. [프로젝트 소개](#프로젝트-소개)
2. [프로젝트 배경 및 목적](#프로젝트-배경-및-목적)
3. [핵심 기능](#핵심-기능)
4. [기술 스택](#기술-스택)
5. [시스템 아키텍처](#시스템-아키텍처)
6. [도메인 구조](#도메인-구조)
7. [빠른 시작](#빠른-시작)
8. [API 문서](#api-문서)
9. [배포 가이드](#배포-가이드)
10. [개발 로드맵](#개발-로드맵)
11. [프로젝트 구조](#프로젝트-구조)
12. [기여하기](#기여하기)
13. [라이선스](#라이선스)

---

## 프로젝트 소개

**CareCode Interface (맘편한)**는 육아를 하는 부모들을 위한 **통합 육아 지원 플랫폼**입니다.

대한민국의 부모들은 육아 과정에서 다음과 같은 어려움을 겪고 있습니다:
- 정부/지자체 육아 정책 정보가 흩어져 있어 찾기 어려움
- 돌봄 시설(어린이집, 유치원) 정보 비교가 불편함
- 자녀 건강 기록을 체계적으로 관리하기 어려움
- 육아 고민을 나눌 커뮤니티 부족
- 24시간 육아 상담 서비스의 부재

**CareCode Interface**는 이러한 문제를 해결하기 위해 **7가지 핵심 도메인**을 통합하여 부모들에게 원스톱 육아 지원 서비스를 제공합니다.

---

## 프로젝트 배경 및 목적

### 배경

대한민국의 합계출산율은 2023년 기준 **0.72명**으로 OECD 국가 중 최하위를 기록하고 있습니다. 저출산의 주요 원인 중 하나는 **육아 부담**입니다. 정부는 매년 수백 가지의 육아 지원 정책을 발표하지만, 정보 접근성이 낮아 실제로 혜택을 받는 부모는 제한적입니다.

또한, 육아 관련 정보가 여러 플랫폼에 분산되어 있어 부모들은:
- 정책 정보는 **정부24** 또는 각 지자체 홈페이지
- 돌봄 시설 정보는 **아이사랑보육포털**
- 건강 정보는 **질병관리청** 또는 병원 앱
- 커뮤니티는 **맘카페** 등

각각 다른 서비스를 이용해야 하는 불편함을 겪고 있습니다.

### 목적

CareCode Interface는 다음을 목표로 합니다:

1. **정보 통합**: 육아 관련 모든 정보를 한 곳에서 제공
2. **접근성 향상**: 직관적인 UI/UX로 누구나 쉽게 사용 가능
3. **개인화**: 자녀 정보를 기반으로 맞춤형 정보 제공
4. **커뮤니티**: 부모들 간의 정보 공유 및 상호 지원
5. **AI 지원**: 24시간 챗봇을 통한 즉각적인 육아 상담
6. **데이터 관리**: 자녀 건강 기록, 성장 곡선 등 체계적 관리
7. **공공 데이터 활용**: 공공 API를 통한 신뢰성 있는 정보 제공

### 기대 효과

- 육아 정보 탐색 시간 **60% 단축**
- 육아 정책 활용도 **40% 증가**
- 부모들의 육아 스트레스 **30% 감소**
- 커뮤니티를 통한 **사회적 지지 네트워크 형성**

---

## 핵심 기능

### 1. 사용자 관리 (User Domain)

- **회원가입/로그인**: 이메일 인증 기반 일반 회원가입
- **소셜 로그인**: 카카오, 구글 OAuth2 지원
- **JWT 토큰 인증**: 안전한 인증 및 세션 관리
- **자녀 정보 관리**: 다수의 자녀 정보 등록 및 관리
- **알림 설정**: 개인화된 알림 채널 및 시간대 설정

### 2. 돌봄 시설 관리 (CareFacility Domain)

- **시설 검색**: 유형별(어린이집, 유치원, 놀이방), 지역별, 연령대별 필터링
- **위치 기반 검색**: 내 주변 돌봄 시설 찾기
- **시설 상세 정보**: 운영 시간, 정원, 프로그램, 요금 등
- **시설 예약**: 온라인 방문 예약 및 관리
- **리뷰 시스템**: 시설 이용 후기 작성 및 조회

### 3. 커뮤니티 (Community Domain)

- **게시글 관리**: 카테고리별(일반, 질문, 후기, 뉴스 등) 게시글 작성
- **댓글/대댓글**: 무한 댓글 기능으로 활발한 소통
- **태그 시스템**: 해시태그를 통한 게시글 분류 및 검색
- **좋아요/북마크**: 유용한 게시글 저장 및 공유
- **익명 게시**: 민감한 고민도 안전하게 공유

### 4. 건강 관리 (Health Domain)

- **건강 기록**: 예방접종, 진료, 검진, 성장 기록 관리
- **성장 추적**: 신장, 체중 기록 및 성장 곡선 분석
- **진료 기록 첨부**: 진단서, 검진표 등 문서 보관
- **병원 정보**: 소아과, 소아치과 등 병원 검색 및 리뷰
- **건강 통계**: 자녀의 건강 데이터 시각화

### 5. 정책 정보 (Policy Domain)

- **정책 검색**: 육아휴직, 보육료 지원, 양육비 지원 등
- **카테고리별 분류**: 정책 유형에 따른 체계적 분류
- **공공 데이터 연동**: 서울시 등 공공 API 실시간 연동
- **정책 북마크**: 관심 정책 저장 및 알림 설정
- **맞춤형 정책**: 자녀 나이, 지역 기반 추천

### 6. 알림 시스템 (Notification Domain)

- **실시간 알림**: 예약, 댓글, 정책 업데이트 즉시 알림
- **다중 채널**: 이메일, 푸시, SMS 중 선택
- **알림 템플릿**: 일관된 형식의 알림 메시지
- **야간 모드**: 방해 금지 시간대 설정
- **알림 이력**: 놓친 알림 확인

### 7. AI 챗봇 (Chatbot Domain)

- **24/7 육아 상담**: 언제든지 육아 고민 상담
- **다중 세션 관리**: 여러 주제의 대화 동시 진행
- **대화 히스토리**: 이전 상담 내용 저장 및 조회
- **피드백 수집**: 챗봇 응답의 유용성 평가
- **맥락 이해**: 이전 대화 내용을 기반으로 한 상담

### 8. 관리자 기능 (Admin Domain)

- **사용자 관리**: 회원 정보 조회 및 관리
- **콘텐츠 관리**: 시설, 병원, 정책 데이터 등록/수정
- **커뮤니티 모니터링**: 부적절한 게시글/댓글 관리
- **대시보드**: 서비스 통계 및 모니터링
- **권한 관리**: 역할 기반 접근 제어 (RBAC)

---

## 기술 스택

### Backend

| 분류 | 기술 | 버전 | 설명 |
|-----|------|------|------|
| 언어 | Java | 17 (LTS) | 안정적이고 성능이 우수한 LTS 버전 |
| 프레임워크 | Spring Boot | 3.3.3 | 최신 Spring Boot 기반 |
| 데이터베이스 | MariaDB | 10.x | 오픈소스 관계형 데이터베이스 |
| 캐시 | Redis | 7.x | 고성능 인메모리 캐시 |
| 인증 | JWT | - | 토큰 기반 인증 |
| ORM | Spring Data JPA | - | 객체-관계 매핑 |
| 보안 | Spring Security | - | 인증 및 권한 관리 |
| API 문서 | SpringDoc OpenAPI | 2.3.0 | Swagger UI 자동 생성 |

### Infrastructure

| 분류 | 기술 | 설명 |
|-----|------|------|
| 컨테이너 | Docker | 애플리케이션 컨테이너화 |
| 오케스트레이션 | Docker Compose | 멀티 컨테이너 관리 |
| 리버스 프록시 | Nginx | HTTP/HTTPS 라우팅 |
| 빌드 도구 | Gradle | 8.14.2 |
| CI/CD | GitHub Actions, CodeQL | 빌드·테스트·Trivy·Java 정적 분석(codeql.yml) |

### Monitoring & Logging

| 분류 | 기술 | 설명 |
|-----|------|------|
| 모니터링 | Spring Actuator | 애플리케이션 헬스 체크 |
| 메트릭 | Prometheus | 시계열 메트릭 수집 |
| 로깅 | Logback + Logstash | JSON 형식 구조화 로깅 |

### External APIs

- **카카오 OAuth2**: 소셜 로그인
- **구글 OAuth2**: 소셜 로그인
- **서울시 공공 데이터 API**: 돌봄 시설, 정책 정보
- **이메일 서비스**: 이메일 인증 및 알림

---

## 시스템 아키텍처

### 계층형 아키텍처 (Layered Architecture)

```
┌─────────────────────────────────────────────┐
│         Presentation Layer                  │
│  (Controllers, REST API, Exception Handler) │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│         Application Layer                   │
│      (Facades, Service Orchestration)       │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│          Business Layer                     │
│    (Services, Domain Logic, Validators)     │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│         Persistence Layer                   │
│      (Repositories, JPA Entities)           │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│           Database Layer                    │
│          (MariaDB, Redis)                   │
└─────────────────────────────────────────────┘
```

### 컴포넌트 다이어그램

```
┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│   Client     │─────→│    Nginx     │─────→│ Spring Boot  │
│ (Web/Mobile) │      │ (Port 80/443)│      │   (8080)     │
└──────────────┘      └──────────────┘      └──────────────┘
                                                    │
                                  ┌─────────────────┼─────────────────┐
                                  │                 │                 │
                            ┌─────▼─────┐    ┌─────▼─────┐    ┌─────▼─────┐
                            │  MariaDB  │    │   Redis   │    │ External  │
                            │  (3307)   │    │  (6380)   │    │    APIs   │
                            └───────────┘    └───────────┘    └───────────┘
```

### 도메인 주도 설계 (DDD)

프로젝트는 **7개의 독립적인 도메인**으로 구성되어 있으며, 각 도메인은 자체 엔티티, 서비스, 리포지토리를 포함합니다:

```
com.carecode.domain/
├── user/              # 사용자 및 인증
├── careFacility/      # 돌봄 시설
├── community/         # 커뮤니티
├── health/            # 건강 관리
├── policy/            # 정책 정보
├── notification/      # 알림
├── chatbot/           # AI 챗봇
└── admin/             # 관리자
```

---

## 도메인 구조

### 1. User Domain (사용자)

**엔티티**:
- `User`: 사용자 기본 정보, 로그인, 이메일 인증
- `Child`: 자녀 정보 (이름, 생년월일, 성별)
- `EmailVerificationToken`: 이메일 인증 토큰
- `NotificationSettings`: 사용자별 알림 설정

**주요 API**:
```
POST   /auth/login                    - 일반 로그인 (응답: TokenDto JSON)
POST   /auth/register                 - 회원가입 (응답: TokenDto JSON)
POST   /auth/refresh                  - 액세스·리프레시 토큰 갱신
POST   /auth/kakao/login              - 카카오 code 교환 (응답: TokenDto JSON, isNewUser 포함)
GET    /auth/kakao/login-url          - 카카오 인가 URL (설정: KAKAO_CLIENT_ID, kakao.redirect-uri)
POST   /auth/kakao/complete-registration - 카카오 가입 완료 (Bearer JWT)
POST   /auth/logout                   - 로그아웃 (Bearer 필요, Redis 저장 시 해당 사용자 리프레시 토큰 전부 폐기)
```

리프레시 토큰을 Redis에서 관리하려면 `jwt.refresh-token.store=redis`(또는 환경 변수 `JWT_REFRESH_TOKEN_STORE=redis`)와 동작 중인 Redis를 설정합니다. 기본값 `none`이면 JWT 서명 검증만 수행합니다.

### 2. CareFacility Domain (돌봄 시설)

**엔티티**:
- `CareFacility`: 시설 정보 (위치, 운영 시간, 정원, 프로그램)
- `CareFacilityBooking`: 예약 정보
- `Review`: 시설 리뷰
- `FacilityType`: KINDERGARTEN, DAYCARE, PLAYGROUP, NURSERY

**주요 API**:
```
GET    /facilities                          - 시설 목록
GET    /facilities/{id}                     - 시설 상세
GET    /facilities/type/{facilityType}      - 유형별 조회
GET    /facilities/location/{location}      - 지역별 조회
POST   /facilities/{id}/bookings            - 예약 생성
PUT    /facilities/{id}/bookings/{bid}      - 예약 수정
DELETE /facilities/{id}/bookings/{bid}      - 예약 취소
```

### 3. Community Domain (커뮤니티)

**엔티티**:
- `Post`: 게시글
- `Comment`: 댓글 (대댓글 지원)
- `Tag`: 해시태그
- `PostLike`: 좋아요
- `Bookmark`: 북마크
- `PostCategory`: GENERAL, QUESTION, SHARE, REVIEW, NEWS, EVENT, NOTICE

**주요 API**:
```
GET    /community/posts                     - 게시글 목록 (페이징)
POST   /community/posts                     - 게시글 작성
GET    /community/posts/{postId}            - 게시글 상세
PUT    /community/posts/{postId}            - 게시글 수정
DELETE /community/posts/{postId}            - 게시글 삭제
POST   /community/posts/{postId}/like       - 좋아요
POST   /community/posts/{postId}/bookmark   - 북마크
```

### 4. Health Domain (건강 관리)

**엔티티**:
- `HealthRecord`: 건강 기록 (CHECKUP, VACCINATION, ILLNESS, GROWTH)
- `HealthRecordAttachment`: 첨부파일 (진단서, 검진표)
- `Hospital`: 병원 정보
- `HospitalReview`: 병원 리뷰
- `HospitalLike`: 병원 좋아요

**주요 API**:
```
POST   /health/records                      - 건강 기록 등록
GET    /health/records/{recordId}           - 건강 기록 조회
GET    /health/records/user/{userId}        - 사용자 건강 기록
GET    /health/hospitals                    - 병원 목록
GET    /health/statistics                   - 건강 통계
```

### 5. Policy Domain (정책)

**엔티티**:
- `Policy`: 정책 정보
- `PolicyCategory`: 정책 카테고리
- `PolicyDocument`: 정책 관련 문서

**주요 API**:
```
GET    /policies                            - 정책 목록
GET    /policies/{policyId}                 - 정책 상세
POST   /policies/search                     - 정책 검색
GET    /policies/category/{category}        - 카테고리별 정책
```

### 6. Notification Domain (알림)

**엔티티**:
- `Notification`: 알림 메시지
- `NotificationTemplate`: 알림 템플릿
- `NotificationPreference`: 알림 채널 선호도
- `NotificationSettings`: 전역 알림 설정

**알림 타입**:
- BOOKING: 예약 관련
- REVIEW: 리뷰 관련
- COMMENT: 댓글
- POLICY: 정책 업데이트
- SYSTEM: 시스템 알림

### 7. Chatbot Domain (챗봇)

**엔티티**:
- `ChatSession`: 대화 세션
- `ChatMessage`: 메시지 (사용자/봇)

**주요 API**:
```
POST   /chatbot/chat                        - 메시지 전송
GET    /chatbot/sessions                    - 세션 목록
GET    /chatbot/history                     - 대화 기록
```

### 8. Admin Domain (관리자)

**주요 기능**:
- 사용자 관리 (조회, 정지, 삭제)
- 시설/병원 데이터 관리
- 커뮤니티 모니터링
- 정책 데이터 관리
- 대시보드 및 통계

---

## 빠른 시작

### 필요한 것

- **Docker Desktop** (Compose v2 포함). 이것만 있으면 됩니다. JDK·MariaDB·Redis 를 따로 설치하지 않습니다.
- IDE 에서 앱을 직접 띄우려면 **JDK 17** 이 추가로 필요합니다 (Gradle 은 포함된 `./gradlew` 를 씁니다).

### 1. 한 줄로 띄우기

```bash
git clone https://github.com/CareCode-Repo/CareCode_Interface.git
cd CareCode_Interface
docker compose up --build
```

앱 + MariaDB + Redis 가 함께 뜹니다. **`.env` 없이도 뜹니다.** 로컬 전용 기본값이 `docker-compose.yml` 에 들어 있습니다.
처음에는 이미지와 의존성을 받고 스키마를 만드느라 몇 분 걸립니다(`docker compose ps` 에서 app 이 `healthy` 가 되면 준비 완료).

| 무엇 | 주소 |
|------|------|
| API | http://localhost:8082 |
| 헬스체크 | http://localhost:8082/actuator/health |
| Swagger UI | http://localhost:8082/swagger-ui.html |
| MariaDB | `localhost:3307` (DB·계정 `carecode` / 비밀번호 `carecode-local`) |
| Redis | `localhost:6380` |

포트가 이미 쓰이고 있으면 바꿔서 띄웁니다: `APP_PORT=18082 DB_PORT=3317 docker compose up --build`

스키마는 **Flyway 마이그레이션(V1~V18)으로 만들고 Hibernate 는 검증만** 합니다(운영과 같은 방식).
엔티티와 마이그레이션이 어긋나면 로컬에서도 기동이 실패하므로 바로 알 수 있습니다.

### 2. 가입하고 로그인해 보기

```bash
# 가입 (역할은 서버가 PARENT 로 정한다)
curl -X POST http://localhost:8082/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"me@example.com","password":"password1234","name":"테스트"}'

# 로그인 → 본문에 accessToken 이 온다. 리프레시 토큰은 HttpOnly 쿠키(refreshToken)로도 내려간다.
curl -X POST http://localhost:8082/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"me@example.com","password":"password1234"}'

# 받은 토큰으로 내 정보
curl http://localhost:8082/users/me -H "Authorization: Bearer <accessToken>"
```

> Windows 의 PowerShell·Git Bash 에서 한글이 든 JSON 을 `-d` 로 보내면 인코딩이 깨져
> `400 요청 본문을 읽을 수 없습니다` 가 납니다. 이름을 영문으로 바꾸거나 Swagger UI 에서 호출하세요.

미리 만들어 둔 관리자 계정은 없습니다. 관리자 화면(`/api/admin/**`)을 보려면 가입한 계정을 DB 에서 올립니다.

```bash
docker compose exec mariadb mariadb -ucarecode -pcarecode-local carecode \
  -e "UPDATE TBL_USER SET ROLE='ADMIN' WHERE EMAIL='me@example.com';"
```

역할은 토큰에 실리므로 **다시 로그인**해야 반영됩니다.

### 3. 화면을 채우고 싶다면 — 샘플 데이터

공공데이터 키가 없으면 시설·정책 목록이 비어 있습니다. 샘플 데이터를 넣고 띄울 수 있습니다.

```bash
SEED_SAMPLE_DATA=true docker compose up --build
```

실제 지원 금액이 아닙니다. 확인이 끝나면 관리자 토큰으로 `DELETE /api/admin/dev/sample-data` 를 호출해 지웁니다.

### 4. 외부 연동을 켜려면 — `.env`

카카오 로그인·인증 메일·공공데이터 동기화·AI 챗봇은 외부 키가 있어야 동작합니다.
키가 없어도 앱은 뜨고, **해당 기능만** 실패하거나 대체 동작(챗봇은 규칙 기반 응답)을 합니다.

```bash
cp .env.example .env   # 필요한 줄만 주석을 풀고 값을 넣는다
docker compose up --build
```

항목별 설명은 [`.env.example`](.env.example) 에 있습니다. `.env` 는 커밋되지 않습니다.

### 5. 프런트엔드와 함께

[CareCode_FE](https://github.com/CareCode-Repo/CareCode_FE) 를 `http://localhost:3000` 에서 띄우면 그대로 붙습니다.
CORS 허용 오리진과 로컬용 쿠키 설정(`Secure=false`, `SameSite=Lax`)이 compose 에 이미 들어 있습니다.

### IDE 에서 앱만 띄우기

DB·Redis 만 컨테이너로 띄우고 앱은 IDE 나 Gradle 로 실행합니다.

```bash
docker compose up -d mariadb redis

DB_URL=jdbc:mariadb://localhost:3307/carecode DB_USERNAME=carecode DB_PASSWORD=carecode-local \
REDIS_PORT=6380 SPRING_FLYWAY_ENABLED=true \
JWT_SECRET=local-only-jwt-secret-do-not-use-in-production-0123456789 \
KAKAO_CLIENT_ID=x KAKAO_CLIENT_SECRET=x MAIL_USERNAME=x MAIL_PASSWORD=x \
EMAIL_VERIFICATION_BASE_URL=http://localhost:8082 \
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 정리

```bash
docker compose down        # 컨테이너만 내린다 (데이터 유지)
docker compose down -v     # DB·업로드 볼륨까지 지운다
```

### 테스트

```bash
./gradlew test
```

H2 기반 테스트는 Docker 없이 돕니다. 스키마 정합성 테스트(`FlywaySchemaValidationTest`)는 Testcontainers 로
MariaDB 를 띄우므로 Docker 가 켜져 있어야 하고, 꺼져 있으면 건너뛰고 끝에 경고를 띄웁니다.
자세한 내용은 [회귀 방지](docs/quality/regression-safety.md) 를 보세요.

---

## API 문서

### 설계 문서

기능별 상세 문서와 아키텍처는 [`docs/`](docs/README.md) 에 있습니다.
무엇을 만들었는지보다 **왜 그렇게 만들었는지**를 남기는 것을 목표로 합니다.

| 문서 | 내용 |
|------|------|
| [시스템 개요](docs/architecture/system-overview.md) | 계층 구조, 요청·배치 흐름 |
| [데이터 흐름](docs/architecture/data-flow.md) | 공공데이터 수집 → 알림까지의 파이프라인 |
| [공공데이터 연동](docs/features/public-data-integration.md) | 4개 정부 API, 공급자 추상화 |
| [지원금 지능화](docs/features/benefit-intelligence.md) | 추천·비교·놓친 지원금·금액 신뢰도 |
| [시설 지능화](docs/features/facility-intelligence.md) | 정원 시계열·입소 예측·빈자리 알림 |
| [알림과 리텐션](docs/features/notification-and-retention.md) | 알림 3종과 중복 방지 |
| [기동 안정화](docs/quality/runtime-hardening.md) | 실기동에서 드러난 차단 8건 |
| [회귀 방지](docs/quality/regression-safety.md) | 왜 CI 가 못 잡았는지 |

### Swagger UI

프로젝트는 **SpringDoc OpenAPI 3**를 사용하여 자동으로 API 문서를 생성합니다.
**운영(prod) 프로파일에서는 차단됩니다.**

**접속 URL**: http://localhost:8082/swagger-ui.html (로컬 `dev` 프로파일)

### 주요 API 엔드포인트

| 도메인 | 엔드포인트 | 설명 |
|-------|----------|------|
| 인증 | `/auth/*` | 로그인, 회원가입, 토큰 갱신 |
| 사용자 | `/users/*` | 사용자 정보 관리 |
| 시설 | `/facilities/*` | 돌봄 시설 조회, 예약 |
| 커뮤니티 | `/community/*` | 게시글, 댓글 관리 |
| 건강 | `/health/*` | 건강 기록, 병원 정보 |
| 정책 | `/policies/*` | 정책 조회, 검색 |
| 알림 | `/notifications/*` | 알림 조회, 설정 |
| 챗봇 | `/chatbot/*` | 챗봇 대화 |
| 관리자 | `/api/admin/*` | 관리자 기능 (JWT + `ROLE_ADMIN` 필요) |

### 인증

대부분의 API는 **JWT 토큰 인증**이 필요합니다. 토큰은 응답 **JSON 본문**(`TokenDto`: `accessToken`, `refreshToken`, `expiresIn` 등)으로 내려가며, 카카오 로그인도 동일합니다. 컨트롤러 단위 `@CrossOrigin`은 사용하지 않으며, 허용 오리진은 **`app.security.cors.allowed-origins`**(쉼표 구분)와 `SecurityConfig`의 CORS 설정만 따릅니다. `jwt.refresh-token.store=redis`이면 로그인·갱신 시 발급한 리프레시 토큰이 Redis에 등록되고, `/auth/refresh`는 등록된 토큰만 갱신하며 `POST /auth/logout`으로 해당 사용자의 리프레시 세션을 일괄 폐기할 수 있습니다(액세스 토큰 블랙리스트는 포함하지 않음).

**헤더 예시**:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**로그인**: 가입·로그인 예시는 [빠른 시작](#2-가입하고-로그인해-보기) 에 있습니다.
로그인 응답 본문에 액세스 토큰이 실리고, 리프레시 토큰은 HttpOnly 쿠키로도 내려갑니다.

---

## 배포 가이드

운영 배포는 **GitHub Actions** 가 합니다. 서버에서 직접 빌드하지 않습니다.

```
main 에 머지
  → 테스트 + 스키마 정합성 검사 + 커버리지 게이트
  → Docker 이미지 빌드 → GHCR(ghcr.io) 에 push
  → 서버에 SSH 접속
      1) 새 이미지를 예비 포트(127.0.0.1:18082)에 먼저 띄워 헬스체크
      2) 통과하면 운영 컨테이너(8082)를 새 이미지로 교체
      3) 실패하면 예비 컨테이너만 지우고 기존 운영은 그대로 둔다
  → 외부 헬스체크 URL 로 한 번 더 확인 → Slack 알림
```

`develop` 에 머지하면 같은 흐름으로 스테이징에 배포됩니다. Actions 탭에서 수동 실행(`workflow_dispatch`)도 됩니다.

필요한 GitHub 시크릿, 서버 쪽 전제(`.env` 위치, 포트), Blue/Green 을 쓰지 않는 이유는
[운영 문서의 배포 절](docs/features/operations.md#배포) 에 정리돼 있습니다.

### 환경별 프로파일

| 프로파일 | 파일 | 쓰는 곳 |
|---------|------|--------|
| (기본) | `application.yml` | 모든 환경의 공통값 |
| `dev` | `application-dev.yml` | 로컬·`docker compose`. Swagger 공개, 상세 로그, 샘플 데이터 옵션 |
| `prod` | `application-prod.yml` | 운영. Swagger 차단, 필수 환경변수가 없으면 기동 실패 |

운영 설정은 비밀값을 기본값으로 두지 않습니다. `JWT_SECRET`, `EMAIL_VERIFICATION_BASE_URL` 같은 값이 빠지면
잘못된 값으로 조용히 뜨는 대신 **기동 단계에서 멈춥니다.**

### 데이터베이스 마이그레이션

스키마는 **Flyway** 로 관리합니다 (`src/main/resources/db/migration`, 현재 V1~V18).
운영은 `ddl-auto=validate` 라 엔티티와 스키마가 다르면 기동하지 않습니다.

엔티티에 필드나 테이블을 추가하면 **반드시 다음 번호의 마이그레이션도 함께** 작성합니다.
빠뜨리면 CI 의 스키마 정합성 테스트가 실패합니다.

---

## 데이터베이스 구조

MariaDB 10.11, 테이블 38개(Flyway 이력 테이블 제외). 전체 ERD 는 [docs/ERD.md](docs/ERD.md) 에 있습니다.

### 주요 테이블

| 테이블 | 설명 |
|--------|------|
| `TBL_USER` | 사용자. 이메일 가입과 카카오 가입을 함께 담는다 |
| `TBL_CHILD` | 자녀 |
| `TBL_CARE_FACILITIES` | 돌봄 시설 (공공데이터 동기화) |
| `care_facility_bookings` | 시설 방문·상담 예약 |
| `TBL_POST`, `TBL_COMMENT` | 커뮤니티 게시글·댓글 |
| `TBL_HEALTH_RECORD` | 건강 기록 |
| `TBL_VACCINATION_SCHEDULE` | 예방접종 일정 |
| `TBL_POLICIES` | 육아 지원 정책 |
| `TBL_NOTIFICATION` | 알림 |
| `TBL_CHAT_SESSIONS`, `TBL_CHAT_MESSAGES` | 챗봇 대화 |

---
## 개발 로드맵

### Phase 1: 긴급 개선 (완료)

- [x] 예외 처리 일관성 개선
- [x] 설정 파일 환경별 분리
- [x] 기본 테스트 코드 작성
- [x] 챗봇 응답 DTO 추가

### Phase 2: 중요 개선 (진행 중)

- [x] JSON 형식 로깅 (Logstash)
- [x] 캐싱 전략 확장 (Redis)
- [x] 트랜잭션 관리 개선
- [x] 통합 테스트 확대 (접근제어·스키마 정합성·API 계약)
- [ ] API 응답 시간 최적화

### Phase 3: 장기 개선 (예정)

- [x] N+1 쿼리 최적화
- [ ] 모니터링 대시보드 구축
- [ ] API 버전 관리 (v2)
- [ ] WebSocket 기반 실시간 알림
- [ ] 챗봇 AI 모델 고도화
- [ ] 모바일 앱 개발 (React Native)

### 향후 계획

- **2025 Q2**: 공공 데이터 API 확대 (전국 17개 시도)
- **2025 Q3**: 챗봇 GPT-4 모델 적용
- **2025 Q4**: 모바일 앱 출시

---

## 프로젝트 구조

```
CareCode_Interface/
├── src/main/java/com/carecode/
│   ├── domain/                     # 도메인별 패키지 (controller / service / repository / entity / dto / mapper)
│   │   ├── user/                   #   가입·로그인·카카오·프로필
│   │   ├── careFacility/           #   시설 조회·예약·리뷰·대기·입소 예측
│   │   ├── community/              #   게시글·댓글·좋아요·신고
│   │   ├── health/                 #   건강 기록·병원·예방접종·성장 곡선
│   │   ├── policy/                 #   지원 정책·추천·놓친 지원금
│   │   ├── notification/           #   알림·채널 설정·푸시
│   │   ├── chatbot/                #   AI 챗봇
│   │   └── admin/                  #   관리자 API (/api/admin/**)
│   ├── core/                       # 공통
│   │   ├── security/               #   JWT 필터, SecurityConfig, CurrentUserFacade
│   │   ├── aspect/                 #   로깅·Rate Limit·입력 검증 AOP
│   │   ├── handler/                #   전역 예외 → HTTP 상태 매핑
│   │   ├── exception/              #   ErrorCode, 도메인 예외
│   │   ├── config/ scheduler/ storage/ monitoring/ ...
│   │   └── devtools/               #   로컬 샘플 데이터
│   └── CareCodeApplication.java
├── src/main/resources/
│   ├── application.yml / application-dev.yml / application-prod.yml
│   ├── db/migration/               # Flyway V1~V18
│   ├── legal/                      # 개인정보처리방침·약관
│   └── public-data/                # 공공데이터 매핑
├── src/test/                       # 단위(Mockito) · 통합(H2) · 스키마(Testcontainers MariaDB)
├── docs/                           # 설계 문서 (아래 표)
├── .github/workflows/ci-cd.yml     # 테스트 → 이미지 → 배포
├── docker-compose.yml              # 로컬 실행 (앱 + MariaDB + Redis)
├── .env.example                    # 선택 연동 키 예시
└── Dockerfile
```

| 문서 | 내용 |
|------|------|
| [접근제어 매트릭스](docs/reference/access-control-matrix.md) | 경로별 공개/인증/관리자 여부와 근거 |
| [마이그레이션](docs/reference/database-migrations.md) | Flyway 버전별 변경 이력 |
| [운영](docs/features/operations.md) | 배포·헬스체크·알림·스케줄러 |
| [이슈 관리](docs/ISSUE_MANAGEMENT.md) | 이슈 제목·라벨·마일스톤 규칙 |

---
## 주요 개선 사항

### 1. 예외 처리 개선

- **ErrorCode Enum**: 도메인별 에러 코드 체계화
- **CareServiceException**: 공통 예외 클래스
- **GlobalExceptionHandler**: 일관된 에러 응답 형식

### 2. 로깅 개선

- **Logback + Logstash**: JSON 형식 구조화 로깅
- **LogExecutionTime AOP**: 메서드 실행 시간 자동 로깅
- **요청/응답 로깅**: 모든 HTTP 요청/응답 로깅

### 3. 캐싱 전략

- **Redis 캐싱**: 자주 조회되는 데이터 캐싱
- **TTL 설정**: 데이터 유형별 차별화된 TTL
  - 건강 기록: 5분
  - 정책: 30분
  - 돌봄 시설: 15분
  - 사용자 정보: 10분

### 4. AOP 기반 공통 기능

- `@LogExecutionTime`: 실행 시간 로깅
- `@RateLimit`: Rate Limiting (로그인 시도 제한)
- `@RequireAuthentication`: 인증 필수(일부 컨트롤러는 `SecurityConfig` + `@PreAuthorize("isAuthenticated()")`로 대체)
- `@RequireAdminRole`: 관리자 권한 필수
- `@ValidateLocation`: 지역 검증
- `@ValidateChildAge`: 연령대 검증

### 5. 성능 최적화

- **N+1 쿼리 해결**: `@EntityGraph`·`@BatchSize`, 댓글 트리는 한 번에 읽어 메모리에서 조립
- **페이징 처리**: 대용량 데이터 효율적 조회
- **인덱스 최적화**: 50개 이상의 인덱스 설정

---

## 기여하기

CareCode Interface는 오픈소스 프로젝트입니다. 기여를 환영합니다!

### 기여 방법

1. **이슈 등록**: 버그 발견 또는 기능 제안
2. **Fork**: 저장소를 Fork합니다
3. **브랜치 생성**: `git checkout -b feature/amazing-feature`
4. **커밋**: `git commit -m 'feat: Add some amazing feature'`
5. **푸시**: `git push origin feature/amazing-feature`
6. **Pull Request**: PR을 생성하고 리뷰 요청

### 커밋 메시지 규칙

```
feat: 새로운 기능 추가
fix: 버그 수정
refactor: 코드 리팩토링
docs: 문서 수정
test: 테스트 코드 추가/수정
chore: 빌드, 설정 파일 수정
style: 코드 포맷팅 (기능 변경 없음)
```

### 코드 스타일

- **Java**: Google Java Style Guide
- **들여쓰기**: 스페이스 4칸
- **줄 길이**: 최대 120자

---

## 라이선스

이 프로젝트의 **소스 코드**는 **MIT 라이선스**를 따릅니다. Copyright (c) 2025-2026 CareCode.
전문은 [LICENSE](LICENSE) 파일에 있습니다.

코드에 담긴 **외부 데이터는 MIT 적용 대상이 아니며** 각 제공처의 이용 조건을 따릅니다.

| 데이터 | 출처 | 쓰이는 곳 |
|--------|------|-----------|
| 어린이집·유치원·병원·지원금 정보 | 공공데이터포털, 보육통합정보, 유치원알리미, 서울 열린데이터광장 | 동기화로 받아오며 저장소에 포함하지 않음 |
| 아동 성장 표준(LMS) | WHO Child Growth Standards | `domain/health/growth/GrowthStandardTable` |

---

## 팀 정보

**프로젝트명**: CareCode Interface (맘편한)
**개발 기간**: 2024.10 ~ 현재
**참여 인원**: 5명
**GitHub**: https://github.com/CareCode-Repo

---

## 연락처

프로젝트에 대한 문의사항이 있으시면 아래로 연락 주세요: 정보제공 책임자 - 오태훈

- **이메일**: dhxogns920@gmail.com
- **이슈 트래커**: https://github.com/CareCode-Repo/CareCode_Interface/issues

---

## 감사의 글

이 프로젝트는 **오픈소스 개발자 경진대회**를 위해 개발되었습니다.

육아를 하는 모든 부모님들의 행복한 육아를 응원합니다.

**CareCode Interface - 부모를 위한 통합 육아 지원 플랫폼**
