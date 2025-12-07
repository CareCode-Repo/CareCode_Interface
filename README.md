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

1. [프로젝트 소개](#-프로젝트-소개)
2. [프로젝트 배경 및 목적](#-프로젝트-배경-및-목적)
3. [핵심 기능](#-핵심-기능)
4. [기술 스택](#-기술-스택)
5. [시스템 아키텍처](#-시스템-아키텍처)
6. [도메인 구조](#-도메인-구조)
7. [빠른 시작](#-빠른-시작)
8. [API 문서](#-api-문서)
9. [배포 가이드](#-배포-가이드)
10. [개발 로드맵](#-개발-로드맵)
11. [기여하기](#-기여하기)
12. [라이선스](#-라이선스)

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
| CI/CD | GitHub Actions | 자동화된 빌드 및 배포 |

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
POST   /auth/login                  - 로그인
POST   /auth/register               - 회원가입
POST   /auth/refresh-token          - 토큰 갱신
GET    /auth/me                     - 현재 사용자 정보
POST   /auth/logout                 - 로그아웃
GET    /auth/kakao/callback         - 카카오 로그인 콜백
```

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

### 필수 요구사항

- **Java**: 17 이상
- **Docker**: 20.x 이상
- **Docker Compose**: 2.x 이상
- **Gradle**: 8.x 이상 (또는 포함된 Gradle Wrapper 사용)

### 로컬 개발 환경 설정

#### 1. 프로젝트 클론

```bash
git clone https://github.com/your-organization/carecode-interface.git
cd carecode-interface
```

#### 2. 환경 변수 설정

`.env` 파일을 생성하고 다음 정보를 입력하세요:

```env
# Database
DB_USERNAME=carecode_user
DB_PASSWORD=your_secure_password
DB_NAME=carecode_db

# JWT
JWT_SECRET=your_jwt_secret_key_here

# Redis
REDIS_HOST=localhost
REDIS_PORT=6380

# OAuth2
KAKAO_CLIENT_ID=your_kakao_client_id
KAKAO_CLIENT_SECRET=your_kakao_client_secret
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret

# Email
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_email_app_password

# Public Data API
PUBLIC_DATA_API_KEY=your_public_data_api_key
```

#### 3. 프로젝트 빌드

```bash
./gradlew clean build
```

#### 4. Docker로 실행

```bash
docker-compose up -d
```

#### 5. 애플리케이션 접속

- **웹 애플리케이션**: http://localhost
- **API 문서 (Swagger UI)**: http://localhost/swagger-ui.html
- **MariaDB**: localhost:3307
- **Redis**: localhost:6380

### 로컬 개발 (IDE)

데이터베이스와 Redis만 Docker로 실행하고, Spring Boot 애플리케이션은 IDE에서 실행:

```bash
# 데이터베이스/레디스만 실행
docker-compose up carecode-mariadb carecode-redis -d

# 애플리케이션 실행
./gradlew bootRun
```

---

## API 문서

### Swagger UI

프로젝트는 **SpringDoc OpenAPI 3**를 사용하여 자동으로 API 문서를 생성합니다.

**접속 URL**: http://localhost/swagger-ui.html

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
| 관리자 | `/admin/*` | 관리자 기능 |

### 인증

대부분의 API는 **JWT 토큰 인증**이 필요합니다.

**헤더 예시**:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**로그인 API**:
```bash
curl -X POST http://localhost/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@carecode.com",
    "password": "admin123"
  }'
```

**응답**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600
}
```

---

## 배포 가이드

### Docker Compose를 이용한 배포

#### 1. 프로덕션 빌드

```bash
./gradlew clean build -Pprofile=prod
```

#### 2. Docker 이미지 빌드

```bash
docker build -t carecode-interface:latest .
```

#### 3. Docker Compose 실행

```bash
docker-compose up -d
```

#### 4. 로그 확인

```bash
# 전체 로그
docker-compose logs -f

# API 로그만
docker-compose logs -f carecode-api

# 특정 줄 수만
docker-compose logs -f --tail=100 carecode-api
```

#### 5. 컨테이너 상태 확인

```bash
docker-compose ps
```

### 환경별 프로파일

- **dev**: 개발 환경 (`application-dev.yml`)
- **prod**: 프로덕션 환경 (`application-prod.yml`)
- **docker**: Docker 환경 (`application-docker.yml`)

프로파일 변경:
```bash
SPRING_PROFILES_ACTIVE=prod docker-compose up -d
```

### 데이터베이스 마이그레이션

초기 데이터베이스 스키마는 JPA의 `ddl-auto` 설정으로 자동 생성됩니다.

**프로덕션 환경**에서는 `ddl-auto: validate`로 설정하고, 별도의 마이그레이션 도구(Flyway, Liquibase 등)를 사용할 것을 권장합니다.

---

## 데이터베이스 구조

### ERD (Entity Relationship Diagram)

총 **25개 테이블**로 구성되어 있습니다.

자세한 ERD는 [ERD 문서](src/main/resources/documents/ERD.md)를 참고하세요.

### 주요 테이블

| 테이블명 | 설명 | 주요 컬럼 |
|---------|------|----------|
| TBL_USER | 사용자 | user_id, email, password, username |
| TBL_CHILD | 자녀 정보 | child_id, user_id, name, birth_date |
| TBL_CARE_FACILITY | 돌봄 시설 | facility_id, name, type, location |
| TBL_POST | 게시글 | post_id, user_id, title, content, category |
| TBL_HEALTH_RECORD | 건강 기록 | record_id, child_id, record_type, date |
| TBL_POLICY | 정책 | policy_id, title, category, target_age |
| TBL_NOTIFICATION | 알림 | notification_id, user_id, type, message |
| TBL_CHAT_SESSION | 챗봇 세션 | session_id, user_id, created_at |

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
- [ ] 통합 테스트 확대
- [ ] API 응답 시간 최적화

### Phase 3: 장기 개선 (예정)

- [ ] N+1 쿼리 최적화
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
├── src/
│   ├── main/
│   │   ├── java/com/carecode/
│   │   │   ├── domain/                   # 도메인별 패키지
│   │   │   │   ├── user/                 # 사용자 도메인
│   │   │   │   │   ├── controller/
│   │   │   │   │   ├── service/
│   │   │   │   │   ├── repository/
│   │   │   │   │   ├── entity/
│   │   │   │   │   └── dto/
│   │   │   │   ├── careFacility/         # 돌봄 시설 도메인
│   │   │   │   ├── community/            # 커뮤니티 도메인
│   │   │   │   ├── health/               # 건강 도메인
│   │   │   │   ├── policy/               # 정책 도메인
│   │   │   │   ├── notification/         # 알림 도메인
│   │   │   │   ├── chatbot/              # 챗봇 도메인
│   │   │   │   └── admin/                # 관리자 도메인
│   │   │   ├── core/                     # 공통 컴포넌트
│   │   │   │   ├── security/             # 보안 (JWT, OAuth2)
│   │   │   │   ├── exception/            # 예외 처리
│   │   │   │   ├── config/               # 설정
│   │   │   │   ├── aop/                  # AOP 기능
│   │   │   │   └── util/                 # 유틸리티
│   │   │   └── CareCodeApplication.java  # 메인 클래스
│   │   └── resources/
│   │       ├── application.yml           # 기본 설정
│   │       ├── application-dev.yml       # 개발 환경 설정
│   │       ├── application-prod.yml      # 프로덕션 설정
│   │       ├── application-docker.yml    # Docker 설정
│   │       ├── templates/                # Thymeleaf 템플릿
│   │       ├── static/                   # 정적 리소스
│   │       └── documents/                # 문서
│   │           └── ERD.md                # ERD 문서
│   └── test/                             # 테스트 코드
│       ├── java/com/carecode/
│       │   ├── domain/                   # 도메인별 테스트
│       │   └── core/                     # 공통 기능 테스트
│       └── resources/
│           └── application-test.yml      # 테스트 설정
├── scripts/                              # 배포 스크립트
│   └── deploy.sh
├── .github/                              # GitHub 설정
│   ├── workflows/                        # CI/CD 워크플로우
│   ├── ISSUE_TEMPLATE/                   # 이슈 템플릿
│   └── pull_request_template.md          # PR 템플릿
├── docker-compose.yml                    # Docker Compose 설정
├── Dockerfile                            # Docker 이미지 빌드
├── build.gradle                          # Gradle 빌드 설정
├── settings.gradle                       # Gradle 프로젝트 설정
├── .env                                  # 환경 변수 (Git 제외)
├── .gitignore                            # Git 제외 파일
├── README.md                             # 프로젝트 소개 (본 문서)
├── ARCHITECTURE_IMPROVEMENTS.md          # 아키텍처 개선 문서
└── IMPROVEMENTS_SUMMARY.md               # 개선 사항 요약
```

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
- `@RequireAuthentication`: 인증 필수
- `@RequireAdminRole`: 관리자 권한 필수
- `@ValidateLocation`: 지역 검증
- `@ValidateChildAge`: 연령대 검증

### 5. 성능 최적화

- **N+1 쿼리 해결**: JOIN FETCH 활용
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

이 프로젝트는 **MIT 라이선스**를 따릅니다. 자세한 내용은 [LICENSE](LICENSE) 파일을 참고하세요.

---

## 팀 정보

**프로젝트명**: CareCode Interface (맘편한)
**개발 기간**: 2024.10 ~ 현재
**참여 인원**: 5명
**GitHub**: https://github.com/your-organization/carecode-interface

---

## 연락처

프로젝트에 대한 문의사항이 있으시면 아래로 연락 주세요: 정보제공 책임자 - 오태훈

- **이메일**: dhxogns920@gmail.com
- **이슈 트래커**: https://github.com/CareCode-Repo/carecode-interface/issues

---

## 감사의 글

이 프로젝트는 **오픈소스 개발자 경진대회**를 위해 개발되었습니다.

육아를 하는 모든 부모님들의 행복한 육아를 응원합니다.

**CareCode Interface - 부모를 위한 통합 육아 지원 플랫폼**
