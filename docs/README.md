# CareCode(맘편한) 문서

육아 지원금·보육시설·건강관리를 한 곳에서 다루는 백엔드 서비스입니다.
이 디렉터리는 **무엇을 만들었는지**가 아니라 **왜 그렇게 만들었는지**를 남기는 것을 목표로 합니다.

## 이 문서들의 전제

이 서비스의 핵심 데이터는 전부 **정부 공공데이터**에서 옵니다. 그래서 대부분의 설계 판단은
"우리가 무엇을 하고 싶은가" 보다 **"공공데이터가 무엇을 주지 않는가"** 에서 출발합니다.

예를 들어 어린이집 정원 데이터는 시설 전체 수치만 주고 반별로는 주지 않습니다.
그래서 빈자리 알림은 "어느 반에 자리가 났는지는 알 수 없다" 는 한계를 문구에 그대로 밝힙니다.
정확한 척하는 것이 틀린 정보보다 위험하기 때문입니다.

이런 판단의 근거를 각 문서에 함께 적었습니다.

## 문서 지도

### 아키텍처

| 문서 | 내용 |
|------|------|
| [시스템 개요](architecture/system-overview.md) | 전체 구성, 계층 구조, 요청·배치 흐름 (Mermaid) |
| [데이터 흐름](architecture/data-flow.md) | 공공데이터 수집 → 정제 → 알림까지의 파이프라인 (Mermaid) |
| [carecode-architecture.drawio](architecture/carecode-architecture.drawio) | draw.io 편집용 아키텍처 원본 |

### 기능

| 문서 | 다루는 범위 | 관련 이슈 |
|------|-------------|-----------|
| [공공데이터 연동](features/public-data-integration.md) | 4개 정부 API 연동, 공급자 추상화, 전국 순회 동기화 | #61 #68 |
| [지원금 지능화](features/benefit-intelligence.md) | 추천·지역 비교·놓친 지원금·실수령액 제보·중복 수급 배타 | #65 #67 #69 |
| [시설 지능화](features/facility-intelligence.md) | 정원 시계열·입소 예측·인기도·대기 기록·빈자리 알림 | #65 #67 #69 #73 |
| [알림과 리텐션](features/notification-and-retention.md) | 정책 변경·빈자리·마감 임박 알림, 딥링크, 클릭 전환 | #69 #73 #74 |
| [지표 수집](features/analytics.md) | 행동 이벤트, 퍼널, 코호트 리텐션 | #68 |
| [개인정보와 법적 문서](features/privacy-and-legal.md) | 동의 분리, 민감정보 차단, 처리방침·약관 | #68 #71 |
| [운영](features/operations.md) | 운영 알림, 헬스체크, 스케줄러, 수동 실행 | #68 #70 |

### 품질

| 문서 | 내용 | 관련 이슈 |
|------|------|-----------|
| [기동 안정화](quality/runtime-hardening.md) | 실기동에서 드러난 차단 8건과 접근제어 결함 | #70 |
| [회귀 방지](quality/regression-safety.md) | 왜 CI 가 못 잡았는지, 어떻게 막았는지 | #72 |

### 레퍼런스

| 문서 | 내용 |
|------|------|
| [데이터베이스 마이그레이션](reference/database-migrations.md) | V1~V17 각각이 왜 필요했는지 |
| [접근제어 매트릭스](reference/access-control-matrix.md) | 공개·인증·관리자 경로 전수 |

### 기존 문서

| 문서 | 내용 |
|------|------|
| [ERD.md](ERD.md) | 엔티티 관계도 |
| [ISSUE_MANAGEMENT.md](ISSUE_MANAGEMENT.md) | 이슈·커밋 연결 규칙 |
| [system-architecture.md](system-architecture.md) | 초기 아키텍처 문서 |
| [ARCHITECTURE_IMPROVEMENTS.md](ARCHITECTURE_IMPROVEMENTS.md) | 초기 개선 기록 |

## 기술 스택

| 구분 | 사용 기술 |
|------|-----------|
| 런타임 | Java 17, Spring Boot 3.3.3 |
| 데이터 | MariaDB 10.11, Redis 7, Flyway |
| 빌드 | Gradle 8.14, JaCoCo |
| 테스트 | JUnit 5, Mockito, AssertJ, Testcontainers, H2 |
| 문서 | springdoc-openapi (운영에서는 비공개) |
| 배포 | Docker, GitHub Actions, Blue/Green |

## 개발 규칙

- **커밋**: `TYPE : 한글 설명 (#이슈번호)` — 관심사별로 잘게 나눕니다.
- **스키마**: 운영은 `ddl-auto=validate` 입니다. 엔티티를 바꾸면 마이그레이션도 반드시 씁니다.
  안 쓰면 [스키마 정합성 테스트](quality/regression-safety.md)가 기동 단계에서 깨뜨립니다.
- **접근제어**: 경로를 추가하면 [접근제어 계약 테스트](reference/access-control-matrix.md)에도 넣습니다.
  SecurityConfig 는 앞선 규칙이 뒤를 덮어서, 규칙만 보고는 실제로 열렸는지 알 수 없습니다.
- **비밀값**: API 키·자격증명은 저장소에 넣지 않습니다. 환경변수로만 주입합니다.
