package com.carecode.domain.health.app;

import com.carecode.domain.health.dto.request.HealthRequest;
import com.carecode.domain.health.dto.request.HealthCreateHealthRecordRequest;
import com.carecode.domain.health.dto.request.HealthUpdateHealthRecordRequest;
import com.carecode.domain.health.dto.request.HealthCreateHospitalReviewRequest;
import com.carecode.domain.health.dto.request.HealthUpdateHospitalReviewRequest;
import com.carecode.domain.health.dto.response.HealthResponse;
import com.carecode.domain.health.dto.response.HealthRecordResponse;
import com.carecode.domain.health.dto.response.VaccineScheduleResponse;
import com.carecode.domain.health.dto.response.CheckupScheduleResponse;
import com.carecode.domain.health.dto.response.HealthStatsResponse;
import com.carecode.domain.health.dto.response.HealthAlertResponse;
import com.carecode.domain.health.dto.response.HospitalInfoResponse;
import com.carecode.domain.health.dto.response.HospitalReviewResponse;
import com.carecode.core.exception.HospitalNotFoundException;
import com.carecode.core.exception.HospitalReviewNotFoundException;
import com.carecode.core.exception.HospitalReviewAccessDeniedException;
import com.carecode.domain.health.service.HealthService;
import com.carecode.domain.health.entity.Hospital;
import com.carecode.domain.health.entity.HospitalLike;
import com.carecode.domain.health.entity.HospitalReview;
import com.carecode.domain.health.repository.HospitalRepository;
import com.carecode.domain.health.repository.HospitalLikeRepository;
import com.carecode.domain.health.repository.HospitalReviewRepository;
import lombok.RequiredArgsConstructor;
import com.carecode.domain.health.mapper.HospitalMapper;
import com.carecode.domain.health.mapper.HospitalReviewMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HealthFacade {

    private final HealthService healthService;
    private final HospitalRepository hospitalRepository;
    private final HospitalLikeRepository hospitalLikeRepository;
    private final HospitalReviewRepository hospitalReviewRepository;
    private final HospitalMapper hospitalMapper;
    private final HospitalReviewMapper hospitalReviewMapper;

    // ==================== 건강 기록 관리 ====================
    // 트랜잭션은 Service 계층에서 관리하므로 Facade에서는 제거

    public HealthRecordResponse createHealthRecord(HealthCreateHealthRecordRequest request) {
        return healthService.createHealthRecord(request);
    }

    public HealthRecordResponse getHealthRecordById(Long recordId) {
        return healthService.getHealthRecordById(recordId);
    }

    public List<HealthRecordResponse> getHealthRecordsByUserId(String userId) {
        return healthService.getHealthRecordsByUserId(userId);
    }

    public HealthRecordResponse updateHealthRecord(Long recordId, HealthUpdateHealthRecordRequest request) {
        return healthService.updateHealthRecord(recordId, request);
    }

    public void deleteHealthRecord(Long recordId) {
        healthService.deleteHealthRecord(recordId);
    }

    public HealthStatsResponse getHealthStatistics(String userId) {
        return healthService.getHealthStatistics(userId);
    }

    public List<VaccineScheduleResponse> getVaccineSchedule(String childId) {
        return healthService.getVaccineSchedule(childId);
    }

    public List<CheckupScheduleResponse> getCheckupSchedule(String childId) {
        return healthService.getCheckupSchedule(childId);
    }

    public List<HealthAlertResponse> getHealthAlerts(String userId) {
        return healthService.getHealthAlerts(userId);
    }

    public List<HealthRecordResponse> getHealthRecordsByDateRangeAsc(Long childId, LocalDate startDate, LocalDate endDate) {
        return healthService.getHealthRecordsByDateRangeAsc(childId, startDate, endDate);
    }

    public List<HealthRecordResponse> getHealthRecordsByType(Long childId, com.carecode.domain.health.entity.HealthRecord.RecordType recordType) {
        return healthService.getHealthRecordsByType(childId, recordType);
    }

    public List<com.carecode.domain.health.dto.response.ChildInfoResponse> getChildrenByAgeRange(Long userId, Integer minAge, Integer maxAge) {
        return healthService.getChildrenByAgeRange(userId, minAge, maxAge);
    }

    public List<com.carecode.domain.health.dto.response.ChildInfoResponse> getChildrenByGender(Long userId, String gender) {
        return healthService.getChildrenByGender(userId, gender);
    }

    public List<com.carecode.domain.health.dto.response.ChildInfoResponse> getChildrenWithSpecialNeeds(Long userId) {
        return healthService.getChildrenWithSpecialNeeds(userId);
    }

    public List<com.carecode.domain.health.dto.response.ChildInfoResponse> searchChildrenByName(Long userId, String name) {
        return healthService.searchChildrenByName(userId, name);
    }

    // ==================== 건강 분석 및 리포트 ====================

    public Map<String, Object> analyzeHealthStatus(HealthCreateHealthRecordRequest request) {
        return healthService.analyzeHealthStatus(request);
    }

    public Map<String, Object> generateHealthReport(HealthCreateHealthRecordRequest request) {
        return healthService.generateHealthReport(request);
    }

    public Map<String, Object> getHealthGoals(String userId) {
        return healthService.getHealthGoals(userId);
    }

    public List<Map<String, Object>> getHealthChart(String userId, String type, String from, String to) {
        LocalDate fromDate = from != null ? LocalDate.parse(from) : null;
        LocalDate toDate = to != null ? LocalDate.parse(to) : null;
        return healthService.getHealthChart(userId, type, fromDate, toDate);
    }

    public Map<String, Object> checkSystemHealth() {
        return healthService.checkSystemHealth();
    }

    // ==================== 병원 관리 ====================
    // 병원 관련 작업은 Facade에서 직접 처리하므로 트랜잭션 필요
    // 하지만 Service 계층으로 이동하는 것이 더 나음 (향후 개선)

    public List<HospitalInfoResponse> getAllHospitals() {
        return hospitalRepository.findAll().stream()
                .map(hospitalMapper::toResponse)
                .toList();
    }

    public HospitalInfoResponse getHospitalById(Long id) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new HospitalNotFoundException(id));
        return hospitalMapper.toResponse(hospital);
    }

    public boolean likeHospital(Long id, Long userId) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new HospitalNotFoundException(id));
        
        // 이미 좋아요를 누른 경우 false 반환
        if (hospitalLikeRepository.existsByHospitalIdAndUserId(id, userId)) {
            return false;
        }
        
        HospitalLike like = HospitalLike.builder()
                .hospital(hospital)
                .userId(userId)
                .createdAt(java.time.LocalDateTime.now())
                .build();
        hospitalLikeRepository.save(like);
        return true;
    }

    public boolean unlikeHospital(Long id, Long userId) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new HospitalNotFoundException(id));
        
        // 좋아요를 누르지 않은 경우 false 반환
        if (!hospitalLikeRepository.existsByHospitalIdAndUserId(id, userId)) {
            return false;
        }
        
        hospitalLikeRepository.deleteByHospitalIdAndUserId(id, userId);
        return true;
    }

    public long getLikeCount(Long id) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new HospitalNotFoundException(id));
        
        return hospitalLikeRepository.countByHospitalId(id);
    }

    public List<HospitalInfoResponse> getNearbyHospitals(double lat, double lng, double radius) {
        // 반경을 미터 단위로 변환 (km -> m)
        double radiusInMeters = radius * 1000;
        
        return hospitalRepository.findNearby(lat, lng, radiusInMeters).stream()
                .map(hospitalMapper::toResponse)
                .toList();
    }

    public List<HospitalInfoResponse> getHospitalsByType(String type) {
        return hospitalRepository.findByType(type).stream()
                .map(hospitalMapper::toResponse)
                .toList();
    }

    public List<HospitalInfoResponse> getPopularHospitals(int limit) {
        return hospitalRepository.findAll().stream()
                .sorted((h1, h2) -> Long.compare(
                        hospitalLikeRepository.countByHospitalId(h2.getId()),
                        hospitalLikeRepository.countByHospitalId(h1.getId())))
                .limit(limit)
                .map(hospitalMapper::toResponse)
                .toList();
    }

    // ==================== 병원 리뷰 관리 ====================

    public List<HospitalReviewResponse> getHospitalReviews(Long hospitalId) {
        return hospitalReviewRepository.findByHospitalId(hospitalId).stream()
                .map(hospitalReviewMapper::toResponse)
                .toList();
    }

    public HospitalReviewResponse createHospitalReview(Long hospitalId, Long userId, Integer rating, String content) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new HospitalNotFoundException(hospitalId));
        
        HospitalReview review = HospitalReview.builder()
                .hospital(hospital)
                .userId(userId)
                .rating(rating)
                .content(content)
                .build();
        
        HospitalReview savedReview = hospitalReviewRepository.save(review);
        return hospitalReviewMapper.toResponse(savedReview);
    }

    public HospitalReviewResponse updateHospitalReview(Long reviewId, Long userId, Integer rating, String content) {
        HospitalReview review = hospitalReviewRepository.findById(reviewId)
                .orElseThrow(() -> new HospitalReviewNotFoundException(reviewId));
        
        if (!review.getUserId().equals(userId)) {
            throw new HospitalReviewAccessDeniedException("리뷰를 수정할 권한이 없습니다.");
        }
        
        review.setRating(rating);
        review.setContent(content);
        
        HospitalReview updatedReview = hospitalReviewRepository.save(review);
        return hospitalReviewMapper.toResponse(updatedReview);
    }

    public void deleteHospitalReview(Long reviewId, Long userId) {
        HospitalReview review = hospitalReviewRepository.findById(reviewId)
                .orElseThrow(() -> new HospitalReviewNotFoundException(reviewId));
        
        if (!review.getUserId().equals(userId)) {
            throw new HospitalReviewAccessDeniedException("리뷰를 삭제할 권한이 없습니다.");
        }
        
        hospitalReviewRepository.delete(review);
    }

    // ==================== Helper Methods ====================

    // 매핑은 HospitalMapper/HospitalReviewMapper에 위임
}


