package com.carecode.domain.health.service;

import com.carecode.core.exception.HealthRecordNotFoundException;
import com.carecode.core.security.CurrentUserFacade;
import com.carecode.core.storage.FileStorageService;
import com.carecode.core.storage.StoredFile;
import com.carecode.domain.health.dto.response.AttachmentResponse;
import com.carecode.domain.health.entity.HealthRecord;
import com.carecode.domain.health.entity.HealthRecordAttachment;
import com.carecode.domain.health.repository.HealthRecordAttachmentRepository;
import com.carecode.domain.health.repository.HealthRecordRepository;
import com.carecode.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** 건강기록 첨부파일 관리. 첨부 엔티티와 테이블은 있었지만 업로드 경로가 없어 사용할 수 없던 기능을 연결한다. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HealthRecordAttachmentService {

    private static final String DIRECTORY = "health-records";

    private final HealthRecordRepository healthRecordRepository;
    private final HealthRecordAttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    private final CurrentUserFacade currentUserFacade;

    @Transactional
    public AttachmentResponse upload(Long recordId, MultipartFile file, String description) {
        HealthRecord record = requireOwnedRecord(recordId);

        StoredFile stored = fileStorageService.store(file, DIRECTORY);

        HealthRecordAttachment attachment = HealthRecordAttachment.builder()
                .healthRecord(record)
                .fileUrl(stored.getUrl())
                .fileName(stored.getOriginalFilename())
                .fileType(stored.getContentType())
                .fileSize(stored.getSize())
                .description(description)
                .displayOrder((int) attachmentRepository.countByHealthRecordId(recordId))
                .build();

        return AttachmentResponse.from(attachmentRepository.save(attachment));
    }

    public List<AttachmentResponse> list(Long recordId) {
        requireOwnedRecord(recordId);
        return attachmentRepository.findByHealthRecordIdOrderByDisplayOrderAsc(recordId).stream()
                .map(AttachmentResponse::from)
                .toList();
    }

    @Transactional
    public void delete(Long recordId, Long attachmentId) {
        requireOwnedRecord(recordId);

        HealthRecordAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("첨부파일을 찾을 수 없습니다: " + attachmentId));

        if (attachment.getHealthRecord() == null
                || !attachment.getHealthRecord().getId().equals(recordId)) {
            throw new IllegalArgumentException("첨부파일을 찾을 수 없습니다: " + attachmentId);
        }

        attachmentRepository.delete(attachment);
    }

    /** 건강기록 조회 + 소유권 검증. 건강기록은 민감정보이므로 본인 기록만 접근할 수 있어야 한다. */
    private HealthRecord requireOwnedRecord(Long recordId) {
        User currentUser = currentUserFacade.requireCurrentUser();
        HealthRecord record = healthRecordRepository.findById(recordId)
                .orElseThrow(() -> new HealthRecordNotFoundException("건강 기록을 찾을 수 없습니다: " + recordId));

        if (record.getUser() == null || !record.getUser().getId().equals(currentUser.getId())) {
            // 존재 여부를 숨기기 위해 동일한 404 로 응답한다.
            throw new HealthRecordNotFoundException("건강 기록을 찾을 수 없습니다: " + recordId);
        }
        return record;
    }
}
