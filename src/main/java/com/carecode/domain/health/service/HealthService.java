package com.carecode.domain.health.service;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.exception.CareServiceException;
import com.carecode.domain.health.dto.HealthRequest;
import com.carecode.domain.health.dto.HealthResponse;
import com.carecode.domain.health.entity.HealthRecord;
import com.carecode.domain.health.repository.HealthRecordRepository;
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
    
    private final HealthRecordRepository healthRecordRepository;
    private final ChildRepository childRepository;
    private final UserRepository userRepository;
    private final HealthRecordMapper healthRecordMapper;
    private final ChildMapper childMapper;
    
    // ===== 건강 기록 관리 =====
    
    /**
     * 건강 기록 생성
     */
    @LogExecutionTime
    @Transactional
    public HealthResponse.HealthRecordResponse createHealthRecord(HealthRequest.CreateHealthRecord request) {
        log.info("건강 기록 생성: 아이ID={}, 제목={}", request.getChildId(), request.getTitle());
        
        try {
            // Child 엔티티 조회
            Child child = childRepository.findById(Long.valueOf(request.getChildId()))
                    .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다: " + request.getChildId()));
            
            // HealthRecord 엔티티 생성 (매퍼 사용)
            HealthRecord record = healthRecordMapper.toEntity(request);
            record.setChild(child);
            record.setUser(child.getUser());
            
            HealthRecord savedRecord = healthRecordRepository.save(record);
            return healthRecordMapper.toResponse(savedRecord);
        } catch (Exception e) {
            log.error("건강 기록 생성 실패: {}", e.getMessage());
            throw new CareServiceException("건강 기록 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 건강 기록 조회
     */
    @LogExecutionTime
    public HealthResponse.HealthRecordResponse getHealthRecordById(Long recordId) {
        log.info("건강 기록 조회: 기록ID={}", recordId);
        
        try {
            HealthRecord record = healthRecordRepository.findById(recordId)
                    .orElseThrow(() -> new IllegalArgumentException("건강 기록을 찾을 수 없습니다: " + recordId));
            
            return healthRecordMapper.toResponse(record);
        } catch (Exception e) {
            log.error("건강 기록 조회 실패: {}", e.getMessage());
            throw new CareServiceException("건강 기록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 사용자별 건강 기록 조회 (DTO 반환)
     */
    @LogExecutionTime
    public List<HealthResponse.HealthRecordResponse> getHealthRecordsByUserId(String userId) {
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
     */
    @LogExecutionTime
    public List<HealthRecord> getHealthRecordsByUserIdAsEntity(String userId) {
        // userId(문자열)로 먼저 조회
        Optional<User> userOpt = userRepository.findByUserId(userId);
        User user;
        if (userOpt.isPresent()) {
            user = userOpt.get();
        } else {
            // userId가 숫자라면 PK(id)로도 조회 시도
            try {
                Long id = Long.parseLong(userId);
                user = userRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId);
            }
        }
        return healthRecordRepository.findByUserOrderByRecordDateDesc(user);
    }

    /**
     * 건강 기록 수정
     */
    @LogExecutionTime
    @Transactional
    public HealthResponse.HealthRecordResponse updateHealthRecord(Long recordId, HealthRequest.UpdateHealthRecord request) {
        HealthRecord record = healthRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("건강 기록을 찾을 수 없습니다: " + recordId));
        
        // 기록 업데이트
        record.setTitle(request.getTitle());
        record.setDescription(request.getDescription());
        record.setRecordDate(request.getRecordDate() != null ? request.getRecordDate().toLocalDate() : null);
        record.setNextDate(request.getNextDate() != null ? request.getNextDate().toLocalDate() : null);
        record.setLocation(request.getLocation());
        record.setDoctorName(request.getDoctorName());
        record.setIsCompleted(request.getIsCompleted());
        
        HealthRecord updatedRecord = healthRecordRepository.save(record);
        return healthRecordMapper.toResponse(updatedRecord);
    }

    /**
     * 건강 기록 삭제
     */
    @LogExecutionTime
    @Transactional
    public void deleteHealthRecord(Long recordId) {
        HealthRecord record = healthRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("건강 기록을 찾을 수 없습니다: " + recordId));
        
        healthRecordRepository.delete(record);
        log.info("건강 기록이 삭제되었습니다: 기록ID={}", recordId);
    }

    /**
     * 건강 기록 목록 조회 (페이징)
     */
    @LogExecutionTime
    public List<HealthResponse.HealthRecordResponse> getHealthRecords(Long childId, int page, int size) {
        log.info("건강 기록 목록 조회 - 아동 ID: {}", childId);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "recordDate"));
        Page<HealthRecord> records = healthRecordRepository.findByChildIdOrderByRecordDateDesc(childId, pageable);
        
        return records.getContent().stream()
                .map(healthRecordMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 기간별 건강 기록 조회
     */
    @LogExecutionTime
    public List<HealthResponse.HealthRecordResponse> getHealthRecordsByDateRange(Long childId, LocalDate startDate, LocalDate endDate) {
        log.info("기간별 건강 기록 조회 - 아동 ID: {}, 시작일: {}, 종료일: {}", childId, startDate, endDate);
        
        List<HealthRecord> records = healthRecordRepository.findByChildIdAndRecordDateBetweenOrderByRecordDateDesc(
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
    public HealthResponse.Child getChildById(Long childId) {
        log.info("아동 정보 조회 - 아동 ID: {}", childId);
        
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다: " + childId));
        
        return childMapper.toResponse(child);
    }

    /**
     * 사용자별 아동 목록 조회
     */
    @LogExecutionTime
    public List<HealthResponse.Child> getChildrenByUserId(Long userId) {
        log.info("사용자별 아동 목록 조회 - 사용자 ID: {}", userId);
        
        List<Child> children = childRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return children.stream()
                .map(childMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 아동 정보 생성
     */
    @Transactional
    public HealthResponse.Child createChild(HealthRequest.CreateChild request) {
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
    public HealthResponse.GrowthTrend getGrowthTrend(Long childId, int months) {
        log.info("성장 추이 조회 - 아동 ID: {}, 개월: {}", childId, months);
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(months);
        
        List<HealthRecord> records = healthRecordRepository.findByChildIdAndRecordDateBetweenOrderByRecordDateAsc(
                childId, startDate, endDate);
        
        return HealthResponse.GrowthTrend.builder()
                .childId(childId.toString())
                .childName("아동") // 실제로는 Child 엔티티에서 조회
                .growthData(records.stream().map(r -> HealthResponse.GrowthData.builder()
                        .date(r.getRecordDate().toString())
                        .height(r.getHeight())
                        .weight(r.getWeight())
                        .headCircumference(null)
                        .notes(r.getDescription())
                        .build()).collect(Collectors.toList()))
                .period(months + "개월")
                .trend("정상")
                .build();
    }

    /**
     * 건강 상태 분석
     */
    @LogExecutionTime
    public Map<String, Object> analyzeHealthStatus(HealthRequest.CreateHealthRecord request) {
        Child child = childRepository.findById(Long.valueOf(request.getChildId()))
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다: " + request.getChildId()));
        
        List<HealthRecord> records = healthRecordRepository.findByChildOrderByRecordDateDesc(child);
        
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("childId", request.getChildId());
        analysis.put("totalRecords", records.size());
        analysis.put("healthScore", calculateHealthScore(records));
        analysis.put("riskLevel", determineRiskLevel(records));
        analysis.put("recommendations", generateRecommendations(records));
        analysis.put("nextCheckup", LocalDateTime.now().plusMonths(3).toString());
        
        return analysis;
    }

    /**
     * 건강 리포트 생성
     */
    @LogExecutionTime
    public Map<String, Object> generateHealthReport(HealthRequest.CreateHealthRecord request) {
        Child child = childRepository.findById(Long.valueOf(request.getChildId()))
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다: " + request.getChildId()));

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
    public HealthResponse.HealthStatsResponse getHealthStatistics(String userId) {
        log.info("건강 통계 조회: 사용자ID={}", userId);
        
        try {
            List<HealthRecord> records = getHealthRecordsByUserIdAsEntity(userId);
            
            return HealthResponse.HealthStatsResponse.builder()
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
    public List<HealthResponse.VaccineScheduleResponse> getVaccineSchedule(String childId) {
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
    public List<HealthResponse.CheckupScheduleResponse> getCheckupSchedule(String childId) {
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
     * 건강 알림 조회
     */
    @LogExecutionTime
    public List<HealthResponse.HealthAlertResponse> getHealthAlerts(String userId) {
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
     */
    @LogExecutionTime
    public void setHealthAlert(HealthRequest.CreateHealthRecord request) {
        log.info("건강 알림 설정: 아이ID={}", request.getChildId());

        try {
            // 실제로는 알림 설정을 저장
            log.info("건강 알림이 설정되었습니다: 아이ID={}", request.getChildId());
        } catch (Exception e) {
            log.error("건강 알림 설정 실패: {}", e.getMessage());
            throw new CareServiceException("건강 알림 설정 중 오류가 발생했습니다.", e);
        }
    }

    // ===== 건강 목표 관리 =====

    /**
     * 건강 목표 설정
     */
    @LogExecutionTime
    public void setHealthGoal(HealthRequest.CreateHealthRecord request) {
        try {
            // 실제로는 건강 목표를 저장
            log.info("건강 목표가 설정되었습니다: 아이ID={}", request.getChildId());
        } catch (Exception e) {
            log.error("건강 목표 설정 실패: {}", e.getMessage());
            throw new CareServiceException("건강 목표 설정 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 건강 목표 조회
     */
    @LogExecutionTime
    public Map<String, Object> getHealthGoals(String userId) {
        // userId(문자열)로 먼저 조회
        Optional<User> userOpt = userRepository.findByUserId(userId);
        User user;
        if (userOpt.isPresent()) {
            user = userOpt.get();
        } else {
            Long id = Long.parseLong(userId);
            user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        }
        
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
    public List<Map<String, Object>> getHealthChart(String userId, String type, LocalDate from, LocalDate to) {
        User user = userRepository.findByUserId(userId).orElseThrow();
        List<HealthRecord> records = healthRecordRepository.findByUserOrderByRecordDateDesc(user);
        return records.stream()
                .filter(r -> {
                    LocalDate localDate = r.getRecordDate();
                    return (from == null || !localDate.isBefore(from)) && (to == null || !localDate.isAfter(to));
                })
                .map(r -> {
                    Object value = null;
                    switch (type) {
                        case "weight": value = r.getWeight(); break;
                        case "height": value = r.getHeight(); break;
                        case "temperature": value = r.getTemperature(); break;
                        case "pulseRate": value = r.getPulseRate(); break;
                        case "bloodPressure": value = r.getBloodPressure(); break;
                        // 필요시 추가
                        default: value = null;
                    }
                    return Map.of(
                        "date", r.getRecordDate().toString(),
                        "value", value
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
    private HealthResponse.VaccineScheduleResponse convertToVaccineScheduleResponse(HealthRecord record) {
        return HealthResponse.VaccineScheduleResponse.builder()
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
    private HealthResponse.CheckupScheduleResponse convertToCheckupScheduleResponse(HealthRecord record) {
        return HealthResponse.CheckupScheduleResponse.builder()
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
    private HealthResponse.HealthAlertResponse convertToHealthAlertResponse(HealthRecord record) {
        return HealthResponse.HealthAlertResponse.builder()
                .alertId(record.getId().toString())
                .alertType(record.getRecordType().name())
                .title(record.getTitle())
                .message(record.getDescription())
                .priority("MEDIUM")
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
        if (healthScore >= 80) return "LOW";
        else if (healthScore >= 60) return "MEDIUM";
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
                .limit(5)
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
        progress.put("nutrition", 85); // 임시 값
        
        return progress;
    }

    /**
     * HealthRecord 엔티티를 HealthRecordResponse DTO로 변환
     */
    // HealthRecord 매핑은 HealthRecordMapper 사용
}
