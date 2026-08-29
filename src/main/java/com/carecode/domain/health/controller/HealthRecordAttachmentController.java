package com.carecode.domain.health.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.domain.health.dto.response.AttachmentDownload;
import com.carecode.domain.health.dto.response.AttachmentResponse;
import com.carecode.domain.health.service.HealthRecordAttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/** 건강기록 첨부파일 업로드 API. 기존 POST /health/records/{id/attachments} 는 이미 업로드된 파일의 URL·메타데이터를 JSON 으로 */
@RestController
@RequestMapping("/health/records/{recordId}/attachments")
@RequiredArgsConstructor
@Tag(name = "건강기록 첨부 업로드", description = "예방접종 수첩, 진료 기록 사진 등 파일 업로드 API")
public class HealthRecordAttachmentController {

    private final HealthRecordAttachmentService attachmentService;

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @LogExecutionTime
    @Operation(summary = "첨부파일 업로드",
            description = "이미지 또는 PDF 를 업로드하고 건강기록에 연결합니다")
    public ResponseEntity<AttachmentResponse> upload(
            @PathVariable Long recordId,
            @Parameter(description = "업로드할 파일", required = true) @RequestPart("file") MultipartFile file,
            @Parameter(description = "설명") @RequestParam(required = false) String description) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attachmentService.upload(recordId, file, description));
    }

    /**
     * 첨부파일 내려받기.
     *
     * 업로드 저장소(`/files/**`)를 정적으로 공개하지 않는다 — 주소만 아는 사람이 남의
     * 진료 기록을 볼 수 있기 때문이다. 본인 기록인지 확인한 뒤 서버가 직접 내려준다.
     */
    @GetMapping("/{attachmentId}/download")
    @LogExecutionTime
    @Operation(summary = "첨부파일 내려받기", description = "본인 건강기록의 첨부파일만 받을 수 있습니다")
    public ResponseEntity<Resource> download(
            @PathVariable Long recordId,
            @Parameter(description = "첨부파일 ID", required = true) @PathVariable Long attachmentId) {
        AttachmentDownload download = attachmentService.download(recordId, attachmentId);

        // 브라우저가 파일명을 그대로 쓸 수 있게 RFC 5987 로 인코딩한다(한글 파일명 대응).
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.getFileName() != null ? download.getFileName() : "attachment",
                        StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(download.getContentType() != null
                        ? MediaType.parseMediaType(download.getContentType())
                        : MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(download.getResource());
    }
}
