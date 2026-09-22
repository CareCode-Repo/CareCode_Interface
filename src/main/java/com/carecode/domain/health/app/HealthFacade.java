package com.carecode.domain.health.app;

import com.carecode.domain.health.dto.request.HealthCreateHealthRecordRequest;
import com.carecode.domain.health.dto.request.HealthRecordAttachmentRequest;
import com.carecode.domain.health.dto.request.HealthUpdateHealthRecordRequest;
import com.carecode.domain.health.dto.response.HealthRecordResponse;
import com.carecode.domain.health.dto.response.HealthRecordAttachmentResponse;
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
import com.carecode.domain.user.repository.UserRepository;
import com.carecode.domain.health.repository.HospitalReviewRepository;
import lombok.RequiredArgsConstructor;
import com.carecode.domain.health.mapper.HospitalMapper;
import com.carecode.domain.health.mapper.HospitalReviewMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

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
    private final UserRepository userRepository;

    // ==================== 건강 기록 관리 ====================
    // 트랜잭션은 Service 계층에서 관리하므로 Facade에서는 제거
    public HealthRecordResponse createHealthRecord(HealthCreateHealthRecordRequest request, Long actorUserId) {
        return healthService.createHealthRecord(request, actorUserId);
    }

    public HealthRecordResponse getHealthRecordById(Long recordId, Long actorUserId) {
        return healthService.getHealthRecordById(recordId, actorUserId);
    }

    public List<HealthRecordResponse> getHealthRecordsByUserId(String userId, Long actorUserId) {
        return healthService.getHealthRecordsByUserId(userId, actorUserId);
    }

    public HealthRecordResponse updateHealthRecord(Long recordId, HealthUpdateHealthRecordRequest request, Long actorUserId) {
        return healthService.updateHealthRecord(recordId, request, actorUserId);
    }

    public void deleteHealthRecord(Long recordId, Long actorUserId) {
        healthService.deleteHealthRecord(recordId, actorUserId);
    }

    public HealthStatsResponse getHealthStatistics(String userId, Long actorUserId) {
        return healthService.getHealthStatistics(userId, actorUserId);
    }

    public List<VaccineScheduleResponse> getVaccineSchedule(String childId, Long actorUserId) {
        return healthService.getVaccineSchedule(childId, actorUserId);
    }

    public List<CheckupScheduleResponse> getCheckupSchedule(String childId, Long actorUserId) {
        return healthService.getCheckupSchedule(childId, actorUserId);
    }

    public List<HealthAlertResponse> getHealthAlerts(String userId, Long actorUserId) {
        return healthService.getHealthAlerts(userId, actorUserId);
    }

    public List<HealthRecordResponse> getHealthRecordsByDateRangeAsc(Long childId, LocalDate startDate, LocalDate endDate, Long actorUserId) {
        return healthService.getHealthRecordsByDateRangeAsc(childId, startDate, endDate, actorUserId);
    }

    public List<HealthRecordResponse> getHealthRecordsByType(Long childId, com.carecode.domain.health.entity.HealthRecord.RecordType recordType, Long actorUserId) {
        return healthService.getHealthRecordsByType(childId, recordType, actorUserId);
    }

    public HealthRecordAttachmentResponse addAttachment(Long recordId, HealthRecordAttachmentRequest request, Long actorUserId) {
        return healthService.addAttachment(recordId, request, actorUserId);
    }

    public List<HealthRecordAttachmentResponse> getAttachments(Long recordId, Long actorUserId) {
        return healthService.getAttachments(recordId, actorUserId);
    }

    public void deleteAttachment(Long attachmentId, Long actorUserId) {
        healthService.deleteAttachment(attachmentId, actorUserId);
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

    // ====================
    // 건강 분석 및 리포트 ====================
    public Map<String, Object> analyzeHealthStatus(HealthCreateHealthRecordRequest request, Long actorUserId) {
        return healthService.analyzeHealthStatus(request, actorUserId);
    }

    public Map<String, Object> generateHealthReport(HealthCreateHealthRecordRequest request, Long actorUserId) {
        return healthService.generateHealthReport(request, actorUserId);
    }

    public Map<String, Object> getHealthGoals(String userId, Long actorUserId) {
        return healthService.getHealthGoals(userId, actorUserId);
    }

    public Map<String, Object> getIntegratedRecommendations(String userId, Long actorUserId) {
        return healthService.getIntegratedRecommendations(userId, actorUserId);
    }

    public List<Map<String, Object>> getHealthChart(String userId, String type, String from, String to, Long actorUserId) {
        LocalDate fromDate = from != null ? LocalDate.parse(from) : null;
        LocalDate toDate = to != null ? LocalDate.parse(to) : null;
        return healthService.getHealthChart(userId, type, fromDate, toDate, actorUserId);
    }

    public Map<String, Object> checkSystemHealth() {
        return healthService.checkSystemHealth();
    }

    // ==================== 병원 관리 ====================
    // 병원 관련 작업은 Facade에서 직접 처리하므로 트랜잭션 필요 하지만
    public List<HospitalInfoResponse> getAllHospitals(int page, int size) {
        // 테이블 전체를 메모리로 올리지 않도록 항상 페이지 단위로 읽는다.
        return hospitalRepository.findAll(PageRequest.of(page, size, Sort.by("name")))
                .getContent().stream()
                .map(hospitalMapper::toResponse)
                .toList();
    }

    public HospitalInfoResponse getHospitalById(Long id) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new HospitalNotFoundException(id));
        return hospitalMapper.toResponse(hospital);
    }

    @Transactional
    public boolean likeHospital(Long id, Long userId) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new HospitalNotFoundException(id));
        
        // 이미 좋아요를 누른 경우 false 반환
        if (hospitalLikeRepository.existsByHospitalIdAndUserId(id, userId)) {
            return false;
        }
        
        // userId 필드는 insertable=false 인 읽기 전용 그림자다. 여기에 값을 넣어도
        // user_id 컬럼에는 아무것도 쓰이지 않아 그동안 모든 찜이 user_id=NULL 로 저장됐다.
        // 그 결과 중복 확인·해제·찜 여부가 전부 어긋났다. 연관 자체를 채운다.
        HospitalLike like = HospitalLike.builder()
                .hospital(hospital)
                .user(userRepository.getReferenceById(userId))
                .createdAt(java.time.LocalDateTime.now())
                .build();
        hospitalLikeRepository.save(like);
        return true;
    }

    /** 파생 delete 는 트랜잭션 없이는 실행되지 않는다. 이게 없어 찜 해제가 항상 500 이었다. */
    @Transactional
    public boolean unlikeHospital(Long id, Long userId) {
        hospitalRepository.findById(id).orElseThrow(() -> new HospitalNotFoundException(id));
        
        // 좋아요를 누르지 않은 경우 false 반환
        if (!hospitalLikeRepository.existsByHospitalIdAndUserId(id, userId)) {
            return false;
        }
        
        hospitalLikeRepository.deleteByHospitalIdAndUserId(id, userId);
        return true;
    }

    /** 내가 찜한 병원 목록. 찜을 걸 수는 있는데 모아 볼 방법이 없었다. */
    public List<HospitalInfoResponse> getLikedHospitals(Long userId) {
        return hospitalLikeRepository.findLikedWithHospitalByUserId(userId).stream()
                .map(like -> hospitalMapper.toResponse(like.getHospital()))
                .toList();
    }

    public long getLikeCount(Long id) {
        hospitalRepository.findById(id).orElseThrow(() -> new HospitalNotFoundException(id));

        return hospitalLikeRepository.countByHospitalId(id);
    }

    /** 현재 사용자가 이 병원을 찜했는지 여부. 이 값이 없으면 클라이언트가 찜 상태를 화면에 유지할 수 없어 새로고침마다 초기화된다. */
    public boolean isLikedByUser(Long id, Long userId) {
        hospitalRepository.findById(id).orElseThrow(() -> new HospitalNotFoundException(id));

        return hospitalLikeRepository.existsByHospitalIdAndUserId(id, userId);
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

    @Transactional(readOnly = true)
    public com.carecode.domain.health.dto.response.HospitalStatsResponse getHospitalStats() {
        java.util.Map<String, Long> byType = new java.util.HashMap<>();
        long total = 0;
        for (Object[] row : hospitalRepository.countByType()) {
            String type = row[0] == null || row[0].toString().isBlank() ? "기타" : row[0].toString();
            long count = ((Number) row[1]).longValue();
            byType.merge(type, count, Long::sum);
            total += count;
        }
        java.util.Map<String, Long> sorted = new java.util.LinkedHashMap<>();
        byType.entrySet().stream()
                .sorted(java.util.Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(java.util.Map.Entry.comparingByKey()))
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        return com.carecode.domain.health.dto.response.HospitalStatsResponse.builder()
                .totalHospitals(total)
                .byType(sorted)
                .build();
    }

    public List<HospitalInfoResponse> getPopularHospitals(int limit) {
        int safeLimit = Math.max(limit, 1);
        return hospitalRepository.findPopularHospitals(PageRequest.of(0, safeLimit)).stream()
                .limit(safeLimit)
                .map(hospitalMapper::toResponse)
                .toList();
    }

    // ====================
    // 병원 리뷰 관리 ====================
    public List<HospitalReviewResponse> getHospitalReviews(Long hospitalId) {
        return hospitalReviewRepository.findByHospitalId(hospitalId).stream()
                .map(hospitalReviewMapper::toResponse)
                .toList();
    }

    @Transactional
    public HospitalReviewResponse createHospitalReview(Long hospitalId, Long userId, Integer rating, String content) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new HospitalNotFoundException(hospitalId));
        
        // 찜과 같은 이유로 user 연관을 채운다 (userId 는 읽기 전용 그림자다)
        HospitalReview review = HospitalReview.builder()
                .hospital(hospital)
                .user(userRepository.getReferenceById(userId))
                .rating(rating)
                .content(content)
                .build();
        
        HospitalReview savedReview = hospitalReviewRepository.save(review);
        return hospitalReviewMapper.toResponse(savedReview);
    }

    @Transactional
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

    @Transactional
    public void deleteHospitalReview(Long reviewId, Long userId) {
        HospitalReview review = hospitalReviewRepository.findById(reviewId)
                .orElseThrow(() -> new HospitalReviewNotFoundException(reviewId));
        
        if (!review.getUserId().equals(userId)) {
            throw new HospitalReviewAccessDeniedException("리뷰를 삭제할 권한이 없습니다.");
        }
        
        hospitalReviewRepository.delete(review);
    }

    // ====================
    // Helper Methods ====================

    // 매핑은 HospitalMapper/HospitalReviewMapper에 위임

    /** 조건부 응답용 지문. 일정이 수정되면 값이 바뀌어 캐시가 무효화된다. */
    public String getVaccineScheduleVersion(String childId) {
        return healthService.getVaccineScheduleVersion(childId);
    }
}
