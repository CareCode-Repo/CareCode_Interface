# CareCode Organization README

CareCode 저장소 운영을 위한 협업 기준 문서입니다.  
제품 소개와 실행 방법은 `README.md`를, 팀 운영 규칙은 이 문서를 기준으로 확인합니다.

## 1) Repository Overview

- **Repository**: `CareCode_Interface`
- **Primary Stack**: Java 17, Spring Boot 3.3.3, Gradle, MariaDB, Redis
- **Architecture**: 도메인 중심(DDD) + 계층형 구조
- **Main Goal**: 부모를 위한 통합 육아 지원 플랫폼 백엔드 개발 및 운영

## 2) Team Working Agreement

- 기능 개발은 작은 단위로 나누어 빠르게 PR을 올립니다.
- PR 리뷰 이전에 기본 빌드/테스트 통과를 확인합니다.
- 공개 가능한 정보만 커밋하며, 민감 정보는 절대 저장소에 포함하지 않습니다.
- 장애/버그 이슈는 재현 방법을 우선 기록합니다.

## 3) Branch Strategy

- `main`: 배포 가능한 안정 브랜치
- `develop`: 통합 개발 브랜치
- `feature/*`: 기능 개발
- `fix/*`: 버그 수정
- `hotfix/*`: 운영 긴급 수정
- `docs/*`: 문서 전용 변경

브랜치 이름 예시:

- `feature/policy-search-pagination`
- `fix/care-facility-booking-validation`
- `docs/update-api-onboarding`

## 4) Commit Convention

커밋 메시지는 Conventional Commits 스타일을 권장합니다.

- `feat`: 새로운 기능
- `fix`: 버그 수정
- `refactor`: 리팩터링(동작 변경 없음)
- `docs`: 문서 수정
- `test`: 테스트 추가/수정
- `chore`: 빌드, 설정, 의존성 등 기타 변경

예시:

- `feat: add policy category filter endpoint`
- `fix: prevent duplicate booking creation`
- `docs: update organization contribution guide`

## 5) Pull Request Rules

- PR 제목은 변경 목적이 드러나게 작성합니다.
- 본문에는 최소한 아래 항목을 포함합니다.
  - 배경/목적
  - 변경 내용 요약
  - 테스트 방법 및 결과
  - 리뷰 시 확인 포인트
- 가능한 경우 API 변경은 Swagger 또는 문서 업데이트를 함께 반영합니다.
- PR 크기는 리뷰 가능한 범위(권장 300~500 lines 내외)로 유지합니다.

## 6) Review Checklist

리뷰어/작성자는 아래 항목을 기준으로 확인합니다.

- 비즈니스 로직이 요구사항과 일치하는가
- 예외 처리와 에러 응답이 일관적인가
- 보안/권한 검증 누락이 없는가
- 성능 이슈(N+1, 불필요한 반복 조회 등)가 없는가
- 테스트 코드 또는 검증 절차가 충분한가
- 문서/주석이 필요한 변경에 반영되었는가

## 7) Issue Management

- 버그: 재현 절차, 기대 결과, 실제 결과, 로그를 포함합니다.
- 기능 제안: 문제 정의, 기대 사용자 가치, 완료 조건을 포함합니다.
- 우선순위 라벨 예시: `P0`, `P1`, `P2`
- 영역 라벨 예시: `domain:policy`, `domain:careFacility`, `infra`, `docs`

## 8) CI/CD and Quality Gate

- 기본 품질 게이트(권장):
  - `./gradlew clean test`
  - 정적 분석 또는 포맷 검사(도입 시)
  - API 문서 생성 스크립트 검증(변경 시)
- 배포 전 체크:
  - 환경 변수 누락 여부 확인
  - DB 스키마 영향도 확인
  - 롤백 방법 준비

## 9) Security Policy (Basic)

- `.env`, 키 파일, 인증 토큰은 커밋 금지
- 외부 API Key는 환경 변수 또는 Secret Manager로만 관리
- 운영 데이터가 포함된 로그/스크린샷 공유 금지
- 의존성 업데이트 시 보안 이슈(CVE) 확인

## 10) Documentation Policy

- 아키텍처/운영 정책 변경 시 이 문서 또는 관련 문서를 함께 업데이트합니다.
- API 변경 시 `README.md`의 API 관련 섹션 또는 별도 문서를 동기화합니다.
- 신규 팀원 온보딩에 필요한 내용은 문서화하고, 구두 전파에 의존하지 않습니다.

## 11) Onboarding Quick Start

1. 저장소 클론 후 `README.md`의 실행 가이드를 따라 로컬 환경을 구성합니다.
2. `develop`에서 작업 브랜치를 생성합니다.
3. 작은 단위로 커밋하고 PR을 생성합니다.
4. 리뷰 반영 후 머지합니다.

---

문서 오너: CareCode Team  
최종 업데이트: 2026-04-20
