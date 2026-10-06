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

## 규칙과 현재 위반

진척: **489건(0단계) → 391건(1b) → 383건(3a) → 364건(4a) → 362건(5a)**. 남은 것은 도메인 경계(247)와
컨트롤러의 리포지토리 직접 사용(61), core 기반의 도메인 참조(50)입니다.

> 세는 법: `archunit-baseline/` 에는 규칙별 위반 파일 외에 색인 파일 `stored.rules` 도 있습니다.
> `cat archunit-baseline/* | wc -l` 은 색인 11줄을 함께 세므로 실제 위반보다 11 크게 나옵니다.

| 규칙 | 위반 | 뜻 |
|------|------|-----|
| 다른 도메인의 리포지토리를 직접 쓰지 않는다 | 247 | 남의 테이블을 직접 읽으면 그 도메인의 규칙(소유권 검증 등)을 건너뛴다 |
| 도메인 패키지 이름은 소문자다 | **0** | `careFacility`→`facility` 로 해소 (1b 단계) |
| 컨트롤러는 리포지토리를 직접 쓰지 않는다 | 61 | 검증·트랜잭션 경계가 컨트롤러로 샌다 |
| core 기반 패키지는 domain 을 의존하지 않는다 | 50 | 기반 코드가 도메인을 알면 공통이 아니다 |
| 매퍼는 리포지토리를 쓰지 않는다 | 3 | 변환 한 번에 숨은 쿼리가 생기면 N+1 을 추적할 수 없다 |
| core 에는 컨트롤러가 없다 | **0** | 1a 에서 도메인으로 옮기고, 3a 에서 중복이라 삭제 |
| 도메인은 SecurityContextHolder 를 직접 쓰지 않는다 | 1 | principal 모양이 바뀌면 고칠 곳이 흩어진다(시설 API 500 의 원인) |
| 서비스·리포지토리는 컨트롤러를 의존하지 않는다 | **0** | 층 방향은 이미 지켜지고 있다 |
| 엔티티는 DTO·컨트롤러를 의존하지 않는다 | **0** | 매핑 경계는 지켜지고 있다 |

마지막 두 줄이 0 인 것이 중요합니다 — **층 구조 자체는 맞습니다.** 문제는 같은 층 안에서 경계를 지키지 않는 쪽입니다.

## core 를 이름이 뜻을 말하는 자리로 쪼갰다 (1단계)

`core` 라는 한 패키지에 성격이 다른 셋이 섞여 있었습니다.

| 성격 | 내용 | 도메인을 아는가 |
|------|------|-----------------|
| 기반 | 보안·웹·설정·저장소·유틸·예외·모니터링 | 몰라야 한다 |
| 조합 | 공공데이터 연동·배치·지오코딩 | **아는 게 본질이다** (도메인 참조 47건) |
| 도메인 | 시설 공개 API 컨트롤러 2개, 지원금 계산기 | 도메인이다 |

조합 코드가 "공통 기반" 자리에 있으면, 규칙을 세울 수도 없고(도메인 참조가 정상이므로)
새로 온 사람은 "core 는 뭐든 넣는 곳" 으로 읽습니다. 그래서 이름이 뜻을 말하도록 옮겼습니다.

| 전 | 후 | 무엇인가 |
|----|----|----------|
| `core/client` | `external/publicdata` | 외부 공공데이터 연동·동기화 |
| `core/geocoding` | `external/geocoding` | 외부 좌표 변환 |
| `core/scheduler` | `batch` | 주기 작업 |
| `core/devtools` | `devtools` | 개발용 샘플 데이터 |
| `core/benefit` | `domain/policy/benefit` | policy 만 쓰는 도메인 로직이었다 |
| `core/controller/CareFacilityApiController` | `domain/careFacility/controller` | 373줄짜리 시설 공개 API |
| `core/client/controller/PublicDataController` | `domain/careFacility/controller` | 같은 성격 |
| `core/controller/BaseController` | `core/web` | 패키지 해체 |

`integration` 이라는 이름은 쓰지 않았습니다. 테스트 쪽에서 이미 "통합 테스트" 뜻으로 쓰고 있어
같은 이름이 두 뜻으로 읽힙니다.

### 총 위반은 줄지 않았다 (489 → 489)

