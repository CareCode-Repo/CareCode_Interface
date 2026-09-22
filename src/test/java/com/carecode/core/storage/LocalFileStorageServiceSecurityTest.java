package com.carecode.core.storage;

import com.carecode.core.exception.BusinessException;
import com.carecode.core.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("업로드 보안 — 내용 확인과 악성코드 검사")
class LocalFileStorageServiceSecurityTest {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};

    @TempDir Path root;

    private LocalFileStorageService storage(FileScanner scanner) {
        return new LocalFileStorageService(root.toString(), "/files", 10 * 1024 * 1024, scanner);
    }

    @Test
    @DisplayName("진짜 PNG 는 저장된다")
    void realPngIsStored() {
        StoredFile stored = storage(new NoOpFileScanner())
                .store(new MockMultipartFile("file", "x.png", "image/png", PNG), "health-records");

        assertThat(root.resolve(stored.getKey())).exists();
    }

    @Test
    @DisplayName("HTML 을 .png 로 올리면 거절하고 아무것도 저장하지 않는다")
    void htmlDisguisedAsPngIsRejected() throws Exception {
        byte[] html = "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> storage(new NoOpFileScanner())
                .store(new MockMultipartFile("file", "x.png", "image/png", html), "health-records"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_CONTENT_MISMATCH);
        assertThat(storedFileCount()).isZero();
    }

    @Test
    @DisplayName("검사기가 악성으로 판정하면 저장하지 않는다")
    void infectedIsRejected() throws Exception {
        FileScanner infected = content -> FileScanner.ScanResult.infected("Eicar-Test-Signature");

        assertThatThrownBy(() -> storage(infected)
                .store(new MockMultipartFile("file", "x.png", "image/png", PNG), "health-records"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_REJECTED_BY_SCAN);
        assertThat(storedFileCount()).isZero();
    }

    @Test
    @DisplayName("검사기에 닿지 못하면 받지 않는다 (503)")
    void scannerDownFailsClosed() {
        FileScanner down = content -> {
            throw new FileScanner.ScannerUnavailableException("down", null);
        };

        assertThatThrownBy(() -> storage(down)
                .store(new MockMultipartFile("file", "x.png", "image/png", PNG), "health-records"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getHttpStatus().value())
                .isEqualTo(503);
    }

    private long storedFileCount() throws Exception {
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(Files::isRegularFile).count();
        }
    }
}
