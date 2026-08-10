# 접근제어 매트릭스

## 왜 이 문서가 필요한가

SecurityConfig 는 **앞선 규칙이 뒤를 덮습니다.** 게다가 클래스 레벨 `@PreAuthorize` 가
URL 규칙을 다시 덮습니다. 두 겹이 서로 다른 말을 할 수 있습니다.

실제로 병원 조회는 공개 규칙이 선언돼 있는데도 **전부 로그인 필수**였습니다.
규칙 목록만 읽어서는 알 수 없었고, 호출해 보고서야 드러났습니다.

그래서 이 문서와 [접근제어 계약 테스트](../quality/regression-safety.md#대응-2--접근제어-계약-테스트)를
함께 둡니다. 문서는 의도를, 테스트는 사실을 기록합니다.

## 두 겹의 접근제어

```mermaid
flowchart TD
    REQ[요청] --> URL{SecurityConfig<br/>URL 규칙}
    URL -->|거부| E401[401]
    URL -->|통과| PRE{메서드<br/>@PreAuthorize}
    PRE -->|거부| E403[403]
    PRE -->|통과| CTRL[컨트롤러 진입]
    CTRL --> CONSENT{ConsentGuard<br/>민감정보}
    CONSENT -->|미동의| E403C["403<br/>CONSENT_REQUIRED"]
    CONSENT -->|동의| OK[처리]

    style E401 fill:#f8d7da,stroke:#dc3545
    style E403 fill:#f8d7da,stroke:#dc3545
    style E403C fill:#fff3cd,stroke:#ffc107
    style OK fill:#d4edda,stroke:#28a745
```

**URL 규칙만 열어서는 부족합니다.** 클래스에 `@PreAuthorize("isAuthenticated()")` 가 붙어 있으면
메서드에도 `@PreAuthorize("permitAll()")` 를 붙여야 실제로 열립니다.

## 공개 경로 (로그인 불필요)

### 시스템·문서

| 경로 | 비고 |
|------|------|
| `/actuator/health`, `/actuator/info`, `/actuator/prometheus` | 로드밸런서·모니터링 |
| `/legal/privacy-policy`, `/legal/terms`, `/legal/version` | **동의하기 전에 읽어야 하므로 공개** |
| `/`, `/error`, `/favicon.ico` | — |
| `/css/**`, `/js/**`, `/images/**`, `/static/**` | 정적 리소스 |

### 인증 흐름

| 경로 | 비고 |
|------|------|
| `/auth/login`, `/auth/register` | — |
| `/auth/refresh` | 토큰 갱신 |
| `/auth/kakao/login`, `/auth/kakao/login-url`, `/auth/kakao/complete-registration` | 카카오 |
| `/oauth2/**` | — |
| `/users/send-code`, `/users/verify-code`, `/users/verify` | 이메일 인증 |

### 지원금

| 경로 | 비고 |
|------|------|
| `/policies` | 목록 |
| `/policies/search` | 검색 |
| `/policies/categories` | 분류 |
| `/policies/statistics` | 통계 |
| `/policies/{id}` | 상세 |

### 시설

| 경로 | 비고 |
|------|------|
| `/facilities` | 목록 |
| `/facilities/type/**`, `/facilities/location/**`, `/facilities/age` | 조건별 조회 |
| `/facilities/operating-hours`, `/facilities/radius` | — |
| `/facilities/popular`, `/facilities/new` | — |
| `/facilities/statistics` | — |
| `/facilities/{id}/view` | 조회수 증가 |
| `/facilities/{id}/rating` (GET) | 평점 조회 |
| `/api/public/care-facilities/**` | 공공데이터 조회 |

### 병원

**실제 경로는 `/health/hospitals/**` 입니다.** `/hospitals/**` 가 아닙니다.
`/health/**` → `authenticated()` 보다 **먼저** 선언해야 합니다.

| 경로 | 메서드 | 비고 |
|------|--------|------|
| `/health/hospitals` | GET | 목록 |
| `/health/hospitals/{id}` | GET | 상세 |
| `/health/hospitals/nearby` | GET | 반경 검색 |
| `/health/hospitals/popular` | GET | 인기 |
| `/health/hospitals/type/{type}` | GET | 진료과목별 |
| `/health/hospitals/{id}/reviews` | GET | 리뷰 조회 |
| `/health/hospitals/{id}/likes` | GET | 좋아요 수 |

> 위 7개는 URL 규칙과 **메서드 `@PreAuthorize("permitAll()")` 둘 다** 필요합니다.
> `HealthController` 에 클래스 레벨 `@PreAuthorize("isAuthenticated()")` 가 있기 때문입니다.

### 커뮤니티 (GET 만)

| 경로 |
|------|
| `/community/posts`, `/community/posts/{id}`, `/community/posts/{id}/comments` |
| `/community/search`, `/community/search/all` |
| `/community/popular`, `/community/popular/limit` |
| `/community/latest`, `/community/latest/limit` |
| `/community/tags`, `/community/tags/**` |

## 인증 필요

### 개인화 — 남의 정보가 걸린 경로

| 경로 | 이유 |
|------|------|
| `/policies/recommendations` | 자녀·주소·소득 기반 |
| `/policies/missed-benefits` | 동일 |
| `/policies/regional-comparison` | 동일 |
| `/policies/bookmarks`, `/policies/{id}/bookmarks` | 내 북마크 |
| `POST /policies/{id}/amount-reports` | 제보자 식별 |

### 건강 — 민감정보

| 경로 | 이유 |
|------|------|
| `/health/**` (병원 공개 조회 제외) | 건강기록은 민감정보 |
| `/health/records/**` | 소유권을 서비스 계층에서 다시 검증 |
| `/health/hospitals/{id}/like-status` | **"내" 좋아요 여부라 공개 조회와 구분** |

### 그 외

| 경로 | 비고 |
|------|------|
| `/auth/user/**`, `/auth/logout` | — |
| `/users/privacy/**` | 열람·동의·탈퇴 |
| `/children/**` | 자녀 정보 |
| `/notifications/**` | — |
| `/facilities/search` | 개인화 검색 |
| `/facilities/{id}/bookings/**` | 예약 |
| `/facilities/waitlist/**`, `POST /facilities/{facilityId}/waitlist` | 대기 등록 |
| `/community/comments/**` | 댓글 작성·수정 |
| `POST /facilities/{id}/rating` | 평점 등록 |
| `POST/DELETE /health/hospitals/{id}/like` | 좋아요 등록·해제 |
| `/api/**` | **기본 정책** — 명시하지 않은 `/api` 경로는 인증 필요 |

### 최종 규칙

```java
.anyRequest().authenticated()
```

명시하지 않은 모든 경로는 인증이 필요합니다.
새 컨트롤러를 만들고 규칙을 빠뜨리면 **닫힌 채로 시작**합니다. 안전한 기본값입니다.

## 관리자 전용

`ROLE_ADMIN` 이 필요합니다.

| 경로 | 용도 |
|------|------|
| `/api/admin/**` | 전체 |
| `/api/admin/public-data/*/sync` | 수동 동기화 |
| `/api/admin/public-data/facilities/geocode` | 좌표 보정 |
| `/api/admin/public-data/facilities/notify-vacancy` | 빈자리 알림 실행 |
| `/api/admin/public-data/policies/notify-deadline` | 마감 알림 실행 |
| `/api/admin/analytics/**` | 퍼널·리텐션 |
| `/api/admin/policy-verification/**` | 금액 수기 검증 |
| `/api/admin/reports/**` | 신고 처리 |

## 프로파일별 차이

| 경로 | dev / docker | prod |
|------|--------------|------|
| `/swagger-ui/**`, `/v3/api-docs/**` | 공개 | **차단(401)** |
| `/*.html` | 공개 | 차단 |
| `/kakao-test.html`, `/kakao-debug.html` | 공개 | 차단 |

운영에서 API 문서를 열어두면 공격 표면을 그대로 알려주는 셈입니다.

## 오류 응답

| 상황 | 상태 | 본문 |
|------|------|------|
| 미인증 | 401 | `{"error":"Unauthorized","message":"Authentication required"}` |
| 권한 없음 | 403 | `{"code":"C003","message":"접근 권한이 없습니다"}` |
| 동의 필요 | 403 | `{"error":"CONSENT_REQUIRED","consentType":"...","displayName":"..."}` |
| 없는 경로 | 404 | `{"code":"C004","message":"요청하신 경로를 찾을 수 없습니다"}` |
| 서버 오류 | 500 | `{"code":"C000","message":"서버 내부 오류가 발생했습니다"}` + 운영 알림 |

모든 오류 응답에는 `traceId` 가 함께 담기고, 응답 헤더 `X-Request-Id` 로도 나갑니다.
사용자가 알려준 ID 하나로 로그를 바로 찾을 수 있습니다.

**404·403 은 운영 알림을 보내지 않습니다.** 장애가 아니기 때문입니다.
초기에는 봇이 없는 URL 을 긁을 때마다 알림이 울렸고, 그러면 진짜 장애가 소음에 묻힙니다.

## 경로를 추가할 때

1. SecurityConfig 에 규칙을 넣습니다. **와일드카드보다 구체적인 경로를 먼저** 선언합니다.
2. 클래스 레벨 `@PreAuthorize` 가 있는 컨트롤러라면 메서드에도 붙입니다.
3. `AccessControlContractTest` 에 공개/보호 중 하나로 등록합니다.

3번을 빠뜨리면 다음에 누가 규칙 순서를 바꿨을 때 아무도 모릅니다.
