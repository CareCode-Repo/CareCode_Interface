package com.carecode.domain.health.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.domain.health.dto.response.AttachmentResponse;
import com.carecode.domain.health.service.HealthRecordAttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 건강기록 첨부파일 업로드 API.
 *
 * <p>기존 {@code POST /health/records/{id}/attachments} 는 이미 업로드된 파일의
 * URL·메타데이터를 JSON 으로 받는 등록 API 다. 실제 바이너리를 올릴 경로가 없어
 * 첨부 기능을 쓸 수 없었으므로, 여기서 multipart 업로드만 추가한다.
 * 목록 조회·삭제는 기존 엔드포인트를 그대로 사용한다.
 */
@RestController
@RequestMapping("/health/records/{recordId}/attachments")
@RequiredArgsConstructor
@Tag(name = "건강기록 첨부 업로드", description = "예방접종 수첩, 진료 기록 사진 등 파일 업로드 API")
public class HealthRecordAttachmentController {

    private final HealthRecordAttachmentService attachmentService;

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @LogExecutionTime
    @Operation(summary = "첨부파일 업로드",
            description = "이미지 또는 PDF 를 업로드하고 건강기록에 연결합니다. (최대 10MB)")
    public ResponseEntity<AttachmentResponse> upload(
            @PathVariable Long recordId,
            @Parameter(description = "업로드할 파일", required = true) @RequestPart("file") MultipartFile file,
            @Parameter(description = "설명") @RequestParam(required = false) String description) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attachmentService.upload(recordId, file, description));
    }
}
