package com.carecode.domain.health.service;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.exception.CareServiceException;
import com.carecode.core.exception.UserNotFoundException;
import com.carecode.core.exception.HealthRecordNotFoundException;
import com.carecode.core.exception.ChildNotFoundException;
import com.carecode.core.exception.BusinessException;
import com.carecode.core.exception.ErrorCode;
import com.carecode.domain.health.dto.request.HealthRequest;
import com.carecode.domain.health.dto.request.HealthCreateHealthRecordRequest;
import com.carecode.domain.health.dto.request.HealthUpdateHealthRecordRequest;
import com.carecode.domain.health.dto.request.HealthCreateChildRequest;
import com.carecode.domain.health.dto.response.HealthResponse;
import com.carecode.domain.health.dto.response.HealthRecordResponse;
import com.carecode.domain.health.dto.response.VaccineScheduleResponse;
import com.carecode.domain.health.dto.response.CheckupScheduleResponse;
import com.carecode.domain.health.dto.response.HealthStatsResponse;
import com.carecode.domain.health.dto.response.HealthAlertResponse;
import com.carecode.domain.health.dto.response.ChildInfoResponse;
import com.carecode.domain.health.dto.response.GrowthTrendResponse;
import com.carecode.domain.health.dto.response.GrowthDataResponse;
import com.carecode.domain.health.dto.response.HospitalReviewResponse;
import com.carecode.domain.health.entity.HealthRecord;
import com.carecode.domain.health.entity.HospitalReview;
import com.carecode.domain.health.repository.HealthRecordRepository;
import com.carecode.domain.health.repository.HospitalReviewRepository;
import com.carecode.domain.user.entity.Child;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.ChildRepository;
import com.carecode.domain.user.repository.UserRepository;
import com.carecode.domain.health.mapper.HealthRecordMapper;
import com.carecode.domain.health.mapper.ChildMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 통합 건강 관리 서비스 클래스
 * 건강 기록, 아동 정보, 건강 분석 등 모든 건강 관련 비즈니스 로직을 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HealthService {
    
    // 상수 정의
    private static final String DEFAULT_CHILD_NAME = "아동";
    private static final String DEFAULT_ALERT_PRIORITY = "MEDIUM";
    private static final String DEFAULT_TREND = "정상";
    private static final int DEFAULT_NUTRITION_PROGRESS = 85;
    private static final int DEFAULT_MONTHS_FOR_NEXT_CHECKUP = 3;
    private static final int MAX_UPCOMING_EVENTS = 5;
    private static final int HEALTH_SCORE_HIGH_THRESHOLD = 80;
    private static final int HEALTH_SCORE_MEDIUM_THRESHOLD = 60;
    
    private final HealthRecordRepository healthRecordRepository;
    private final ChildRepository childRepository;
    private final UserRepository userRepository;
    private final HospitalReviewRepository hospitalReviewRepository;
    private final HealthRecordMapper healthRecordMapper;
    private final ChildMapper childMapper;
    
    // ===== 건강 기록 관리 =====
    
    /**
     * 건강 기록 생성
     */
    @LogExecutionTime
    @Transactional
    public HealthRecordResponse createHealthRecord(HealthCreateHealthRecordRequest request) {
        validateRequest(request);
        log.info("건강 기록 생성: 아이ID={}, 제목={}", request.getChildId(), request.getTitle());
        
        try {
            // Child 엔티티 조회
            Long childId = Long.valueOf(request.getChildId());
            Child child = childRepository.findById(childId)
                    .orElseThrow(() -> new ChildNotFoundException(childId));
            
            // HealthRecord 엔티티 생성 (매퍼 사용)
            HealthRecord record = healthRecordMapper.toEntity(request);
            record.setChild(child);
            record.setUser(child.getUser());
            
            HealthRecord savedRecord = healthRecordRepository.save(record);
            return healthRecordMapper.toResponse(savedRecord);
        } catch (NumberFormatException e) {
            log.error("잘못된 아동 ID 형식: {}", request.getChildId());
            throw new CareServiceException("잘못된 아동 ID 형식입니다: " + request.getChildId(), e);
        } catch (CareServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("건강 기록 생성 실패: {}", e.getMessage(), e);
            throw new CareServiceException("건강 기록 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 건강 기록 조회
     */
    @LogExecutionTime
    public HealthRecordResponse getHealthRecordById(Long recordId) {
        validateRecordId(recordId);
        log.info("건강 기록 조회: 기록ID={}", recordId);
        
        try {
            HealthRecord record = healthRecordRepository.findById(recordId)
                    .orElseThrow(() -> new HealthRecordNotFoundException(recordId));
            
            return healthRecordMapper.toResponse(record);
        } catch (CareServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("건강 기록 조회 실패: {}", e.getMessage(), e);
            throw new CareServiceException("건강 기록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 사용자별 건강 기록 조회 (DTO 반환)
     */
    @LogExecutionTime
    public List<HealthRecordResponse> getHealthRecordsByUserId(String userId) {
        log.info("사용자별 건강 기록 조회: 사용자ID={}", userId);
        try {
            List<HealthRecord> records = getHealthRecordsByUserIdAsEntity(userId);
            return records.stream()
                    .map(healthRecordMapper::toResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("사용자별 건강 기록 조회 실패: {}", e.getMessage());
            throw new CareServiceException("사용자별 건강 기록 조회 중 오류가 발생했습니다.", e);
        }
    }

    // HealthRecord -> DTO 변환은 healthRecordMapper 사용

    /**
     * 사용자별 건강 기록 조회 (Entity 반환)
     * JOIN FETCH를 사용하여 N+1 쿼리 문제 해결
     */
    @LogExecutionTime
    public List<HealthRecord> getHealthRecordsByUserIdAsEntity(String userId) {
        validateUserId(userId);
        User user = findUserByIdOrUserId(userId);
        // JOIN FETCH를 사용하여 Child와 User를 한 번에 조회
        return healthRecordRepository.findByUserIdWithChildAndUser(user.getId());
    }

    /**
     * 건강 기록 수정
     */
    @LogExecutionTime
    @Transactional
    public HealthRecordResponse updateHealthRecord(Long recordId, HealthUpdateHealthRecordRequest request) {
        validateRecordId(recordId);
        validateUpdateRequest(request);
        
        HealthRecord record = healthRecordRepository.findById(recordId)
                .orElseThrow(() -> new CareServiceException("건강 기록을 찾을 수 없습니다: " + recordId));
        
        // 기록 업데이트
        record.setTitle(request.getTitle());
        record.setDescription(request.getDescription());
        record.setRecordDate(request.getRecordDate() != null ? request.getRecordDate().toLocalDate() : null);
        record.setNextDate(request.getNextDate() != null ? request.getNextDate().toLocalDate() : null);
        record.setLocation(request.getLocation());
        record.setDoctorName(request.getDoctorName());
        record.setIsCompleted(request.getIsCompleted());
        
        HealthRecord updatedRecord = healthRecordRepository.save(record);
        log.info("건강 기록 수정 완료: 기록ID={}", recordId);
        return healthRecordMapper.toResponse(updatedRecord);
    }

    /**
     * 건강 기록 삭제
     */
    @LogExecutionTime
    @Transactional
    public void deleteHealthRecord(Long recordId) {
        validateRecordId(recordId);
        
        HealthRecord record = healthRecordRepository.findById(recordId)
                .orElseThrow(() -> new HealthRecordNotFoundException(recordId));
        
        healthRecordRepository.delete(record);
        log.info("건강 기록이 삭제되었습니다: 기록ID={}", recordId);
    }

    /**
     * 건강 기록 목록 조회 (페이징)
     */
    @LogExecutionTime
    public List<HealthRecordResponse> getHealthRecords(Long childId, int page, int size) {
        validateChildId(childId);
        validatePaginationParams(page, size);
        
        log.info("건강 기록 목록 조회 - 아동 ID: {}, 페이지: {}, 크기: {}", childId, page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "recordDate"));
        Page<HealthRecord> records = healthRecordRepository.findByChildIdOrderByRecordDateDesc(childId, pageable);
        
        return records.getContent().stream()
                .map(healthRecordMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 기간별 건강 기록 조회
     * JOIN FETCH를 사용하여 N+1 쿼리 문제 해결
     */
    @LogExecutionTime
    public List<HealthRecordResponse> getHealthRecordsByDateRange(Long childId, LocalDate startDate, LocalDate endDate) {
        validateChildId(childId);
        validateDateRange(startDate, endDate);
        
        log.info("기간별 건강 기록 조회 - 아동 ID: {}, 시작일: {}, 종료일: {}", childId, startDate, endDate);
        
        // JOIN FETCH를 사용하여 Child와 User를 한 번에 조회
        List<HealthRecord> records = healthRecordRepository.findByChildIdAndRecordDateBetweenWithChildAndUser(
                childId, startDate, endDate);
        
        return records.stream()
                .map(healthRecordMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ===== 아동 정보 관리 =====

    /**
     * 아동 정보 조회
     */
    @LogExecutionTime
    public ChildInfoResponse getChildById(Long childId) {
        validateChildId(childId);
        log.info("아동 정보 조회 - 아동 ID: {}", childId);
        
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new CareServiceException("아동을 찾을 수 없습니다: " + childId));
        
        return childMapper.toResponse(child);
    }

    /**
     * 사용자별 아동 목록 조회
     */
    @LogExecutionTime
    public List<ChildInfoResponse> getChildrenByUserId(Long userId) {
        log.info("사용자별 아동 목록 조회 - 사용자 ID: {}", userId);
        
        List<Child> children = childRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return children.stream()
                .map(childMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 연령 범위별 자녀 조회
     */
    @LogExecutionTime
    public List<ChildInfoResponse> getChildrenByAgeRange(Long userId, Integer minAge, Integer maxAge) {
        log.info("연령 범위별 자녀 조회 - 사용자 ID: {}, 최소 연령: {}, 최대 연령: {}", userId, minAge, maxAge);
        
        List<Child> children = childRepository.findByUserIdAndAgeRange(userId, minAge, maxAge);
        return children.stream()
                .map(childMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 성별 자녀 조회
     */
    @LogExecutionTime
    public List<ChildInfoResponse> getChildrenByGender(Long userId, String gender) {
        log.info("성별 자녀 조회 - 사용자 ID: {}, 성별: {}", userId, gender);
        
        List<Child> children = childRepository.findByUserIdAndGender(userId, gender);
        return children.stream()
                .map(childMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 특별한 요구사항이 있는 자녀 조회
     */
    @LogExecutionTime
    public List<ChildInfoResponse> getChildrenWithSpecialNeeds(Long userId) {
        log.info("특별한 요구사항이 있는 자녀 조회 - 사용자 ID: {}", userId);
        
        List<Child> children = childRepository.findByUserIdAndHasSpecialNeeds(userId);
        return children.stream()
                .map(childMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 이름으로 자녀 검색
     */
    @LogExecutionTime
    public List<ChildInfoResponse> searchChildrenByName(Long userId, String name) {
        log.info("이름으로 자녀 검색 - 사용자 ID: {}, 이름: {}", userId, name);
        
        List<Child> children = childRepository.findByUserIdAndNameContaining(userId, name);
        return children.stream()
                .map(childMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 아동 정보 생성
     */
    @Transactional
    public ChildInfoResponse createChild(HealthCreateChildRequest request) {
        log.info("아동 정보 생성 - 사용자 ID: {}, 이름: {}", request.getUserId(), request.getName());
        
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + request.getUserId()));
        
        Child child = Child.builder()
                .user(user)
                .name(request.getName())
                .birthDate(request.getBirthDate())
                .gender(request.getGender())
                .build();
        
        Child savedChild = childRepository.save(child);
        return childMapper.toResponse(savedChild);
    }

    // ===== 성장 추이 및 분석 =====

    /**
     * 성장 추이 조회
     */
    @LogExecutionTime
    public GrowthTrendResponse getGrowthTrend(Long childId, int months) {
        validateChildId(childId);
        validateMonths(months);
        
        log.info("성장 추이 조회 - 아동 ID: {}, 개월: {}", childId, months);
        
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new CareServiceException("아동을 찾을 수 없습니다: " + childId));
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(months);
        
        List<HealthRecord> records = healthRecordRepository.findByChildIdAndRecordDateBetweenOrderByRecordDateAsc(
                childId, startDate, endDate);
        
        return GrowthTrendResponse.builder()
                .childId(childId.toString())
                .childName(child.getName())
                .growthData(records.stream()
                        .filter(r -> r.getRecordDate() != null)
                        .map(r -> GrowthDataResponse.builder()
                        .date(r.getRecordDate().toString())
                        .height(r.getHeight())
                        .weight(r.getWeight())
                        .headCircumference(null)
                        .notes(r.getDescription())
                                .build())
                        .collect(Collectors.toList()))
                .period(months + "개월")
                .trend(DEFAULT_TREND)
                .build();
    }

    /**
     * 건강 상태 분석
     */
    @LogExecutionTime
    public Map<String, Object> analyzeHealthStatus(HealthCreateHealthRecordRequest request) {
        validateRequest(request);
        
        Long childId = Long.valueOf(request.getChildId());
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new CareServiceException("아동을 찾을 수 없습니다: " + request.getChildId()));
        
        List<HealthRecord> records = healthRecordRepository.findByChildOrderByRecordDateDesc(child);
        
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("childId", request.getChildId());
        analysis.put("totalRecords", records.size());
        analysis.put("healthScore", calculateHealthScore(records));
        analysis.put("riskLevel", determineRiskLevel(records));
        analysis.put("recommendations", generateRecommendations(records));
        analysis.put("nextCheckup", LocalDateTime.now().plusMonths(DEFAULT_MONTHS_FOR_NEXT_CHECKUP).toString());
        
        return analysis;
    }

    /**
     * 건강 리포트 생성
     */
    @LogExecutionTime
    public Map<String, Object> generateHealthReport(HealthCreateHealthRecordRequest request) {
        validateRequest(request);
        
        Long childId = Long.valueOf(request.getChildId());
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new CareServiceException("아동을 찾을 수 없습니다: " + request.getChildId()));

        List<HealthRecord> records = healthRecordRepository.findByChildOrderByRecordDateDesc(child);

        Map<String, Object> report = new HashMap<>();
        report.put("childId", request.getChildId());
        report.put("reportDate", LocalDateTime.now().toString());
        report.put("summary", generateHealthSummary(records));
        report.put("vaccineStatus", calculateVaccineStatus(records));
        report.put("checkupStatus", calculateCheckupStatus(records));
        report.put("recommendations", generateRecommendations(records));

        return report;
    }

    /**
     * 건강 통계 조회
     */
    @LogExecutionTime
    public HealthStatsResponse getHealthStatistics(String userId) {
        log.info("건강 통계 조회: 사용자ID={}", userId);
        
        try {
            List<HealthRecord> records = getHealthRecordsByUserIdAsEntity(userId);
            
            return HealthStatsResponse.builder()
                    .totalRecords(records.size())
                    .completedVaccines(countCompletedVaccines(records))
                    .pendingVaccines(countPendingVaccines(records))
                    .completedCheckups(countCompletedCheckups(records))
                    .pendingCheckups(countPendingCheckups(records))
                    .recordTypeDistribution(calculateRecordTypeDistribution(records))
                    .upcomingEvents(generateUpcomingEvents(records))
                    .build();
        } catch (Exception e) {
            log.error("건강 통계 조회 실패: {}", e.getMessage());
            throw new CareServiceException("건강 통계 조회 중 오류가 발생했습니다.", e);
        }
    }

    // ===== 스케줄 및 알림 관리 =====

    /**
     * 예방접종 스케줄 조회
     */
    @LogExecutionTime
    public List<VaccineScheduleResponse> getVaccineSchedule(String childId) {
        log.info("예방접종 스케줄 조회: 아동ID={}", childId);
        
        try {
            List<HealthRecord> vaccineRecords = healthRecordRepository.findByChildIdAndRecordType(
                    Long.valueOf(childId), HealthRecord.RecordType.VACCINATION);
            
            return vaccineRecords.stream()
                    .map(this::convertToVaccineScheduleResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("예방접종 스케줄 조회 실패: {}", e.getMessage());
            throw new CareServiceException("예방접종 스케줄 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 건강 검진 스케줄 조회
     */
    @LogExecutionTime
    public List<CheckupScheduleResponse> getCheckupSchedule(String childId) {
        log.info("건강 검진 스케줄 조회: 아동ID={}", childId);
        
        try {
            List<HealthRecord> checkupRecords = healthRecordRepository.findByChildIdAndRecordType(
                    Long.valueOf(childId), HealthRecord.RecordType.CHECKUP);
            
            return checkupRecords.stream()
                    .map(this::convertToCheckupScheduleResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("건강 검진 스케줄 조회 실패: {}", e.getMessage());
            throw new CareServiceException("건강 검진 스케줄 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 기간별 건강 기록 조회 (오래된순)
     * JOIN FETCH를 사용하여 N+1 쿼리 문제 해결
     */
    @LogExecutionTime
    public List<HealthRecordResponse> getHealthRecordsByDateRangeAsc(Long childId, LocalDate startDate, LocalDate endDate) {
        validateChildId(childId);
        validateDateRange(startDate, endDate);
        
        log.info("기간별 건강 기록 조회 (오래된순): 아동ID={}, 시작일={}, 종료일={}", childId, startDate, endDate);
        
        try {
            // JOIN FETCH를 사용하여 Child와 User를 한 번에 조회
            List<HealthRecord> records = healthRecordRepository.findByChildIdAndRecordDateBetweenOrderByRecordDateAscWithChildAndUser(
                    childId, startDate, endDate);
            
            return records.stream()
                    .map(healthRecordMapper::toResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("기간별 건강 기록 조회 실패: {}", e.getMessage(), e);
            throw new CareServiceException("기간별 건강 기록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 특정 타입의 건강 기록 조회
     * JOIN FETCH를 사용하여 N+1 쿼리 문제 해결
     */
    @LogExecutionTime
    public List<HealthRecordResponse> getHealthRecordsByType(Long childId, HealthRecord.RecordType recordType) {
        validateChildId(childId);
        log.info("타입별 건강 기록 조회: 아동ID={}, 타입={}", childId, recordType);
        
        try {
            // JOIN FETCH를 사용하여 Child와 User를 한 번에 조회
            List<HealthRecord> records = healthRecordRepository.findByChildIdAndRecordTypeWithChildAndUser(childId, recordType);
            
            return records.stream()
                    .map(healthRecordMapper::toResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("타입별 건강 기록 조회 실패: {}", e.getMessage(), e);
            throw new CareServiceException("타입별 건강 기록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 건강 알림 조회
     */
    @LogExecutionTime
    public List<HealthAlertResponse> getHealthAlerts(String userId) {
        log.info("건강 알림 조회: 사용자ID={}", userId);
        
        try {
            List<HealthRecord> records = getHealthRecordsByUserIdAsEntity(userId);
            
            return records.stream()
                    .filter(r -> r.getNextDate() != null && r.getNextDate().isAfter(java.time.LocalDate.now()))
                    .map(this::convertToHealthAlertResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("건강 알림 조회 실패: {}", e.getMessage());
            throw new CareServiceException("건강 알림 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 건강 알림 설정
     * TODO: 실제 알림 설정 엔티티 및 저장 로직 구현 필요
     */
    @LogExecutionTime
    @Transactional
    public void setHealthAlert(HealthCreateHealthRecordRequest request) {
        validateRequest(request);
        log.info("건강 알림 설정: 아이ID={}", request.getChildId());

        try {
            // TODO: 실제로는 알림 설정을 저장하는 로직이 필요합니다
            // 예: HealthAlert 엔티티 생성 및 저장
            log.info("건강 알림이 설정되었습니다: 아이ID={}", request.getChildId());
        } catch (Exception e) {
            log.error("건강 알림 설정 실패: {}", e.getMessage(), e);
            throw new CareServiceException("건강 알림 설정 중 오류가 발생했습니다.", e);
        }
    }

    // ===== 건강 목표 관리 =====

    /**
     * 건강 목표 설정
     * TODO: 실제 건강 목표 엔티티 및 저장 로직 구현 필요
     */
    @LogExecutionTime
    @Transactional
    public void setHealthGoal(HealthCreateHealthRecordRequest request) {
        validateRequest(request);
        
        try {
            // TODO: 실제로는 건강 목표를 저장하는 로직이 필요합니다
            // 예: HealthGoal 엔티티 생성 및 저장
            log.info("건강 목표가 설정되었습니다: 아이ID={}", request.getChildId());
        } catch (Exception e) {
            log.error("건강 목표 설정 실패: {}", e.getMessage(), e);
            throw new CareServiceException("건강 목표 설정 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 건강 목표 조회
     */
    @LogExecutionTime
    public Map<String, Object> getHealthGoals(String userId) {
        validateUserId(userId);
        User user = findUserByIdOrUserId(userId);
        
        List<HealthRecord> records = healthRecordRepository.findByUserOrderByRecordDateDesc(user);
        
        Map<String, Object> goals = new HashMap<>();
        goals.put("userId", userId);
        goals.put("vaccineGoal", "모든 예방접종 완료");
        goals.put("checkupGoal", "정기 검진 100% 완료");
        goals.put("nutritionGoal", "균형 잡힌 영양 섭취");
        goals.put("progress", calculateProgress(records));
        
        return goals;
    }

    // ===== 차트 및 시각화 =====

    /**
     * 건강 차트 데이터 조회
     */
    @LogExecutionTime
    public List<Map<String, Object>> getHealthChart(String userId, String type, LocalDate from, LocalDate to) {
        validateUserId(userId);
        validateChartType(type);
        validateDateRange(from, to);
        
        User user = findUserByIdOrUserId(userId);
        List<HealthRecord> records = healthRecordRepository.findByUserOrderByRecordDateDesc(user);
        
        return records.stream()
                .filter(r -> r.getRecordDate() != null)
                .filter(r -> {
                    LocalDate localDate = r.getRecordDate();
                    return (from == null || !localDate.isBefore(from)) && (to == null || !localDate.isAfter(to));
                })
                .map(r -> {
                    Object value = extractChartValue(r, type);
                    return Map.of(
                        "date", r.getRecordDate().toString(),
                        "value", value != null ? value : ""
                    );
                })
                .collect(Collectors.toList());
    }

    // ===== 시스템 관리 =====

    /**
     * 시스템 상태 확인
     */
    public Map<String, Object> checkSystemHealth() {
        log.info("시스템 상태 확인");
        
        Map<String, Object> healthStatus = new HashMap<>();
        healthStatus.put("status", "UP");
        healthStatus.put("timestamp", System.currentTimeMillis());
        healthStatus.put("version", "1.0.0");
        
        return healthStatus;
    }

    // ===== Helper Methods =====



    /**
     * Child Entity를 DTO로 변환
     */
    // Child 매핑은 ChildMapper 사용

    /**
     * 예방접종 스케줄 응답 DTO 변환
     */
    private VaccineScheduleResponse convertToVaccineScheduleResponse(HealthRecord record) {
        return VaccineScheduleResponse.builder()
                .vaccineName(record.getTitle())
                .description(record.getDescription())
                .recommendedAge(null) // 나이 정보는 별도 계산 필요
                .status(Boolean.TRUE.equals(record.getIsCompleted()) ? "COMPLETED" : "UPCOMING")
                .scheduledDate(record.getRecordDate() != null ? record.getRecordDate().toString() : null)
                .completedDate(Boolean.TRUE.equals(record.getIsCompleted()) ? record.getUpdatedAt().toString() : null)
                .notes(record.getDescription())
                .build();
    }

    /**
     * 건강 검진 스케줄 응답 DTO 변환
     */
    private CheckupScheduleResponse convertToCheckupScheduleResponse(HealthRecord record) {
        return CheckupScheduleResponse.builder()
                .checkupName(record.getTitle())
                .description(record.getDescription())
                .recommendedAge(null) // 나이 정보는 별도 계산 필요
                .status(Boolean.TRUE.equals(record.getIsCompleted()) ? "COMPLETED" : "UPCOMING")
                .scheduledDate(record.getRecordDate() != null ? record.getRecordDate().toString() : null)
                .completedDate(Boolean.TRUE.equals(record.getIsCompleted()) ? record.getUpdatedAt().toString() : null)
                .notes(record.getDescription())
                .build();
    }

    /**
     * 건강 알림 응답 DTO 변환
     */
    private HealthAlertResponse convertToHealthAlertResponse(HealthRecord record) {
        return HealthAlertResponse.builder()
                .alertId(record.getId().toString())
                .alertType(record.getRecordType() != null ? record.getRecordType().name() : null)
                .title(record.getTitle())
                .message(record.getDescription())
                .priority(DEFAULT_ALERT_PRIORITY)
                .dueDate(record.getNextDate() != null ? record.getNextDate().toString() : null)
                .isRead(false)
                .build();
    }

    // ===== 계산 및 분석 Helper Methods =====

    private int calculateHealthScore(List<HealthRecord> records) {
        if (records.isEmpty()) return 0;
        
        int completedVaccines = countCompletedVaccines(records);
        int totalVaccines = (int) records.stream()
                .filter(r -> r.getRecordType() == HealthRecord.RecordType.VACCINATION)
                .count();
        
        return totalVaccines > 0 ? (completedVaccines * 100) / totalVaccines : 0;
    }

    private String determineRiskLevel(List<HealthRecord> records) {
        if (records.isEmpty()) return "UNKNOWN";
        
        int healthScore = calculateHealthScore(records);
        if (healthScore >= HEALTH_SCORE_HIGH_THRESHOLD) return "LOW";
        else if (healthScore >= HEALTH_SCORE_MEDIUM_THRESHOLD) return "MEDIUM";
        else return "HIGH";
    }

    private List<String> generateRecommendations(List<HealthRecord> records) {
        List<String> recommendations = new java.util.ArrayList<>();
        
        if (records.isEmpty()) {
            recommendations.add("첫 건강 기록을 등록해주세요");
            return recommendations;
        }
        
        int completedVaccines = countCompletedVaccines(records);
        int totalVaccines = (int) records.stream()
                .filter(r -> r.getRecordType() == HealthRecord.RecordType.VACCINATION)
                .count();
        
        if (completedVaccines < totalVaccines) {
            recommendations.add("예방접종 완료를 권장합니다");
        }
        
        if (records.stream().noneMatch(r -> r.getRecordType() == HealthRecord.RecordType.CHECKUP)) {
            recommendations.add("정기 건강검진을 권장합니다");
        }
        
        return recommendations;
    }

    private int countCompletedVaccines(List<HealthRecord> records) {
        return (int) records.stream()
                .filter(r -> r.getRecordType() == HealthRecord.RecordType.VACCINATION && Boolean.TRUE.equals(r.getIsCompleted()))
                .count();
    }

    private int countPendingVaccines(List<HealthRecord> records) {
        return (int) records.stream()
                .filter(r -> r.getRecordType() == HealthRecord.RecordType.VACCINATION && !Boolean.TRUE.equals(r.getIsCompleted()))
                .count();
    }

    private int countCompletedCheckups(List<HealthRecord> records) {
        return (int) records.stream()
                .filter(r -> r.getRecordType() == HealthRecord.RecordType.CHECKUP && Boolean.TRUE.equals(r.getIsCompleted()))
                .count();
    }

    private int countPendingCheckups(List<HealthRecord> records) {
        return (int) records.stream()
                .filter(r -> r.getRecordType() == HealthRecord.RecordType.CHECKUP && !Boolean.TRUE.equals(r.getIsCompleted()))
                .count();
    }

    private Map<String, Integer> calculateRecordTypeDistribution(List<HealthRecord> records) {
        return records.stream()
                .collect(Collectors.groupingBy(
                    r -> r.getRecordType().name(),
                    Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));
    }

    private List<String> generateUpcomingEvents(List<HealthRecord> records) {
        return records.stream()
                .filter(r -> r.getNextDate() != null && r.getNextDate().isAfter(java.time.LocalDate.now()))
                .map(r -> String.format("%s: %s", r.getTitle(), r.getNextDate()))
                .limit(MAX_UPCOMING_EVENTS)
                .collect(Collectors.toList());
    }

    private String generateHealthSummary(List<HealthRecord> records) {
        if (records.isEmpty()) return "건강 기록이 없습니다.";
        
        int totalRecords = records.size();
        int completedRecords = (int) records.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsCompleted()))
                .count();
        
        return String.format("총 %d개의 건강 기록 중 %d개 완료 (완료율: %d%%)", 
                totalRecords, completedRecords, totalRecords > 0 ? (completedRecords * 100) / totalRecords : 0);
    }

    private String calculateVaccineStatus(List<HealthRecord> records) {
        int completedVaccines = countCompletedVaccines(records);
        int totalVaccines = (int) records.stream()
                .filter(r -> r.getRecordType() == HealthRecord.RecordType.VACCINATION)
                .count();
        
        return totalVaccines > 0 ? 
                String.format("완료율 %d%%", (completedVaccines * 100) / totalVaccines) : 
                "예방접종 기록 없음";
    }

    private String calculateCheckupStatus(List<HealthRecord> records) {
        int completedCheckups = countCompletedCheckups(records);
        int totalCheckups = (int) records.stream()
                .filter(r -> r.getRecordType() == HealthRecord.RecordType.CHECKUP)
                .count();
        
        return totalCheckups > 0 ? 
                String.format("완료율 %d%%", (completedCheckups * 100) / totalCheckups) : 
                "검진 기록 없음";
    }

    private Map<String, Integer> calculateProgress(List<HealthRecord> records) {
        Map<String, Integer> progress = new HashMap<>();
        
        int completedVaccines = countCompletedVaccines(records);
        int totalVaccines = (int) records.stream()
                .filter(r -> r.getRecordType() == HealthRecord.RecordType.VACCINATION)
                .count();
        
        int completedCheckups = countCompletedCheckups(records);
        int totalCheckups = (int) records.stream()
                .filter(r -> r.getRecordType() == HealthRecord.RecordType.CHECKUP)
                .count();
        
        progress.put("vaccine", totalVaccines > 0 ? (completedVaccines * 100) / totalVaccines : 0);
        progress.put("checkup", totalCheckups > 0 ? (completedCheckups * 100) / totalCheckups : 0);
        progress.put("nutrition", DEFAULT_NUTRITION_PROGRESS); // TODO: 실제 영양 진행률 계산 로직 필요
        
        return progress;
    }

    /**
     * 사용자별 병원 리뷰 조회
     */
    @LogExecutionTime
    public List<HospitalReviewResponse> getHospitalReviewsByUser(String userId) {
        log.info("사용자별 병원 리뷰 조회: 사용자ID={}", userId);
        
        try {
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new CareServiceException("사용자를 찾을 수 없습니다: " + userId));
            
            List<HospitalReview> reviews = hospitalReviewRepository.findByUser(user);
            
            return reviews.stream()
                    .map(review -> HospitalReviewResponse.builder()
                            .id(review.getId())
                            .hospitalId(review.getHospital().getId())
                            .hospitalName(review.getHospital().getName())
                            .userId(review.getUser().getId())
                            .userName(review.getUser().getName())
                            .rating(review.getRating())
                            .content(review.getContent())
                            .createdAt(review.getCreatedAt() != null ? review.getCreatedAt().toString() : null)
                            .updatedAt(review.getUpdatedAt() != null ? review.getUpdatedAt().toString() : null)
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("사용자별 병원 리뷰 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new CareServiceException("사용자별 병원 리뷰 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * HealthRecord 엔티티를 HealthRecordResponse DTO로 변환
     */
    // HealthRecord 매핑은 HealthRecordMapper 사용
    
    // ===== Validation Helper Methods =====
    
    /**
     * User ID 또는 Long ID로 사용자 조회 (중복 로직 제거)
     */
    private User findUserByIdOrUserId(String userId) {
        validateUserId(userId);
        
        // userId(문자열)로 먼저 조회
        Optional<User> userOpt = userRepository.findByUserId(userId);
        if (userOpt.isPresent()) {
            return userOpt.get();
        }
        
        // userId가 숫자라면 PK(id)로도 조회 시도
        try {
            Long id = Long.parseLong(userId);
            return userRepository.findById(id)
                    .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));
        } catch (NumberFormatException e) {
            throw new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId);
        }
    }
    
    /**
     * 요청 객체 검증
     */
    private void validateRequest(HealthCreateHealthRecordRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "요청 정보가 없습니다.");
        }
        if (!StringUtils.hasText(request.getChildId())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "아동 ID가 필요합니다.");
        }
    }
    
    /**
     * 업데이트 요청 객체 검증
     */
    private void validateUpdateRequest(HealthUpdateHealthRecordRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "요청 정보가 없습니다.");
        }
    }
    
    /**
     * 기록 ID 검증
     */
    private void validateRecordId(Long recordId) {
        if (recordId == null || recordId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_RECORD_ID, 
                    ErrorCode.INVALID_RECORD_ID.getMessage() + ": " + recordId);
        }
    }
    
    /**
     * 아동 ID 검증
     */
    private void validateChildId(Long childId) {
        if (childId == null || childId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_CHILD_ID, 
                    ErrorCode.INVALID_CHILD_ID.getMessage() + ": " + childId);
        }
    }
    
    /**
     * 사용자 ID 검증
     */
    private void validateUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "사용자 ID가 필요합니다.");
        }
    }
    
    /**
     * 페이징 파라미터 검증
     */
    private void validatePaginationParams(int page, int size) {
        if (page < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, 
                    "페이지 번호는 0 이상이어야 합니다: " + page);
        }
        if (size <= 0 || size > 100) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, 
                    "페이지 크기는 1 이상 100 이하여야 합니다: " + size);
        }
    }
    
    /**
     * 날짜 범위 검증
     */
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }
    }
    
    /**
     * 개월 수 검증
     */
    private void validateMonths(int months) {
        if (months <= 0 || months > 120) {
            throw new BusinessException(ErrorCode.INVALID_MONTHS, 
                    ErrorCode.INVALID_MONTHS.getMessage() + ": " + months);
        }
    }
    
    /**
     * 차트 타입 검증
     */
    private void validateChartType(String type) {
        if (!StringUtils.hasText(type)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "차트 타입이 필요합니다.");
        }
        // 지원하는 타입: weight, height, temperature, pulseRate, bloodPressure
        List<String> validTypes = List.of("weight", "height", "temperature", "pulseRate", "bloodPressure");
        if (!validTypes.contains(type.toLowerCase())) {
            throw new BusinessException(ErrorCode.INVALID_CHART_TYPE, 
                    ErrorCode.INVALID_CHART_TYPE.getMessage() + ": " + type);
        }
    }
    
    /**
     * 차트 값 추출
     */
    private Object extractChartValue(HealthRecord record, String type) {
        if (record == null || type == null) {
            return null;
        }
        
        return switch (type.toLowerCase()) {
            case "weight" -> record.getWeight();
            case "height" -> record.getHeight();
            case "temperature" -> record.getTemperature();
            case "pulserate" -> record.getPulseRate();
            case "bloodpressure" -> record.getBloodPressure();
            default -> null;
        };
    }
}
