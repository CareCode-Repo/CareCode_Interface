# 데이터 흐름

이 서비스의 가치는 **정부가 흩어놓은 데이터를 한 사람 기준으로 다시 조립하는 것**에서 나옵니다.
그 조립 과정을 단계별로 정리합니다.

## 전체 파이프라인

```mermaid
flowchart LR
    subgraph collect["1. 수집"]
        direction TB
        P1["보육통합정보<br/>XML · HTTPS"]
        P2["유치원알리미<br/>JSON"]
        P3["보조금24<br/>JSON"]
        P4["심평원<br/>XML"]
    end

    subgraph normalize["2. 정제"]
        direction TB
        N1["시군구 순회<br/>RegionCodeCatalog"]
        N2["필드 매핑<br/>Upsert 서비스"]
        N3["좌표 보정<br/>주소 → 위경도"]
        N4["지급유형 판별<br/>월정액·일시금·융자"]
    end

    subgraph enrich["3. 축적"]
        direction TB
        E1["정원 스냅샷<br/>일자별 관측"]
        E2["정책 변경 이력<br/>금액·기한·연령"]
        E3["실수령액 제보<br/>사용자 입력"]
        E4["대기 기록<br/>사용자 입력"]
    end

    subgraph derive["4. 판단"]
        direction TB
        D1["입소 예측"]
        D2["시설 인기도"]
        D3["놓친 지원금"]
        D4["지역별 비교"]
        D5["빈자리 감지"]
        D6["마감 임박 감지"]
    end

    subgraph act["5. 전달"]
        direction TB
        A1["알림 발송"]
        A2["조회 API"]
        A3["행동 이벤트"]
    end

    P1 & P2 --> N1 --> N2 --> N3
    P3 --> N4
    P4 --> N2

    N2 --> E1
    N4 --> E2
    N2 --> E1

    E1 --> D1 & D2 & D5
    E2 --> D6
    E3 --> D3 & D4
    E4 --> D5

    D1 & D2 & D3 & D4 --> A2
    D5 & D6 --> A1
    A1 & A2 --> A3
```

**3단계(축적)가 이 구조의 핵심**입니다. 정부 API 는 "지금 이 순간" 만 알려주고 과거를 주지 않습니다.
매일 관측해서 쌓아야만 "자리가 늘었다", "금액이 바뀌었다" 를 말할 수 있습니다.
그래서 스냅샷과 변경 이력은 기능이 아니라 **다른 모든 판단의 재료**입니다.

## 공공데이터 수집 상세

```mermaid
sequenceDiagram
    participant S as Scheduler
    participant Sync as SyncService
    participant T as PagedSyncTemplate
    participant Cat as RegionCodeCatalog
    participant Prov as Provider
    participant Gov as 정부 API
    participant Up as UpsertService
    participant Snap as CapacitySnapshotRecorder
    participant DB as MariaDB
    participant Ops as OperationalAlerter

    S->>Sync: sync()
    Sync->>Cat: 시군구 코드 목록 (어린이집 202 / 유치원 212)
    loop 시군구마다
        Sync->>T: 페이지 순회
        T->>Prov: 요청
        Prov->>Gov: HTTP
        alt 정상
            Gov-->>Prov: 목록
            Prov-->>T: 파싱 결과
            T->>Up: upsert (코드 기준)
            Up->>DB: 저장
            Up->>Snap: 정원·현원 관측 기록
            Snap->>DB: 스냅샷 (일자별 1행)
        else 응답 코드가 한도 초과·키 만료
            Gov-->>Prov: 오류 코드
            Prov-->>Sync: ChildcareApiStatus
            Sync->>Ops: 운영 알림
        else 통신 실패
            Sync->>Sync: 해당 시군구만 실패 기록
        end
    end
    Sync-->>S: SyncResult (생성·갱신·실패)
    alt 실패 있음
        S->>Ops: 운영 알림
    end
```

한 시군구가 실패해도 **나머지는 계속 돕니다.** 전국 데이터는 일부가 비어도 쓸 수 있지만,
하나 때문에 전체가 멈추면 아무것도 못 씁니다.

## 빈자리 알림 판단

