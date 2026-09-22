package com.carecode.core.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 검사하지 않는 기본 구현. 로컬·테스트용이다.
 * 운영에서 검사를 켜려면 clamd 를 띄우고 {@code STORAGE_SCAN_ENABLED=true} 로 둔다.
 */
@Component
@ConditionalOnProperty(name = "app.storage.scan.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpFileScanner implements FileScanner {

    @Override
    public ScanResult scan(byte[] content) {
        return ScanResult.ok();
    }
}