솔직하게 적어 둡니다. "core 에 컨트롤러" 2건이 **`careFacility` 네이밍 위반 2건으로 옮겨 간 것**입니다.
옮긴 코드가 이제 도메인 규칙의 대상이 되기 때문입니다. 숫자가 줄어드는 것은 다음 단계
(`careFacility`→`facility`, 98건 일괄)에서 나옵니다.

이 단계의 값은 숫자가 아니라 **규칙을 세울 수 있는 구조가 된 것** 입니다. 조합 코드가 기반에서 빠졌으니
"기반은 도메인을 모른다" 를 예외 없이 적용할 수 있습니다.

### 파일을 옮기면 baseline 을 다시 떠야 한다

위반 설명에 클래스 경로가 들어가므로, 같은 문제라도 파일을 옮기면 **새 위반으로 잡힙니다.**
이동 PR 에서는 `freeze.refreeze=true` 로 한 번 다시 뜨고, 규칙별 숫자가 늘지 않았는지 확인한 뒤
다시 `false` 로 돌려놓습니다(그래야 CI 가 진짜 새 위반을 잡습니다).

## 같은 일을 하는 입구를 지웠다 (3a 단계)

`Map<String, Object>` 응답 24곳을 타입으로 바꾸려고 하나씩 읽다가, 그중 7곳은 **타입을 붙일 값이
아니라는 것**을 알았습니다. 이미 같은 일을 하는 다른 엔드포인트가 있었습니다.

| 지운 것 | 같은 일을 하는, 남겨 둔 것 | 왜 지웠나 |
|---------|---------------------------|-----------|
| `POST /api/public/care-facilities/sync-all` | `POST /api/admin/public-data/facilities/sync` | 컨트롤러가 리포지토리에 직접 쓰고 페이지 루프를 돌렸다. 남긴 쪽은 `SyncResult` 와 신선도 이력을 남긴다 |
| `GET /api/public/care-facilities/swagger/sync` | 같음 | 위를 GET 으로 한 번 더 노출한 것. **GET 이라 브라우저 접속만으로 DB 쓰기가 실행됐다** |
| `GET /api/public/care-facilities/swagger/db-facilities` | `GET /facilities` | 응답을 `success`/`message` 로 한 겹 더 싸기만 했다 |
| `GET /api/public/care-facilities/swagger/stats` | `GET /facilities/statistics` | 같음 |
| `GET /notifications/settings/{userId}` | `GET /notifications/preferences` | 고정값을 돌려줬다 (이메일 켬, 조용한 시간 22:00–08:00) |
| `PUT /notifications/settings/{userId}` | `PUT /notifications/preferences/...` | **받은 값을 그대로 돌려주고 저장하지 않았다** |
| `GET /notifications/statistics/{userId}` | `GET /notifications/stats` | 우선순위 분포 `HIGH=5, NORMAL=15, LOW=5`, 일별 건수 `2024-01-15: 3` 이 코드에 박혀 있었다 |

마지막 세 개가 이 단계의 핵심입니다. 응답이 그럴듯하면 프런트는 **설정이 저장된다고 읽습니다.**
타입을 붙이면 그 거짓이 스펙에 정식으로 올라가므로, 타입화가 아니라 삭제가 맞습니다.

입구가 둘이면 보안 설정도 둘입니다. 실제로 `/api/public/care-facilities/**` 가 통째로 `permitAll`
이어서, 그 아래 동기화 트리거까지 열려 있었고 경로별로 `hasRole("ADMIN")` 을 덧붙여 막아 둔
상태였습니다. 중복을 지우면서 그 예외 설정도 함께 사라졌습니다.

되살아나는 것을 막으려고 `AccessControlContractTest.duplicateMappingsRemoved` 가 일곱 경로를
모두 두드려 404/405 를 확인합니다(복사·붙여넣기로 돌아오기 쉬운 종류입니다).
OpenAPI 스펙은 237 → 231 경로가 됐습니다.

## `Map` 응답을 타입으로 바꿨다 (3b 단계)

컨트롤러가 `Map<String, Object>` 를 돌려주면 OpenAPI 에는 `type: object` 만 남습니다. 키도 타입도
스펙에 없으니, 공들여 만든 프런트 계약 대조(`openapi-contract.test.ts`)가 그 경로에서는 **경로가
있는지만** 확인하고 응답은 아무것도 확인하지 못합니다.

