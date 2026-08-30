package com.carecode.domain.health.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.springframework.core.io.Resource;

/**
 * 첨부파일 다운로드 결과.
 *
 * 건강기록 첨부는 민감정보라 정적 경로로 공개할 수 없다. 소유권을 확인한 뒤
 * 서버가 직접 내려주기 위해 파일 본문과 표시용 메타데이터를 함께 담는다.
 */
@Getter
@Builder
public class AttachmentDownload {
    private final Resource resource;
    private final String fileName;
    private final String contentType;
}
