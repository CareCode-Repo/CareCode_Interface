# 구조 규칙

> 관련 이슈: #132

## 왜

이 저장소가 지저분해진 방식은 "로직이 엉킴" 이 아니라 **같은 일을 여러 방식으로 하게 된 것** 입니다.

| 증상 | 수치 |
|------|------|
| 예외 체계 | `CareServiceException` 96건 · `BusinessException` 51건 · 도메인 전용 94건 |
| `catch (Exception e)` 로 원인 덮어쓰기 | ChatbotService 11회 · HealthService 10회 |
| `Map<String, Object>` 응답 | 컨트롤러 24곳 (OpenAPI 에 타입이 `object` 로만 나간다) |
| 소유권 검증 구현 | 3곳 (`ChildService` · `GrowthChartService` · `HealthService`) |
| 다른 도메인 리포지토리 직접 참조 | `UserRepository` 24곳 · `ChildRepository` 12곳 |
| 거대 서비스 | HealthService 977줄 · UserService 626 · CommunityService 616 |

한 번 정리해도 **다음 기능에서 다시 섞이면 의미가 없습니다.** 그래서 규칙을 먼저 테스트로 고정했습니다.

## 방식 — 지금 위반은 baseline 으로 묶는다

ArchUnit 의 `FreezingArchRule` 을 씁니다.

- 현재 위반 목록을 `archunit-baseline/` 에 저장하고 **함께 커밋** 합니다.
- **새 위반만** 빌드를 실패시킵니다. 전부 고칠 때까지 규칙 도입을 미루지 않아도 됩니다.
- 기존 위반을 고치면 목록에서 자동으로 빠집니다. **baseline 줄 수가 리팩터링 진척도** 입니다.

```bash
./gradlew test --tests '*ArchitectureRulesTest'
```

## 규칙과 현재 위반 (2026-10-05 기준)

| 규칙 | 위반 | 뜻 |
|------|------|-----|
| 다른 도메인의 리포지토리를 직접 쓰지 않는다 | 271 | 남의 테이블을 직접 읽으면 그 도메인의 규칙(소유권 검증 등)을 건너뛴다 |
| 도메인 패키지 이름은 소문자다 | 96 | `careFacility` 하나만 낙타등. 한 군데만 다르면 새로 온 사람이 둘 다 흉내 낸다 |
| 컨트롤러는 리포지토리를 직접 쓰지 않는다 | 66 | 검증·트랜잭션 경계가 컨트롤러로 샌다 |
| core 기반 패키지는 domain 을 의존하지 않는다 | 50 | 기반 코드가 도메인을 알면 공통이 아니다 |
| 매퍼는 리포지토리를 쓰지 않는다 | 3 | 변환 한 번에 숨은 쿼리가 생기면 N+1 을 추적할 수 없다 |
| core 에는 컨트롤러가 없다 | 2 | `core/controller/CareFacilityApiController` 373줄 |
| 도메인은 SecurityContextHolder 를 직접 쓰지 않는다 | 1 | principal 모양이 바뀌면 고칠 곳이 흩어진다(시설 API 500 의 원인) |
| 서비스·리포지토리는 컨트롤러를 의존하지 않는다 | **0** | 층 방향은 이미 지켜지고 있다 |
| 엔티티는 DTO·컨트롤러를 의존하지 않는다 | **0** | 매핑 경계는 지켜지고 있다 |

마지막 두 줄이 0 인 것이 중요합니다 — **층 구조 자체는 맞습니다.** 문제는 같은 층 안에서 경계를 지키지 않는 쪽입니다.

## 규칙 대상이 아닌 것

`core/scheduler` · `core/client` · `core/devtools` · `core/geocoding` · `core/benefit` 은
공공데이터 동기화와 배치처럼 **여러 도메인을 조합하는 코드** 입니다. 도메인을 아는 게 본질이라
"기반은 도메인을 모른다" 규칙의 대상이 아닙니다.

다만 그 코드가 "공통 기반" 자리에 있는 것이 혼란의 원인입니다. 조합 계층으로 따로 옮기는 것은
다음 단계에서 다룹니다.

## 다음 단계

| 단계 | 내용 | 효과 |
|------|------|------|
| 1. 자리 정리 | `core` 의 도메인 컨트롤러 이동, 조합 계층 분리, `careFacility`→`facility`, 문서의 `facade`↔코드 `app` 통일 | 위반 96+50+2 감소 |
| 2. 예외 한 체계 | `catch(Exception)` 덮어쓰기 제거, ErrorCode 기반 통합 | 상태 코드 정확해짐 |
| 3. `Map` 응답 → DTO | 24곳. 스펙에 타입이 생겨 프런트 계약 대조가 이 경로까지 본다 | 계약 품질 |
| 4. Child 를 제 자리로 | 엔티티는 `user`, 서비스는 `health` 에 쪼개져 있다 | 결합 1위 해소 |
| 5. 거대 서비스 분해 | HealthService 977줄 | 2·3 과 함께 진행 |
