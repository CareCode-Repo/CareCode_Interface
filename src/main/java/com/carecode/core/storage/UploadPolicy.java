package com.carecode.core.storage;

import com.carecode.core.exception.BusinessException;
import com.carecode.core.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 업로드를 받아들일지 판단하고, 받아들인 바이트와 저장 키를 돌려준다.
 *
 * <p>이 검사들은 한때 {@link LocalFileStorageService} 안에 있었다. 저장소 구현이 하나뿐일 때는
 * 문제가 없었지만, S3·MinIO 구현을 더하면 <b>새 구현이 검사를 건너뛸 수 있다.</b> 크기·확장자·
 * Content-Type·시그니처·악성코드 검사는 "어디에 저장하는가" 와 무관한 규칙이므로 여기로 모았다.
 *
 * <p>구현체는 {@link #accept}가 돌려준 {@link AcceptedUpload#content()} 를 그대로 저장해야 한다.
 * 스트림을 다시 열면 검사한 것과 다른 내용이 저장될 여지가 생긴다.
 */
@Slf4j
@Component
public class UploadPolicy {

    /** 업로드를 허용할 확장자. 실행 가능한 파일이 올라가는 것을 막는다. */
    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "gif", "webp", "heic", "pdf");

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/gif", "image/webp", "image/heic", "application/pdf");

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final long maxFileSize;
    private final FileScanner fileScanner;

    public UploadPolicy(
            @Value("${app.storage.max-file-size-bytes:10485760}") long maxFileSize,
            FileScanner fileScanner) {
        this.maxFileSize = maxFileSize;
        this.fileScanner = fileScanner;
    }

    /**
     * 검사를 통과한 업로드.
     *
     * @param key             저장 키 ({@code <directory>/yyyy/MM/dd/<uuid>.<ext>})
     * @param content         검사한 바이트. 이 값을 그대로 저장한다
     * @param originalFilename 사용자에게 보여줄 원래 이름
     * @param contentType     클라이언트가 보낸 Content-Type
     */
    public record AcceptedUpload(String key, byte[] content, String originalFilename, String contentType) {

        public long size() {
            return content.length;
        }
    }

    public AcceptedUpload accept(MultipartFile file, String directory) {
        validate(file);
        byte[] content = readContent(file);
        String extension = extractExtension(file.getOriginalFilename());
        verifySignature(extension, content);
        scan(content);

        // 원본 파일명을 그대로 쓰면 경로 조작(../)과 파일명 충돌 위험이 있으므로 UUID 로 대체한다.
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        String key = sanitizeDirectory(directory) + "/" + LocalDate.now().format(DATE_PATH) + "/" + storedName;

        return new AcceptedUpload(
                key,
                content,
                StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : storedName),
                file.getContentType());
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "업로드할 파일이 비어 있습니다.");
        }
        if (file.getSize() > maxFileSize) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "파일 크기는 " + (maxFileSize / 1024 / 1024) + "MB를 넘을 수 없습니다.");
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "허용되지 않는 파일 형식입니다: " + extension);
        }

        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "허용되지 않는 파일 형식입니다: " + contentType);
        }
    }

    /** 최대 크기(기본 10MB)를 이미 확인했으므로 메모리에 올려도 된다. */
    private byte[] readContent(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "업로드한 파일을 읽을 수 없습니다.");
        }
    }

    /**
     * 확장자와 Content-Type 은 클라이언트가 정하는 값이다. 실제 바이트가 그 형식인지 본다.
     * (HTML·SVG 를 .png 로 올려도 앞의 두 검사는 통과한다.)
     */
    private void verifySignature(String extension, byte[] content) {
        byte[] header = Arrays.copyOf(content, Math.min(content.length, FileSignatureValidator.HEADER_LENGTH));
        if (!FileSignatureValidator.matches(extension, header)) {
            log.warn("확장자와 내용이 다른 업로드를 거부했습니다 - extension={}", extension);
            throw new BusinessException(ErrorCode.FILE_CONTENT_MISMATCH,
                    "파일 내용이 ." + extension + " 형식이 아닙니다.");
        }
    }

    /** 검사기에 닿지 못하면 받지 않는다. 검사를 켠 환경에서 조용히 통과시키면 켠 의미가 없다. */
    private void scan(byte[] content) {
        FileScanner.ScanResult result;
        try {
            result = fileScanner.scan(content);
        } catch (FileScanner.ScannerUnavailableException e) {
            log.error("파일 검사 실패로 업로드를 거부했습니다", e);
            throw new BusinessException(ErrorCode.FILE_SCAN_UNAVAILABLE, ErrorCode.FILE_SCAN_UNAVAILABLE.getMessage());
        }
        if (!result.clean()) {
            throw new BusinessException(ErrorCode.FILE_REJECTED_BY_SCAN, ErrorCode.FILE_REJECTED_BY_SCAN.getMessage());
        }
    }

    private String extractExtension(String filename) {
        if (filename == null) {
            return "";
        }
        String cleaned = StringUtils.cleanPath(filename);
        int dot = cleaned.lastIndexOf('.');
        return dot >= 0 ? cleaned.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    /** 디렉터리 이름에 경로 구분자나 상위 이동이 들어오지 못하게 한다. */
    private String sanitizeDirectory(String directory) {
        if (directory == null || directory.isBlank()) {
            return "misc";
        }
        return directory.replaceAll("[^a-zA-Z0-9_-]", "");
    }
}
