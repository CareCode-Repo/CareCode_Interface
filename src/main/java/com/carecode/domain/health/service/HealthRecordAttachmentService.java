package com.carecode.domain.health.service;

import com.carecode.core.exception.HealthRecordNotFoundException;
import com.carecode.core.exception.ResourceNotFoundException;
import com.carecode.domain.user.app.CurrentUserFacade;
import com.carecode.core.storage.FileStorageService;
import com.carecode.domain.health.dto.response.AttachmentDownload;
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

    /**
     * 첨부파일 본문.
     *
     * `/files/**` 로 바로 열 수 없다 — 같은 저장소를 정적으로 공개하면 주소만 아는 사람이
     * 남의 진료 기록을 볼 수 있다. 여기서 본인 기록인지 확인한 뒤에만 내려준다.
     */
    public AttachmentDownload download(Long recordId, Long attachmentId) {
        // 소유권 확인이 먼저다. 남의 기록이면 존재 여부를 숨기려 404 로 응답한다.
        requireOwnedRecord(recordId);

        // 없는 첨부와 "남의 기록에 달린 첨부" 는 같은 404 여야 한다.
        // IllegalArgumentException 을 쓰면 전역 핸들러가 400 으로 바꾸는데, 그러면
        // 소유권 실패(404)와 응답이 갈려 "그 id 는 존재한다" 는 사실이 새어 나간다.
        HealthRecordAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("첨부파일을 찾을 수 없습니다: " + attachmentId));

        // 다른 기록의 첨부 id 를 끼워 넣어 남의 파일을 받아가지 못하게 한다.
        if (attachment.getHealthRecord() == null
                || !attachment.getHealthRecord().getId().equals(recordId)) {
            throw new ResourceNotFoundException("첨부파일을 찾을 수 없습니다: " + attachmentId);
        }

        return AttachmentDownload.builder()
                .resource(fileStorageService.load(fileStorageService.toKey(attachment.getFileUrl())))
                .fileName(attachment.getFileName())
                .contentType(attachment.getFileType())
                .build();
    }

    @Transactional
    public void delete(Long recordId, Long attachmentId) {
        requireOwnedRecord(recordId);

        HealthRecordAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("첨부파일을 찾을 수 없습니다: " + attachmentId));

        if (attachment.getHealthRecord() == null
                || !attachment.getHealthRecord().getId().equals(recordId)) {
            throw new ResourceNotFoundException("첨부파일을 찾을 수 없습니다: " + attachmentId);
        }

        attachmentRepository.delete(attachment);

        // 저장된 파일도 지운다. FileStorageService.delete 는 처음부터 있었지만 부르는 곳이
        // 없어서, 첨부를 삭제해도 디스크에 파일이 남았다. 남은 파일은 조회할 길이 없으니 당장
        // 새어 나가지는 않지만, 진단서·처방전 같은 민감정보가 "지웠다" 고 생각한 뒤에도 서버에
        // 남는다. 파기 의무를 생각하면 그게 문제다.
        //
        // 행을 먼저 지우는 순서다. 파일 삭제가 실패해도 사용자에게는 삭제된 것으로 보이고,
        // 남은 파일은 가리키는 행이 없어 접근 경로가 없다. 반대 순서라면 파일은 사라졌는데
        // 행이 남아 다운로드가 깨진다.
        fileStorageService.delete(fileStorageService.toKey(attachment.getFileUrl()));
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