3a 에서 7곳을 지우고 남은 17곳을 레코드로 바꿨습니다.

| 어디 | DTO |
|------|-----|
| 대기 등록·내 대기 목록 | `WaitlistRegisterResponse`, `WaitlistEntryResponse` |
| 좋아요·북마크 토글·개수 | `PostLikeToggleResponse`, `PostBookmarkToggleResponse`, `PostLikeCountResponse`, `PostBookmarkCountResponse` |
| 카카오 로그인 URL | `KakaoLoginUrlResponse` |
| 연계 추천 | `HealthRecommendationResponse` |
| 공공데이터 동기화 4종·좌표 보정·빈자리/마감 알림 | `SyncResultResponse`, `GeocodingResultResponse`, `VacancyNotifyResponse`, `DeadlineNotifyResponse` |
| 주기 작업 상태·예측 정확도 측정 | `SyncStatusResponse`, `ForecastAccuracyMeasureResponse` |
| 정책 검증·지역별 검증 현황 | `PolicyVerificationResponse`, `RegionVerificationStatusResponse` |
| 어드민 대시보드·예약 대시보드 | `AdminDashboardResponse`, `AdminBookingDashboardResponse` |
| 내 데이터 내려받기 | `MyDataExportResponse` |

### 바꾸면서 깨질 수 있는 지점은 키 이름이다

`Map.put("isLiked", ...)` 는 키를 그대로 쓰지만, 같은 값을 Lombok `@Getter` 클래스의
`boolean isLiked` 로 담으면 Jackson 이 `is` 를 떼어 **키가 `liked` 로 나갑니다.** 프런트는
`isLiked` 를 `z.boolean()` 으로 필수로 읽으므로 그 순간 토글이 파싱 단계에서 깨집니다.
이 프로젝트는 같은 사고를 `isRead`·`isAnonymous`·`zScore` 로 이미 겪었습니다.

레코드는 컴포넌트 이름을 그대로 키로 쓰므로 지금은 문제가 없습니다(실제로 확인했습니다 —
`{"isLiked":true,"likeCount":7}`). 다만 나중에 클래스로 바꾸면 되살아나므로 `@JsonProperty` 를
명시해 두었고, `JsonFieldNameContractTest` 가 **바꾼 응답 전부의 키 집합**을 고정합니다.
키가 빠지는 것뿐 아니라 **늘어나는 것도** 계약 변경이므로 `containsExactlyInAnyOrder` 로 봅니다.

### 한 군데는 값도 고쳤다

내 데이터 내려받기의 자녀 생일·성별이 `String.valueOf()` 를 거쳐, 값이 없으면 `"null"` 이라는
**문자열**로 내려갔습니다. 열람권 행사로 받은 파일에 `"null"` 이 적혀 있는 셈입니다.
이제 없는 값은 `null` 로 보냅니다(프런트는 받은 JSON 을 그대로 파일로 저장합니다).

### 숫자

- OpenAPI 스키마: 134 → **158** 개 (경로 수는 그대로 231)
- 프런트가 `z.record(z.unknown())` 으로 받던 내려받기 응답에 처음으로 모양이 생겼다
- 전체 테스트 **616개, 실패 0**

## 자녀 조회에 입구를 만들었다 (4a 단계)

도메인 경계 위반 268건 중 자녀·사용자 리포지토리 직접 참조가 가장 큰 덩어리였습니다. 자녀는
`user` 도메인에 속하는데, 쓰는 쪽은 health·policy·facility 입니다. 지금까지는 그 도메인들이
`ChildRepository` 를 그냥 주입해 썼고, 그 결과 두 가지가 복제됐습니다.

### 1. 소유권 검증이 세 벌이었고, 답이 셋으로 갈렸다

"이 아이가 내 아이인가" 를 세 군데가 각자 구현하고 있었습니다. 그런데 **같은 질문에 대한 답이
경로마다 달랐습니다.**

