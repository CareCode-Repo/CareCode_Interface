package com.carecode.core.storage;

import lombok.Builder;
import lombok.Getter;

/**
 * 저장된 파일의 메타데이터.
 */
@Getter
@Builder
public class StoredFile {

    /** 저장소 내 키(경로). 삭제·조회 시 사용한다. */
    private final String key;

    /** 클라이언트가 접근할 수 있는 URL. */
    private final String url;

    /** 업로드 당시 원본 파일명. */
    private final String originalFilename;

    private final String contentType;

    private final long size;
}
