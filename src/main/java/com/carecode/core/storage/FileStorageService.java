package com.carecode.core.storage;

import org.springframework.web.multipart.MultipartFile;

/** 파일 저장소 추상화. 구현체를 바꾸면 로컬 디스크 ↔ S3 전환이 가능하도록 도메인 코드는 이 인터페이스에만 의존한다. */
public interface FileStorageService {

    /** 파일을 저장한다. */
    StoredFile store(MultipartFile file, String directory);

    /** 저장된 파일을 삭제한다. 없는 키를 지워도 예외를 던지지 않는다. */
    void delete(String key);
}
