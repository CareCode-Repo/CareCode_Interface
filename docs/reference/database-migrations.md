# 데이터베이스 마이그레이션

운영은 `ddl-auto=validate` 입니다. **스키마는 Flyway 만 바꿉니다.**
엔티티를 고치고 마이그레이션을 안 쓰면 [스키마 정합성 테스트](../quality/regression-safety.md)가
기동 단계에서 깨뜨립니다.

## 전체 목록

| 버전 | 파일 | 무엇을 | 왜 |
|------|------|--------|-----|
| V1 | `baseline` | 기본 스키마 전체 | 초기 구축 |
| V2 | `feature_tables` | 예방접종 일정, 동의, 신고, 차단 | 아이 건강·커뮤니티 모더레이션 |
| V3 | `hospital_external_code` | 병원 외부 식별자(요양기호) | 심평원 데이터와 매칭할 자연키 |
| V4 | `search_indexes` | 위치·전문 검색 인덱스 | 반경 검색이 전 행에 삼각함수를 돌리는 풀스캔이었음 |
| V5 | `facility_capacity_snapshot` | 정원·현원 시계열 | 덮어쓰면 관측 이력이 사라져 예측 불가 |
| V6 | `benefit_eligibility` | 소득·다자녀·소급 조건 | 연령·지역만으로는 수급 가능 여부를 못 가림 |
| V7 | `benefit_payment_duration` | 지급 기간 상한 | **대상 연령을 지급 기간으로 착각해 총액이 폭증** |
| V8 | `user_events` | 행동 이벤트 원본 | 전환율·리텐션을 사후 계산하려면 원본이 필요 |
| V9 | `policy_verification` | 금액 검증 이력 | 틀린 금액을 확정치처럼 보이면 분쟁이 됨 |
| V10 | `hospital_grade` | 요양기관 종별 분리 | `type` 에 종별이 들어가 "소아과" 검색이 안 됐음 |
| V11 | `policy_change_log` | 정책 변경 이력 | 덮어쓰면 "무엇이 바뀌었는지" 가 사라져 알림 불가 |
| V12 | `benefit_amount_report` | 실수령액 제보 | 공공데이터가 금액을 숫자로 주지 않음 |
| V13 | `facility_waitlist` | 대기 기록 | **공공데이터에 존재하지 않는 데이터** — 사용자에게서만 얻음 |
| V14 | `policy_exclusion_group` | 중복 수급 배타 그룹 | 부모급여와 양육수당을 합산해 총액이 부풀려짐 |
| V15 | `missing_entity_tables` | 누락 테이블·컬럼 보충 | **엔티티는 있는데 DDL 에 없어 기동 실패** |
| V16 | `waitlist_vacancy_notice` | 빈자리 알림 발송 이력 | 같은 자리를 반복 알리면 신뢰를 잃음 |
| V17 | `policy_deadline_notice` | 마감 알림 발송 이력 | **Blue/Green 에서 인스턴스가 2대가 되면 중복 발송** |
| V18 | `notification_email_default` | 이메일 알림 DDL 기본값 | 엔티티는 `false` 인데 DDL 이 `TRUE` 라 JPA 를 안 거치면 켜짐 |

## 특히 기억할 것들

### V7 — 총액이 3.6배 부풀려졌던 원인

`targetAgeMin`/`targetAgeMax` 는 **"어떤 아이가 대상인가"** 이지 **"몇 개월 받는가"** 가 아닙니다.

이걸 지급 기간으로 쓰는 바람에 아빠육아휴직보너스 250만 원 × 60개월 = **1억 5천만 원**이
한 사람의 예상 수령액에 들어갔습니다.