| 경로 | 남의 아이를 요청하면 | 문제 |
|------|---------------------|------|
| `/children/…` | 404 | 맞는 동작 |
| `HealthService.assertChildOwnedByUser` | 403 | **그 자녀가 존재한다는 사실을 알려준다.** ID 를 훑으면 "이 번호는 누군가의 자녀" 목록을 만들 수 있다 |
| `/health/vaccines/schedule`, `/health/checkups/schedule` | **500** | 위 403 이 `catch (Exception e)` 에 걸려 `CareServiceException` 으로 덮였다. 권한 거부가 서버 장애로 집계되고, 클라이언트는 자기가 잘못 보냈다는 걸 알 수 없다 |

`ChildOwnershipContractTest` 가 여섯 경로를 전부 두드려 **404** 를 확인합니다. 테스트를 먼저
쓰니 500 두 건이 바로 드러났습니다(`expected: 404 but was: 500`).

함께 고친 것: `HealthService` 의 `catch (Exception e)` 10곳 중 8곳에 우리 예외를 그대로
통과시키는 `catch (CareCodeException e) { throw e; }` 를 넣었습니다. 없으면 404·403 이 전부
500 으로 덮입니다. (Slack 알림까지는 가지 않았습니다 — 알림은 최후 핸들러에서만 울리고
`CareServiceException` 은 자기 핸들러가 있습니다. 테스트로 확인했습니다.)

### 2. 월령 계산이 다섯 벌이었다

정책 추천·지원금 비교·마감 알림·제보 요청·대기 등록이 모두
`ChronoUnit.MONTHS.between(child.getBirthDate(), today)` 를 각자 들고 있었고, **생일이 없을 때의
처리가 곳마다 달랐습니다**(제외 / 0 / 통과). 월령은 추천 결과를 좌우하는 입력이라, 복제된 계산이
하나만 어긋나면 같은 아이에게 다른 정책이 나옵니다.

`ChildView.ageMonths(기준일)` 한 곳으로 모았습니다. 생일이 없으면 `null` 이고, **`null` 을 0 으로
바꾸지 않습니다** — 0개월로 바꾸면 "갓 태어난 아이" 로 읽혀 신생아 정책이 추천됩니다.

판단은 여전히 호출하는 쪽이 합니다. 마감 알림만 "생일을 모르면 배제하지 않는다" 로 다르게
처리하는데, 놓친 손해가 성가심보다 크다는 판단이라 의도된 차이입니다. 계산은 하나, 판단은 각자.

### 입구의 모양

```java
public interface ChildDirectory {
    List<ChildView> childrenOf(Long userId);
    ChildView requireOwnedChild(Long childId, Long parentUserId);  // 없는 자녀와 남의 자녀를 구분하지 않는다
}
```

엔티티가 아니라 `ChildView`(읽기 전용 값)를 돌려줍니다. 쓰는 쪽이 자녀 정보를 고치거나
`child.getUser()` 로 사용자 정보까지 타고 들어가는 일이 없습니다.

### 곁에서 드러난 것

- `FacilityWaitlist.child` 가 `@ManyToOne Child` 였는데 **읽는 곳이 없었습니다.** CHILD_ID 를 채우는
  용도뿐이라 바로 위 `facilityId` 와 같은 방식(`Long childId`)으로 맞췄습니다. DB 외래키와 중복 등록
  제약은 그대로입니다.
- 자녀 미등록·생일 없음·남의 대기 기록 수정이 `CareServiceException`(500)이었습니다. 각각
  400·400·403 으로 고쳤습니다. 3단계에서 지운 7곳과 같은 뿌리입니다.

### 숫자

- ArchUnit baseline 383 → **364** (도메인 경계 268 → 249)
- `ChildRepository` 를 쓰는 도메인: policy 5곳·facility 1곳 → **0**. health 5곳은 엔티티가 필요해
  남아 있습니다(4b)
- 전체 테스트 **624개, 실패 0**. OpenAPI 스펙은 변화 없음 — 응답 모양을 바꾸지 않았습니다

## 분해 전에 지울 것이 먼저였다 (5a 단계)

거대 서비스를 쪼개려고 `HealthService` 1011줄을 책임별로 나누기 시작했는데, 나누다 보니
**상당 부분이 아무도 부르지 않는 코드**였습니다. 부르는 곳이 없는 코드를 옮기는 건 옮긴 만큼만
일이 늘어납니다.

컨트롤러에서 거꾸로 따라가 도달 가능한지 확인했습니다.

