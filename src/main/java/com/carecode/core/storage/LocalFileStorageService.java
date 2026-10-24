package com.carecode.core.storage;

import com.carecode.core.exception.BusinessException;
import com.carecode.core.exception.ErrorCode;
import com.carecode.core.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 로컬 디스크 기반 파일 저장소. 단일 인스턴스 배포를 전제로 한다.
 *
 * <p>검사(크기·확장자·Content-Type·시그니처·악성코드)는 {@link UploadPolicy} 가 한다.
 * 저장소 구현이 늘어도 그 검사를 건너뛸 수 없게 하기 위해서다.
 *
 * <p>여러 인스턴스로 늘리거나 서명 URL 이 필요하면 {@code app.storage.type=s3} 로 바꾼다
 * ({@link S3FileStorageService} — S3·MinIO 둘 다 붙는다).
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private final Path rootLocation;
    private final String publicBaseUrl;
    private final UploadPolicy uploadPolicy;

    public LocalFileStorageService(
            @Value("${app.storage.local.root:./uploads}") String root,
            @Value("${app.storage.public-base-url:/files}") String publicBaseUrl,
            UploadPolicy uploadPolicy) {
        this.uploadPolicy = uploadPolicy;
        this.rootLocation = Paths.get(root).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;

        try {
            Files.createDirectories(rootLocation);
            log.info("파일 저장소 초기화(로컬): {}", rootLocation);
        } catch (IOException e) {
            throw new IllegalStateException("파일 저장 디렉터리를 만들 수 없습니다: " + rootLocation, e);
        }
    }

    @Override
    public StoredFile store(MultipartFile file, String directory) {
        UploadPolicy.AcceptedUpload accepted = uploadPolicy.accept(file, directory);

        Path target = rootLocation.resolve(accepted.key()).normalize();
        if (!target.startsWith(rootLocation)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "잘못된 저장 경로입니다.");
        }

        try {
            Files.createDirectories(target.getParent());
            // 검사한 바이트를 그대로 쓴다. 스트림을 다시 열면 검사한 것과 다른 내용이 저장될 여지가 생긴다.
            Files.write(target, accepted.content());
        } catch (IOException e) {
            log.error("파일 저장 실패 - key={}", accepted.key(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "파일 저장에 실패했습니다.");
        }

        return StoredFile.builder()
                .key(accepted.key())
                .url(publicBaseUrl + "/" + accepted.key())
                .originalFilename(accepted.originalFilename())
                .contentType(accepted.contentType())
                .size(accepted.size())
                .build();
    }

    @Override
    public Resource load(String key) {
        // BusinessException 은 전역 핸들러가 ErrorCode 와 무관하게 400 으로 바꾼다.
        // 없는 파일은 404 여야 하므로 ResourceNotFoundException 을 쓴다.
        if (key == null || key.isBlank()) {
            throw new ResourceNotFoundException("파일을 찾을 수 없습니다.");
        }

        Path target = rootLocation.resolve(key).normalize();
        // 저장소 밖을 가리키는 키는 읽지 않는다. 삭제와 같은 방어다.
        if (!target.startsWith(rootLocation) || !Files.isReadable(target)) {
            log.warn("읽을 수 없는 파일 요청: {}", key);
            throw new ResourceNotFoundException("파일을 찾을 수 없습니다.");
        }

        return new FileSystemResource(target);
    }

    @Override
    public String toKey(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) {
            return publicUrl;
        }

        String prefix = publicBaseUrl + "/";
        return publicUrl.startsWith(prefix) ? publicUrl.substring(prefix.length()) : publicUrl;
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
}