`max_payment_months` 를 분리해 2억 9,506만 원 → 8,056만 원이 되었습니다.
자세한 내용은 [지원금 지능화](../features/benefit-intelligence.md#수령액-계산--두-번의-큰-오류)에.

### V13 — 공공데이터에 없는 데이터

대기 기록은 정부가 공개하지 않습니다. **사용자에게서만 얻을 수 있습니다.**

정원 관측은 "자리가 났는가" 만 알려주고, "실제로 얼마나 기다렸는지" 는 겪은 사람만 압니다.
이런 데이터가 쌓일수록 공공데이터만으로는 만들 수 없는 것을 할 수 있게 됩니다.

### V15 — 있는 줄 알았던 테이블

`TBL_POLICY_BOOKMARKS` 는 **컨트롤러·서비스·리포지토리가 다 있는데 테이블이 없었습니다.**
코드만 보면 완성된 기능이라 아무도 의심하지 않았고, 모든 테스트가 통과했습니다.

`TBL_POLICIES.VIEW_COUNT` 도 마찬가지로 코드는 쓰는데 컬럼이 없어 조회수가 저장된 적이 없습니다.

이 마이그레이션이 [회귀 방지](../quality/regression-safety.md)를 만들게 된 직접적인 계기입니다.

### V17 — 유니크 제약이 하는 일

```sql
CONSTRAINT UK_POLICY_DEADLINE_NOTICE UNIQUE (POLICY_ID, USER_ID, NOTIFIED_ON)
```

"남은 일수가 D-7 인 날에만 보낸다" 는 규칙은 **하루에 한 번 실행될 때만** 성립합니다.
Blue/Green 배포로 인스턴스가 잠깐 2대가 되면 모든 알림이 두 번 나갑니다.

존재 확인은 반복 실행을, 유니크 제약은 동시 실행을 막습니다.

### V18 — 엔티티만 고치면 절반만 고친 것

이메일 알림 기본값 버그를 엔티티에서 `false` 로 고쳤는데 DDL 은 `DEFAULT TRUE` 로 남아 있었습니다.

JPA 는 값을 항상 명시해서 쓰기 때문에 **앱을 거치는 생성은 정상**입니다.
그래서 테스트도 통과하고 실사용에서도 드러나지 않습니다.

문제는 **시드 스크립트나 수기 SQL** 처럼 JPA 를 거치지 않는 삽입입니다.
그 경로로는 여전히 요청한 적 없는 이메일 알림이 켜진 채 행이 만들어집니다.

기본값을 바꾸는 엔티티 변경은 **DDL 기본값도 함께 봐야 합니다.**

## 작성 규칙

### MariaDB 문법만 사용

MySQL 전용 문법은 실패합니다. 실제로 V4 의 `WITH PARSER ngram` 이 기동을 막았습니다.

```sql
-- 실패: Function 'ngram' is not defined
CREATE FULLTEXT INDEX ... WITH PARSER ngram;
```

### 테이블명은 대문자

Linux MariaDB 는 `lower_case_table_names=0` 이라 대소문자를 구분합니다.
`CareCodeNamingStrategy` 가 `@Table` 이름을 그대로 쓰도록 하므로 **엔티티와 정확히 일치**해야 합니다.

Windows 개발 환경에서는 대소문자를 구분하지 않아 이 문제가 드러나지 않습니다.
스키마 정합성 테스트가 Linux 컨테이너를 쓰는 이유입니다.

### 주석에 "왜" 를 남긴다

```sql
-- 지급 기간 상한. targetAgeMin/Max 는 "어떤 아이가 대상인가" 이지 "몇 개월 받는가" 가 아니다
```

무엇을 추가하는지는 SQL 이 말해 줍니다. 주석은 **왜 필요했는지**를 남깁니다.
6개월 뒤에 이 컬럼을 지워도 되는지 판단할 사람에게 필요한 건 그 정보입니다.

### 적용된 마이그레이션은 수정하지 않는다

Flyway 체크섬이 어긋나 기동이 실패합니다. 새 버전을 추가하세요.

## 검증

```bash
# 스키마 정합성 (Testcontainers MariaDB 필요)
./gradlew test --tests "*FlywaySchemaValidationTest*"
```

로컬에서 직접 확인하려면:

```bash
docker run -d --name cc-db -e MARIADB_ROOT_PASSWORD=pw -e MARIADB_DATABASE=carecode \
  -p 13306:3306 mariadb:10.11
docker run -d --name cc-redis -p 16379:6379 redis:7-alpine

java -jar build/libs/carecode-app.jar --spring.profiles.active=prod \
  --spring.datasource.url='jdbc:mariadb://localhost:13306/carecode' \
  --spring.datasource.username=root --spring.datasource.password=pw \
  --spring.data.redis.host=localhost --spring.data.redis.port=16379 \
  --spring.flyway.enabled=true
```

`Started CareCodeApplication` 이 나오면 마이그레이션과 엔티티가 일치하는 것입니다.
`validate` 는 불일치가 있으면 그 전에 죽습니다.
