package com.carecode.core.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/** 파일 저장소 추상화. 구현체를 바꾸면 로컬 디스크 ↔ S3 전환이 가능하도록 도메인 코드는 이 인터페이스에만 의존한다. */
public interface FileStorageService {

    /** 파일을 저장한다. */
    StoredFile store(MultipartFile file, String directory);

    /**
     * 저장된 파일을 읽는다.
     *
     * 민감한 파일(건강기록 첨부 등)은 정적 경로로 공개할 수 없어, 인증을 거친 뒤
     * 서버가 직접 내려줘야 한다. 없는 키는 예외를 던진다.
     */
    Resource load(String key);

    /** 공개 URL(`/files/...`)에서 저장 키를 되돌린다. 이미 키면 그대로 돌려준다. */
    String toKey(String publicUrl);

    /** 저장된 파일을 삭제한다. 없는 키를 지워도 예외를 던지지 않는다. */
    void delete(String key);
}
