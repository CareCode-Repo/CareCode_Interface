# CareCode 프로젝트 아키텍처 개선 제안서

## 📋 목차
1. [현재 아키텍처 분석](#현재-아키텍처-분석)
2. [주요 개선 사항](#주요-개선-사항)
3. [상세 개선 제안](#상세-개선-제안)
4. [우선순위별 개선 로드맵](#우선순위별-개선-로드맵)

---

## 현재 아키텍처 분석

### ✅ 잘 구현된 부분

1. **계층형 아키텍처**
   - Controller → Facade → Service → Repository 패턴 적용
   - 도메인별 패키지 분리 (domain/user, domain/health 등)
   - Core 패키지에 공통 기능 집중

2. **공통 기능 모듈화**
   - AOP를 활용한 로깅, 캐싱, 인증, 검증
   - MapStruct를 통한 DTO 매핑
   - 예외 처리 전역 핸들러

3. **기술 스택**
   - Spring Boot 3.3.3, Java 17
   - JPA/Hibernate, MariaDB
   - Redis 캐싱
   - JWT 인증
   - Swagger/OpenAPI 문서화

---

## 주요 개선 사항

### 🔴 높은 우선순위 (Critical)

#### 1. 테스트 코드 부족
**현재 상태**: `CareCodeApplicationTests.java`만 존재, 단위/통합 테스트 거의 없음

**개선 제안**:
```java
// 예시: Service 계층 단위 테스트
@ExtendWith(MockitoExtension.class)
class HealthServiceTest {
    @Mock
    private HealthRecordRepository healthRecordRepository;
    
    @InjectMocks
    private HealthService healthService;
    
    @Test
    void createHealthRecord_ShouldReturnResponse_WhenValidRequest() {
        // Given
        HealthCreateHealthRecordRequest request = ...
        HealthRecord savedRecord = ...
        
        // When
        HealthRecordResponse response = healthService.createHealthRecord(request);
        
        // Then
        assertThat(response).isNotNull();
        verify(healthRecordRepository).save(any());
    }
}
```

**구현 계획**:
- [ ] Service 계층 단위 테스트 (Mockito)
- [ ] Repository 계층 통합 테스트 (@DataJpaTest)
- [ ] Controller 계층 통합 테스트 (@WebMvcTest)
- [ ] API 통합 테스트 (@SpringBootTest + TestRestTemplate)
- [ ] 테스트 커버리지 목표: 70% 이상

---

#### 2. 예외 처리 일관성 부족
**현재 문제**:
- `IllegalArgumentException`과 `CareServiceException` 혼용
- Facade에서 `IllegalArgumentException` 직접 사용 (예: HealthFacade.java:162)
- 예외 메시지가 일관되지 않음

**개선 제안**:
```java
// 1. 예외 계층 구조 개선
public abstract class CareCodeException extends RuntimeException {
    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;
}

public class HealthRecordNotFoundException extends CareCodeException {
    public HealthRecordNotFoundException(Long id) {
        super(ErrorCode.HEALTH_RECORD_NOT_FOUND, 
              HttpStatus.NOT_FOUND, 
              "건강 기록을 찾을 수 없습니다: " + id);
    }
}

// 2. ErrorCode enum 정의
public enum ErrorCode {
    HEALTH_RECORD_NOT_FOUND("H001", "건강 기록을 찾을 수 없습니다"),
    CHILD_NOT_FOUND("H002", "아동을 찾을 수 없습니다"),
    // ...
}

// 3. 전역 예외 핸들러 개선
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(CareCodeException.class)
    public ResponseEntity<ErrorResponse> handleCareCodeException(CareCodeException ex) {
        return ResponseEntity
            .status(ex.getHttpStatus())
            .body(ErrorResponse.of(ex.getErrorCode(), ex.getMessage()));
    }
}
```

**구현 계획**:
- [ ] 예외 계층 구조 정의
- [ ] ErrorCode enum 생성
- [ ] 모든 Facade/Service에서 일관된 예외 사용
- [ ] 예외 응답 형식 표준화

---

#### 3. API 버전 관리 부재
**현재 상태**: `/api/v1` 하드코딩, 버전 관리 전략 없음

**개선 제안**:
```java
// 1. 버전별 Controller 분리
@RestController
@RequestMapping("/api/v1/health")
public class HealthControllerV1 { ... }

@RestController
@RequestMapping("/api/v2/health")
public class HealthControllerV2 { ... }

// 2. 또는 URL 경로 버전 관리
@RestController
@RequestMapping("/api/health")
@ApiVersion("v1")
public class HealthController { ... }

// 3. 헤더 기반 버전 관리
@RestController
@RequestMapping("/api/health")
public class HealthController {
    @GetMapping(headers = "API-Version=v1")
    public ResponseEntity<?> getV1() { ... }
}
```

**구현 계획**:
- [ ] API 버전 관리 전략 수립
- [ ] 버전별 문서화
- [ ] Deprecation 정책 정의

---

### 🟡 중간 우선순위 (Important)

#### 4. 설정 파일 관리 개선
**현재 문제**:
- `application.properties`에 모든 설정 집중
- 환경별 설정 분리 부족
- 민감 정보 관리 개선 필요

**개선 제안**:
```
src/main/resources/
├── application.yml (기본)
├── application-dev.yml
├── application-staging.yml
├── application-prod.yml
└── application-docker.yml
```

```yaml
# application.yml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

# application-prod.yml
logging:
  level:
    root: WARN
    com.carecode: INFO
  file:
    name: /var/log/carecode/application.log
```

**구현 계획**:
- [ ] YAML 형식으로 전환 (가독성 향상)
- [ ] 환경별 프로파일 분리
- [ ] 외부 설정 파일 지원 (ConfigMap/Secret)
- [ ] 설정 검증 로직 추가

---

#### 5. 로깅 전략 개선
**현재 상태**: 기본 로깅만 사용, 구조화된 로깅 부족

**개선 제안**:
```java
// 1. 구조화된 로깅 (JSON 형식)
@Slf4j
public class HealthService {
    public HealthRecordResponse createHealthRecord(HealthCreateHealthRecordRequest request) {
        MDC.put("userId", request.getUserId());
        MDC.put("childId", request.getChildId());
        
        log.info("건강 기록 생성 시작", 
                 kv("childId", request.getChildId()),
                 kv("recordType", request.getRecordType()));
        
        try {
            // ...
            log.info("건강 기록 생성 완료", kv("recordId", savedRecord.getId()));
            return response;
        } catch (Exception e) {
            log.error("건강 기록 생성 실패", 
                     kv("childId", request.getChildId()),
                     kv("error", e.getMessage()),
                     e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
}

// 2. 로깅 설정 (logback-spring.xml)
<configuration>
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp/>
                <version/>
                <logLevel/>
                <message/>
                <mdc/>
                <stackTrace/>
            </providers>
        </encoder>
    </appender>
</configuration>
```

**구현 계획**:
- [ ] Logback 설정 개선 (JSON 형식)
- [ ] MDC를 활용한 트레이싱
- [ ] 로그 레벨 환경별 설정
- [ ] 로그 집계 시스템 연동 (ELK Stack 등)

---

#### 6. 캐싱 전략 확장
**현재 상태**: `@CacheableResult` 커스텀 어노테이션 사용, 제한적

**개선 제안**:
```java
// 1. 캐싱 전략 명시
@Cacheable(
    value = "healthRecords",
    key = "#childId + '_' + #page + '_' + #size",
    unless = "#result == null || #result.isEmpty()"
)
public List<HealthRecordResponse> getHealthRecords(Long childId, int page, int size) {
    // ...
}

// 2. 캐시 무효화 전략
@CacheEvict(value = "healthRecords", allEntries = true)
public HealthRecordResponse createHealthRecord(...) {
    // ...
}

// 3. 캐시 설정
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build();
    }
}
```

**구현 계획**:
- [ ] 캐싱 전략 문서화
- [ ] TTL 설정 최적화
- [ ] 캐시 히트율 모니터링
- [ ] 분산 캐시 고려 (Redis Cluster)

---

#### 7. 트랜잭션 관리 개선
**현재 문제**:
- Facade와 Service 모두에 `@Transactional` 중복
- 읽기 전용 트랜잭션 명시 부족
- 트랜잭션 전파 전략 불명확

**개선 제안**:
```java
// 1. Service 계층에만 트랜잭션 적용
@Service
@Transactional(readOnly = true) // 기본값
public class HealthService {
    
    @Transactional // 쓰기 작업만 명시
    public HealthRecordResponse createHealthRecord(...) {
        // ...
    }
    
    // 읽기 작업은 기본값 사용
    public HealthRecordResponse getHealthRecordById(Long id) {
        // ...
    }
}

// 2. Facade는 트랜잭션 없이 위임만
@Service
public class HealthFacade {
    private final HealthService healthService;
    
    // 트랜잭션 어노테이션 제거 (Service에 위임)
    public HealthRecordResponse createHealthRecord(...) {
        return healthService.createHealthRecord(...);
    }
}
```

**구현 계획**:
- [ ] 트랜잭션 경계 명확화
- [ ] 읽기 전용 트랜잭션 최적화
- [ ] 트랜잭션 전파 전략 문서화

---

### 🟢 낮은 우선순위 (Nice to Have)

#### 8. API 응답 형식 표준화
**개선 제안**:
```java
// 공통 응답 래퍼
public class ApiResponse<T> {
    private String code;
    private String message;
    private T data;
    private LocalDateTime timestamp;
}

// Controller에서 일관된 응답
@RestController
public class HealthController {
    @GetMapping("/records/{id}")
    public ResponseEntity<ApiResponse<HealthRecordResponse>> getRecord(@PathVariable Long id) {
        HealthRecordResponse data = facade.getHealthRecordById(id);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
```

---

#### 9. 검증 로직 개선
**개선 제안**:
```java
// 1. Bean Validation 활용
public class HealthCreateHealthRecordRequest {
    @NotBlank(message = "제목은 필수입니다")
    private String title;
    
    @NotNull(message = "아동 ID는 필수입니다")
    @Positive(message = "아동 ID는 양수여야 합니다")
    private Long childId;
    
    @PastOrPresent(message = "기록일은 과거 또는 현재여야 합니다")
    private LocalDate recordDate;
}

// 2. 커스텀 검증
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ChildAgeValidator.class)
public @interface ValidChildAge {
    String message() default "아동 연령이 유효하지 않습니다";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

---

#### 10. 문서화 개선
**개선 제안**:
- [ ] API 문서 자동화 강화
- [ ] 아키텍처 다이어그램 추가 (C4 Model)
- [ ] 개발자 온보딩 가이드
- [ ] 배포 가이드 상세화

---

## 상세 개선 제안

### 아키텍처 패턴 개선

#### 현재 구조
```
Controller → Facade → Service → Repository
```

#### 개선 제안: DDD 적용
```
Controller → Application Service (Facade) → Domain Service → Repository
                ↓
            Domain Model (Entity)
```

**장점**:
- 도메인 로직이 Entity에 집중
- Service는 오케스트레이션만 담당
- 테스트 용이성 향상

---

### 성능 최적화

#### 1. N+1 쿼리 문제 해결
```java
// 현재 (문제)
public List<HealthRecordResponse> getHealthRecordsByUserId(String userId) {
    List<HealthRecord> records = repository.findByUserId(userId);
    return records.stream()
        .map(mapper::toResponse) // 각각 Child 조회 발생
        .collect(Collectors.toList());
}

// 개선
@Query("SELECT r FROM HealthRecord r JOIN FETCH r.child WHERE r.user.id = :userId")
List<HealthRecord> findByUserIdWithChild(@Param("userId") Long userId);
```

#### 2. 페이징 최적화
```java
// 현재
public List<HealthRecordResponse> getHealthRecords(Long childId, int page, int size) {
    Page<HealthRecord> records = repository.findByChildId(childId, pageable);
    return records.getContent().stream() // 전체 조회 후 변환
        .map(mapper::toResponse)
        .collect(Collectors.toList());
}

// 개선: DTO 직접 조회
@Query("SELECT new com.carecode.domain.health.dto.response.HealthRecordResponse(...) " +
       "FROM HealthRecord r WHERE r.child.id = :childId")
Page<HealthRecordResponse> findDtoByChildId(@Param("childId") Long childId, Pageable pageable);
```

---

### 보안 강화

#### 1. 입력 검증 강화
```java
@RestController
@Validated
public class HealthController {
    @PostMapping("/records")
    public ResponseEntity<?> createRecord(
        @Valid @RequestBody HealthCreateHealthRecordRequest request) {
        // ...
    }
}
```

#### 2. SQL Injection 방지
- JPA 사용으로 대부분 방지됨
- Native Query 사용 시 파라미터 바인딩 확인 필요

#### 3. Rate Limiting
```java
@RateLimit(maxRequests = 100, windowSeconds = 60)
@PostMapping("/records")
public ResponseEntity<?> createRecord(...) {
    // ...
}
```

---

## 우선순위별 개선 로드맵

### Phase 1 (1-2주): 긴급 개선
1. ✅ 예외 처리 일관성 개선
2. ✅ 테스트 코드 작성 시작 (핵심 기능)
3. ✅ 설정 파일 환경별 분리

### Phase 2 (2-4주): 중요 개선
4. 로깅 전략 개선
5. 캐싱 전략 확장
6. 트랜잭션 관리 개선
7. API 응답 형식 표준화

### Phase 3 (1-2개월): 장기 개선
8. API 버전 관리 도입
9. 성능 최적화 (N+1 쿼리 등)
10. 문서화 개선
11. 모니터링 및 알림 시스템 구축

---

## 결론

현재 프로젝트는 **견고한 기반 구조**를 가지고 있으나, **테스트 코드 부족**과 **예외 처리 일관성** 문제가 가장 시급합니다. 

위 개선 사항들을 단계적으로 적용하면:
- ✅ 코드 품질 향상
- ✅ 유지보수성 개선
- ✅ 안정성 강화
- ✅ 개발 생산성 향상

을 기대할 수 있습니다.

