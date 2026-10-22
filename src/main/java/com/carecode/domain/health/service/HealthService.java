package com.carecode.domain.health.service;

import com.carecode.domain.user.entity.ConsentType;
import com.carecode.domain.user.service.ConsentGuard;
import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.exception.CareCodeException;
import com.carecode.core.exception.CareServiceException;
import com.carecode.core.exception.ResourceNotFoundException;
import com.carecode.core.exception.HealthRecordNotFoundException;
import com.carecode.core.exception.ChildNotFoundException;
import com.carecode.core.exception.BusinessException;
import com.carecode.core.exception.ErrorCode;
import com.carecode.domain.health.dto.request.HealthCreateHealthRecordRequest;
import com.carecode.domain.health.dto.request.HealthRecordAttachmentRequest;
import com.carecode.domain.health.dto.request.HealthUpdateHealthRecordRequest;
import com.carecode.domain.health.dto.response.HealthRecordAttachmentResponse;
import com.carecode.domain.health.dto.response.HealthRecordResponse;
import com.carecode.domain.health.entity.HealthRecord;
import com.carecode.domain.health.entity.HealthRecordAttachment;
import com.carecode.domain.health.repository.HealthRecordAttachmentRepository;
import com.carecode.domain.health.repository.HealthRecordRepository;
import com.carecode.domain.user.app.ChildDirectory;
import com.carecode.domain.user.entity.Child;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.ChildRepository;
import com.carecode.domain.health.mapper.HealthRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/** 통합 건강 관리 서비스 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HealthService {
    
    // 상수 정의
    
    private final HealthRecordRepository healthRecordRepository;
    private final ConsentGuard consentGuard;
    private final HealthRecordAttachmentRepository healthRecordAttachmentRepository;
    /**
     * 건강 기록을 자녀에 붙이려면 엔티티가 필요해 남겨 둔다. 조회·소유권 판단은
     * {@link ChildDirectory} 가 하고, 여기서는 통과한 뒤 연관을 걸 때만 쓴다.
     */
    private final HealthActorResolver actorResolver;
    private final ChildRepository childRepository;
    private final ChildDirectory childDirectory;
    private final HealthRecordMapper healthRecordMapper;
    
    // ===== 건강 기록 관리 =====

    // 건강 기록 생성
    @LogExecutionTime
    @Transactional
    public HealthRecordResponse createHealthRecord(HealthCreateHealthRecordRequest request, Long actorUserId) {
        // 건강정보는 민감정보다. 별도 동의 없이는 수집하지 않는다.
        consentGuard.require(actorUserId, ConsentType.HEALTH_DATA);
        validateRequest(request);
        log.info("건강 기록 생성: 아이ID={}, 제목={}", request.getChildId(), request.getTitle());
        
        try {
            // Child 엔티티 조회
            Long childId = Long.valueOf(request.getChildId());
            Child child = childRepository.findById(childId)
                    .orElseThrow(() -> new ChildNotFoundException(childId));
            assertChildOwnedByUser(child, actorUserId);
            
            // HealthRecord 엔티티 생성 (매퍼 사용)
            HealthRecord record = healthRecordMapper.toEntity(request);
            record.setChild(child);
            record.setUser(child.getUser());
            
            HealthRecord savedRecord = healthRecordRepository.save(record);
            return healthRecordMapper.toResponse(savedRecord);
        } catch (NumberFormatException e) {
            log.error("잘못된 아동 ID 형식: {}", request.getChildId());
            throw new CareServiceException("잘못된 아동 ID 형식입니다: " + request.getChildId(), e);
        } catch (CareCodeException e) {
            throw e;
        } catch (CareServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("건강 기록 생성 실패: {}", e.getMessage(), e);
            throw new CareServiceException("건강 기록 생성 중 오류가 발생했습니다.", e);
        }
    }

    // 건강 기록 조회
    @LogExecutionTime
    public HealthRecordResponse getHealthRecordById(Long recordId, Long actorUserId) {
        validateRecordId(recordId);
        log.info("건강 기록 조회: 기록ID={}", recordId);
        
        try {
            HealthRecord record = healthRecordRepository.findById(recordId)
                    .orElseThrow(() -> new HealthRecordNotFoundException(recordId));
            assertHealthRecordOwnedByUser(record, actorUserId);
            
            return healthRecordMapper.toResponse(record);
        } catch (CareCodeException e) {
            throw e;
        } catch (CareServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("건강 기록 조회 실패: {}", e.getMessage(), e);
            throw new CareServiceException("건강 기록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /** 사용자별 건강 기록. JOIN FETCH 로 Child·User 를 함께 읽어 N+1 을 피한다. */
    @LogExecutionTime
    public List<HealthRecordResponse> getHealthRecordsByUserId(String userId, Long actorUserId) {
        User user = actorResolver.requireSelf(userId, actorUserId);

        return healthRecordRepository.findByUserIdWithChildAndUser(user.getId()).stream()
                .map(healthRecordMapper::toResponse)
                .collect(Collectors.toList());
    }

    // 건강 기록 수정
    @LogExecutionTime
    @Transactional
    public HealthRecordResponse updateHealthRecord(Long recordId, HealthUpdateHealthRecordRequest request, Long actorUserId) {
        validateRecordId(recordId);
        validateUpdateRequest(request);
        
        HealthRecord record = healthRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("건강 기록을 찾을 수 없습니다: " + recordId));
        assertHealthRecordOwnedByUser(record, actorUserId);
        
        // 기록 업데이트
        record.setTitle(request.getTitle());
        record.setDescription(request.getDescription());
        record.setRecordDate(request.getRecordDate() != null ? request.getRecordDate().toLocalDate() : null);
        record.setNextDate(request.getNextDate() != null ? request.getNextDate().toLocalDate() : null);
        record.setDoctorName(request.getDoctorName());
        record.setHospitalName(request.getHospitalName());
        record.setHeight(request.getHeight());
        record.setWeight(request.getWeight());
        record.setTemperature(request.getTemperature());
        // 아래는 수정 화면에 없는 필드다. 안 보냈다고 null 로 덮으면 수정할 때마다 지워진다
        // (완료 여부는 false → null 이 됐다). 값을 보냈을 때만 바꾼다.
        if (request.getLocation() != null) record.setLocation(request.getLocation());
        if (request.getBloodPressure() != null) record.setBloodPressure(request.getBloodPressure());
        if (request.getPulseRate() != null) record.setPulseRate(request.getPulseRate());
        if (request.getVaccineName() != null) record.setVaccineName(request.getVaccineName());
        if (request.getIsCompleted() != null) record.setIsCompleted(request.getIsCompleted());

        HealthRecord updatedRecord = healthRecordRepository.save(record);
        log.info("건강 기록 수정 완료: 기록ID={}", recordId);
        return healthRecordMapper.toResponse(updatedRecord);
    }

    // 건강 기록 삭제
    @LogExecutionTime
    @Transactional
    public void deleteHealthRecord(Long recordId, Long actorUserId) {
        validateRecordId(recordId);
        
        HealthRecord record = healthRecordRepository.findById(recordId)
                .orElseThrow(() -> new HealthRecordNotFoundException(recordId));
        assertHealthRecordOwnedByUser(record, actorUserId);
        
        healthRecordRepository.delete(record);
        log.info("건강 기록이 삭제되었습니다: 기록ID={}", recordId);
    }

    // ===== 아동 정보 관리 =====

    // ===== 스케줄 및 알림 관리 =====

    // 기간별 건강 기록 조회 (오래된순) JOIN FETCH를 사용하여 N+1 쿼리 문제 해결
    @LogExecutionTime
    public List<HealthRecordResponse> getHealthRecordsByDateRangeAsc(Long childId, LocalDate startDate, LocalDate endDate, Long actorUserId) {
        validateChildId(childId);
        validateDateRange(startDate, endDate);
        assertChildOwnedByUserId(childId, actorUserId);
        
        log.info("기간별 건강 기록 조회 (오래된순): 아동ID={}, 시작일={}, 종료일={}", childId, startDate, endDate);
        
        try {
            // JOIN FETCH를 사용하여 Child와 User를 한 번에 조회
            List<HealthRecord> records = healthRecordRepository.findByChildIdAndRecordDateBetweenOrderByRecordDateAscWithChildAndUser(
                    childId, startDate, endDate);
            
            return records.stream()
                    .map(healthRecordMapper::toResponse)
                    .collect(Collectors.toList());
        } catch (CareCodeException e) {
            // 우리가 던진 예외는 자기 상태 코드를 들고 있다. 아래 catch 에 걸리면 404·403 이
            // 전부 500 으로 덮이고, 남의 자녀 접근 거부가 서버 오류로 보고된다.
            throw e;
        } catch (Exception e) {
            log.error("기간별 건강 기록 조회 실패: {}", e.getMessage(), e);
            throw new CareServiceException("기간별 건강 기록 조회 중 오류가 발생했습니다.", e);
        }
    }

    // 특정 타입의 건강 기록 조회 JOIN FETCH를 사용하여 N+1 쿼리 문제 해결
    @LogExecutionTime
    public List<HealthRecordResponse> getHealthRecordsByType(Long childId, HealthRecord.RecordType recordType, Long actorUserId) {
        validateChildId(childId);
        assertChildOwnedByUserId(childId, actorUserId);
        log.info("타입별 건강 기록 조회: 아동ID={}, 타입={}", childId, recordType);
        
        try {
            // JOIN FETCH를 사용하여 Child와 User를 한 번에 조회
            List<HealthRecord> records = healthRecordRepository.findByChildIdAndRecordTypeWithChildAndUser(childId, recordType);
            
            return records.stream()
                    .map(healthRecordMapper::toResponse)
                    .collect(Collectors.toList());
        } catch (CareCodeException e) {
            // 우리가 던진 예외는 자기 상태 코드를 들고 있다. 아래 catch 에 걸리면 404·403 이
            // 전부 500 으로 덮이고, 남의 자녀 접근 거부가 서버 오류로 보고된다.
            throw e;
        } catch (Exception e) {
            log.error("타입별 건강 기록 조회 실패: {}", e.getMessage(), e);
            throw new CareServiceException("타입별 건강 기록 조회 중 오류가 발생했습니다.", e);
        }
    }

    @LogExecutionTime
    @Transactional
    public HealthRecordAttachmentResponse addAttachment(Long recordId, HealthRecordAttachmentRequest request, Long actorUserId) {
        HealthRecord record = healthRecordRepository.findById(recordId)
                .orElseThrow(() -> new HealthRecordNotFoundException(recordId));
        assertHealthRecordOwnedByUser(record, actorUserId);
        HealthRecordAttachment attachment = HealthRecordAttachment.builder()
                .healthRecord(record)
                .fileUrl(request.getFileUrl())
                .fileName(request.getFileName())
                .fileType(request.getFileType())
                .fileSize(request.getFileSize())
                .description(request.getDescription())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .build();
        return toAttachmentResponse(healthRecordAttachmentRepository.save(attachment));
    }

    @LogExecutionTime
    public List<HealthRecordAttachmentResponse> getAttachments(Long recordId, Long actorUserId) {
        HealthRecord record = healthRecordRepository.findById(recordId)
                .orElseThrow(() -> new HealthRecordNotFoundException(recordId));
        assertHealthRecordOwnedByUser(record, actorUserId);
        return healthRecordAttachmentRepository.findByHealthRecordAndIsActiveTrueOrderByDisplayOrderAscCreatedAtDesc(record).stream()
                .map(this::toAttachmentResponse)
                .collect(Collectors.toList());
    }

    @LogExecutionTime
    @Transactional
    public void deleteAttachment(Long attachmentId, Long actorUserId) {
        HealthRecordAttachment attachment = healthRecordAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("첨부파일을 찾을 수 없습니다: " + attachmentId));
        assertHealthRecordOwnedByUser(attachment.getHealthRecord(), actorUserId);
        attachment.deactivate();
        healthRecordAttachmentRepository.save(attachment);
    }

    // ===== 차트 및 시각화 =====

    // ===== 시스템 관리 =====

    private void assertHealthRecordOwnedByUser(HealthRecord record, Long actorUserId) {
        if (actorUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "인증이 필요합니다.");
        }
        User owner = record.getUser();
        if (owner == null || owner.getId() == null || !owner.getId().equals(actorUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "해당 건강 기록에 접근할 권한이 없습니다.");
        }
    }

    /**
     * 자녀 소유권 확인. 판단은 {@link ChildDirectory} 한 곳에서만 한다.
     *
     * <p>전에는 여기서 직접 비교하고 403 을 던졌다. 같은 질문에 {@code /children/…} 은 404 를
     * 주고 있었는데, 403 은 <b>그 자녀가 존재한다는 사실을 알려준다</b> — ID 를 훑으면 "이 번호는
     * 누군가의 자녀" 목록을 만들 수 있다. 게다가 그 403 이 아래 {@code catch (Exception e)} 에
     * 걸려 500 으로 덮이는 경로도 있었다(접종·검진 일정 조회).
     *
     * <p>이미 읽어 둔 엔티티를 넘겨도 같은 트랜잭션 안에서는 1차 캐시에 올라와 있어 추가 쿼리가
     * 나가지 않는다.
     */
    private void assertChildOwnedByUser(Child child, Long actorUserId) {
        assertChildOwnedByUserId(child.getId(), actorUserId);
    }

    private void assertChildOwnedByUserId(Long childId, Long actorUserId) {
        childDirectory.requireOwnedChild(childId, actorUserId);
    }

    // ===== Helper Methods =====

    // Child Entity를 DTO로 변환


    // HealthRecord 엔티티를 HealthRecordResponse DTO로 변환

    // HealthRecord 매핑은 HealthRecordMapper 사용
    
    // ===== Validation Helper Methods =====

    // 요청 객체 검증
    private void validateRequest(HealthCreateHealthRecordRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "요청 정보가 없습니다.");
        }
        if (!StringUtils.hasText(request.getChildId())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "아동 ID가 필요합니다.");
        }
    }

    // 업데이트 요청 객체 검증
    private void validateUpdateRequest(HealthUpdateHealthRecordRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "요청 정보가 없습니다.");
        }
    }

    // 기록 ID 검증
    private void validateRecordId(Long recordId) {
        if (recordId == null || recordId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_RECORD_ID, 
                    ErrorCode.INVALID_RECORD_ID.getMessage() + ": " + recordId);
        }
    }

    // 아동 ID 검증
    private void validateChildId(Long childId) {
        if (childId == null || childId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_CHILD_ID, 
                    ErrorCode.INVALID_CHILD_ID.getMessage() + ": " + childId);
        }
    }

    // 날짜 범위 검증
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE.getMessage());
        }
    }

    private HealthRecordAttachmentResponse toAttachmentResponse(HealthRecordAttachment attachment) {
        return HealthRecordAttachmentResponse.builder()
                .attachmentId(attachment.getId())
                .recordId(attachment.getHealthRecord().getId())
                .fileUrl(attachment.getFileUrl())
                .fileName(attachment.getFileName())
                .fileType(attachment.getFileType())
                .fileSize(attachment.getFileSize())
                .description(attachment.getDescription())
                .displayOrder(attachment.getDisplayOrder())
                .createdAt(attachment.getCreatedAt())
                .build();
    }

}