| 지운 것 | 왜 |
|---------|-----|
| `analyzeHealthStatus`, `generateHealthReport` | 파사드에는 있는데 컨트롤러가 부르지 않는다. `healthScore`·`riskLevel` 같은 값을 만들어 내는데 공개된 적이 없다 |
| `getHealthGoals` | 고정 문자열("모든 예방접종 완료")을 돌려준다. 도달 불가 |
| `getHealthChart` | 도달 불가. 성장 곡선은 `GrowthChartService` 가 따로 한다 |
| `checkSystemHealth` | `{status: UP, version: 1.0.0}` 을 하드코딩. **Actuator 의 `/actuator/health` 와 중복**이고 도달 불가 |
| `getHealthRecords`, `getHealthRecordsByDateRange` | 파사드에도 없다 |
| 위 메서드들만 쓰던 private 헬퍼 11개 | `calculateHealthScore`, `determineRiskLevel`, `generateRecommendations`, `calculateVaccineStatus`, `calculateCheckupStatus`, `calculateProgress`, `generateHealthSummary`, `extractChartValue`, `validateChartType`, `validateMonths`, `validatePaginationParams` |
| 상수 3개 | `HEALTH_SCORE_HIGH_THRESHOLD` 등. 점수 계산이 사라지며 함께 쓸 곳이 없어졌다 |

`CareFacilityService` 에서도 같은 것이 나왔습니다. `saveCareFacilitiesFromPublicData`,
`countCareFacilities` 와 그 헬퍼 5개는 **3a 에서 지운 `CareFacilityApiController` 가 쓰던 잔재**였습니다.
입구를 지우면 그 뒤에 남는 코드가 생기는데, 그때 같이 지우지 않으면 "쓰이는 코드" 처럼 보입니다.

### 숫자

| 파일 | 전 | 후 |
|------|-----|-----|
| `HealthService` | 1011줄 | **715줄** |
| `CareFacilityService` | 580줄 | **411줄** |
| `HealthFacade` | 338줄 | **309줄** |

- ArchUnit baseline 364 → **362**
- 전체 테스트 **632개, 실패 0**
- **OpenAPI 스펙 변화 없음** — 공개 API 를 하나도 건드리지 않았습니다. 지운 것은 전부 밖에서
  부를 수 없던 코드입니다

### 남은 분해 (5b)

`HealthService` 715줄은 아직 다섯 가지를 함께 들고 있습니다 — 건강 기록 CRUD, 아이 조회
(연령·성별·특별 요구·이름 검색), 접종·검진 일정, 알림, 통계·추천. 이 중 **아이 조회 4개는
`user` 도메인 일**이라 4a 의 입구로 옮기는 것이 다음 자리입니다.

## 다음 단계

| 단계 | 내용 | 효과 |
|------|------|------|
| ~~1a. core 정리~~ | ~~조합 계층 분리, core 의 도메인 코드 이동~~ | **완료** — core 에 컨트롤러 0 |
| ~~1b. 이름 통일~~ | ~~`careFacility`→`facility`, 문서의 `facade`↔코드 `app`~~ | **완료** — 489 → 391 |
| ~~2. 예외 한 체계~~ | ~~"찾을 수 없습니다" 34곳을 `ResourceNotFoundException` 으로~~ | **완료** — 500 → 404 |
| ~~3a. 중복 입구 제거~~ | ~~같은 일을 하는 엔드포인트 7개 삭제~~ | **완료** — 391 → 383 |
| ~~3b. `Map` 응답 → DTO~~ | ~~남은 17곳~~ | **완료** — 스펙 스키마 134 → 158 |
| ~~4a. 자녀 조회 입구~~ | ~~policy·facility 가 `ChildRepository` 를 직접 쓰고 소유권 검증이 3중~~ | **완료** — 383 → 364, 404 로 통일 |
| 4b. Child 를 제 자리로 | `ChildService` 가 `health` 에 있다. health 는 아직 엔티티가 필요해 `ChildRepository` 를 쓴다 | 결합 추가 해소 |
| ~~5a. 도달 불가 코드 제거~~ | ~~HealthService 1011줄, CareFacilityService 580줄~~ | **완료** — 715줄·411줄 |
| 5b. 남은 책임 분리 | HealthService 는 아직 기록·아이 조회·일정·알림·통계를 함께 들고 있다 | 읽기 쉬움 |
