package com.carecode.core.storage;

import com.carecode.core.exception.BusinessException;
import com.carecode.core.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** 로컬 디스크 기반 파일 저장소. 단일 인스턴스 배포를 전제로 한다. */
@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    /** 업로드를 허용할 확장자. 실행 가능한 파일이 올라가는 것을 막는다. */
    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "gif", "webp", "heic", "pdf");

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/gif", "image/webp", "image/heic", "application/pdf");

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final Path rootLocation;
    private final String publicBaseUrl;
    private final long maxFileSize;

    public LocalFileStorageService(
            @Value("${app.storage.local.root:./uploads}") String root,
            @Value("${app.storage.public-base-url:/files}") String publicBaseUrl,
            @Value("${app.storage.max-file-size-bytes:10485760}") long maxFileSize) {
        this.rootLocation = Paths.get(root).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
        this.maxFileSize = maxFileSize;

        try {
            Files.createDirectories(rootLocation);
            log.info("파일 저장소 초기화: {}", rootLocation);
        } catch (IOException e) {
            throw new IllegalStateException("파일 저장 디렉터리를 만들 수 없습니다: " + rootLocation, e);
        }
    }

    @Override
    public StoredFile store(MultipartFile file, String directory) {
        validate(file);

        String extension = extractExtension(file.getOriginalFilename());
        // 원본 파일명을 그대로 쓰면 경로 조작(../)과 파일명 충돌 위험이 있으므로 UUID 로 대체한다.
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        String relativeDir = sanitizeDirectory(directory) + "/" + LocalDate.now().format(DATE_PATH);
        String key = relativeDir + "/" + storedName;

        Path targetDir = rootLocation.resolve(relativeDir).normalize();
        if (!targetDir.startsWith(rootLocation)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "잘못된 저장 경로입니다.");
        }

        try {
            Files.createDirectories(targetDir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, targetDir.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("파일 저장 실패 - key={}", key, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "파일 저장에 실패했습니다.");
        }

        return StoredFile.builder()
                .key(key)
                .url(publicBaseUrl + "/" + key)
                .originalFilename(StringUtils.cleanPath(
                        file.getOriginalFilename() != null ? file.getOriginalFilename() : storedName))
                .contentType(file.getContentType())
                .size(file.getSize())
                .build();
    }

    @Override
    public void delete(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            Path target = rootLocation.resolve(key).normalize();
            if (!target.startsWith(rootLocation)) {
                log.warn("저장소 밖 경로 삭제 시도를 차단했습니다: {}", key);
                return;
            }
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.error("파일 삭제 실패 - key={}", key, e);
        }
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