```mermaid
flowchart TD
    START([스케줄러 09:30]) --> W{대기자가 있는<br/>시설이 있는가}
    W -->|없음| END1([종료])
    W -->|있음| H[최근 30일 스냅샷 조회]
    H --> C{관측이 2회 이상인가}
    C -->|아니오| SKIP1[증감을 알 수 없음<br/>건너뜀]
    C -->|예| CALC[직전 대비 빈자리 증감 계산]
    CALC --> INC{증가분 ≥ 기준?}
    INC -->|아니오| SKIP2[이미 있던 자리거나<br/>오르내림 · 건너뜀]
    INC -->|예| LOOP[대기자 순회]
    LOOP --> DUP{최근에<br/>알린 적 있는가}
    DUP -->|예| SKIP3[간격 미달 · 건너뜀]
    DUP -->|아니오| SEND[알림 저장 · 발송]
    SEND --> MARK[대기 기록에 관측일 기록]
    MARK --> LOOP

    style SEND fill:#d4edda,stroke:#28a745
    style SKIP1 fill:#f8f9fa,stroke:#adb5bd
    style SKIP2 fill:#f8f9fa,stroke:#adb5bd
    style SKIP3 fill:#f8f9fa,stroke:#adb5bd
```

**"빈자리가 있다" 가 아니라 "빈자리가 늘었다" 로 판단합니다.**
계속 자리가 있는 시설은 사용자도 이미 알고 있어서, 매일 알리면 그냥 스팸이 됩니다.

## 마감 임박 알림 판단

```mermaid
flowchart TD
    START([스케줄러 10:00]) --> LOAD[활성 정책 조회]
    LOAD --> DL{마감일이 있는가}
    DL -->|없음| SKIP0[대상 아님]
    DL -->|있음| LEAD{남은 일수가<br/>D-7 또는 D-1 인가}
    LEAD -->|아니오| SKIP1[해당 없음]
    LEAD -->|예| HIST[오늘 이미 받은 사용자 조회]
    HIST --> USER[활성 사용자 순회]
    USER --> SENT{오늘 이미<br/>받았는가}
    SENT -->|예| SKIP2[중복 방지]
    SENT -->|아니오| T1{자녀가 있는가}
    T1 -->|없음| SKIP3[대상 아님]
    T1 -->|있음| T2{지역이 맞는가}
    T2 -->|아니오| SKIP4[다른 지역]
    T2 -->|예| T3{자녀수 요건<br/>충족하는가}
    T3 -->|아니오| SKIP5[대상 아님]
    T3 -->|예| T4{소득이 기준을<br/>넘는가}
    T4 -->|넘음| SKIP6[대상 아님]
    T4 -->|미입력·이하| T5{연령이 맞는<br/>아이가 있는가}
    T5 -->|없음| SKIP7[대상 아님]
    T5 -->|있음| REC[발송 이력 저장<br/>유니크 제약]
    REC --> SEND[알림 저장 · 발송]
    SEND --> USER

    style SEND fill:#d4edda,stroke:#28a745
    style REC fill:#fff3cd,stroke:#ffc107
```

**소득 미입력은 탈락시키지 않습니다.** 놓친 사람의 손해가 잘못 받은 알림의 성가심보다 훨씬 크기
때문입니다. 반대로 소득이 기준을 명확히 넘으면 보내지 않습니다.

발송 이력을 **먼저** 저장하는 것도 의도적입니다. 이 서비스는 Blue/Green 배포라 인스턴스가 잠깐
2대가 될 수 있고, 그러면 유니크 제약만이 중복을 막습니다.

## 지원금 총액 계산

```mermaid
flowchart LR
    P[정책 목록] --> ELIG{자격 판정}
    ELIG -->|미충족| DROP[제외]
    ELIG -->|충족| TYPE{지급 유형}
    TYPE -->|월정액| M["금액 × min(지급개월, 남은개월)"]
    TYPE -->|일시금| L[금액 그대로]
    TYPE -->|융자·현물| X[총액에서 제외<br/>별도 안내]
    TYPE -->|금액 미상| U[unknownAmountCount 로 노출]
    M & L --> EX{배타 그룹}
    EX -->|같은 그룹| MAX[최댓값 하나만]
    EX -->|무관| ADD[합산]
    MAX & ADD --> TOTAL[예상 총액]

    style X fill:#f8d7da,stroke:#dc3545
    style U fill:#fff3cd,stroke:#ffc107
```

이 계산은 처음에 **2억 9,506만 원** 이라는 값을 냈습니다. 원인이 두 가지였습니다.

1. 자격 요건(자녀수·소득)을 보지 않고 전부 더했습니다.
2. **대상 연령 상한을 지급 기간으로 착각**했습니다. 아빠육아휴직보너스 250만 원을 60개월 곱하면
   1억 5천만 원이 됩니다.

지급 기간 컬럼을 분리하고 자격 판정을 넣어 **8,056만 원**이 되었습니다.
자세한 내용은 [지원금 지능화 문서](../features/benefit-intelligence.md)에 있습니다.
