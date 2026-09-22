package com.carecode.core.storage;

/**
 * 업로드 파일 악성코드 검사 지점.
 *
 * <p>저장 직전에 한 번 부른다. 기본 구현({@link NoOpFileScanner})은 검사하지 않고 통과시키며,
 * {@code app.storage.scan.enabled=true} 이면 ClamAV(clamd) 로 검사한다.
 */
public interface FileScanner {

    ScanResult scan(byte[] content);

    record ScanResult(boolean clean, String threat) {
        public static ScanResult ok() {
            return new ScanResult(true, null);
        }

        public static ScanResult infected(String threat) {
            return new ScanResult(false, threat);
        }
    }

    /** 검사기에 닿을 수 없을 때. 업로드는 받지 않는다(fail-closed). */
    class ScannerUnavailableException extends RuntimeException {
        public ScannerUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
