package com.carecode.core.storage;

import com.carecode.core.exception.BusinessException;
import com.carecode.core.exception.ErrorCode;
import com.carecode.core.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * S3 호환 오브젝트 스토리지 저장소. <b>AWS S3·MinIO·R2 에 같은 코드가 붙는다.</b>
 *
 * <p>엔드포인트를 설정으로 받고 path-style 접근을 켜면 MinIO 가 그대로 동작한다. 그래서 구현을
 * 하나만 두고, 어디에 저장하는지는 환경변수로 정한다 — 로컬·CI 는 MinIO 컨테이너, 운영은 S3 든
 * 자체 MinIO 든 바꿔 끼운다.
 *
 * <p>검사(크기·확장자·Content-Type·시그니처·악성코드)는 {@link UploadPolicy} 가 한다.
 * 저장 위치가 바뀌어도 받아들이는 기준은 같아야 한다.
 *
 * <p><b>왜 공개 URL 을 돌려주지 않는가.</b> 건강기록 첨부는 민감정보라 주소만 알면 열리는 상태로
 * 둘 수 없다. 그래서 {@link #store} 가 돌려주는 {@code url} 은 외부에서 바로 열 수 있는 주소가
 * 아니라 <b>저장 키</b>이고, 다운로드는 소유권을 확인하는 애플리케이션 경로를 거친다
 * ({@code GET /health/records/{recordId}/attachments/{id}/download}). 로컬 구현이
 * {@code /files/...} 를 돌려주던 것과 같은 자리이며, 그쪽도 정적 서빙을 프로필 이미지로만
 * 좁혀 둔 상태다.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3FileStorageService implements FileStorageService {

    private final S3Client s3Client;
    private final String bucket;
    private final UploadPolicy uploadPolicy;

    public S3FileStorageService(
            S3Client s3Client,
            @Value("${app.storage.s3.bucket}") String bucket,
            UploadPolicy uploadPolicy) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.uploadPolicy = uploadPolicy;
        log.info("파일 저장소 초기화(S3 호환): bucket={}", bucket);
    }

    @Override
    public StoredFile store(MultipartFile file, String directory) {
        UploadPolicy.AcceptedUpload accepted = uploadPolicy.accept(file, directory);

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(accepted.key())
                            .contentType(accepted.contentType())
                            .build(),
                    // 검사한 바이트를 그대로 올린다. 스트림을 다시 열면 검사한 것과 다른 내용이 저장될 수 있다.
                    RequestBody.fromBytes(accepted.content()));
        } catch (S3Exception e) {
            log.error("오브젝트 저장 실패 - key={}", accepted.key(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "파일 저장에 실패했습니다.");
        }

        return StoredFile.builder()
                .key(accepted.key())
                .url(accepted.key())
                .originalFilename(accepted.originalFilename())
                .contentType(accepted.contentType())
                .size(accepted.size())
                .build();
    }

    @Override
    public Resource load(String key) {
        if (key == null || key.isBlank()) {
            throw new ResourceNotFoundException("파일을 찾을 수 없습니다.");
        }

        try {
            ResponseBytes<GetObjectResponse> object = s3Client.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(key).build());
            return new ByteArrayResource(object.asByteArray());
        } catch (NoSuchKeyException e) {
            log.warn("없는 오브젝트 요청: {}", key);
            throw new ResourceNotFoundException("파일을 찾을 수 없습니다.");
        } catch (S3Exception e) {
            log.error("오브젝트 읽기 실패 - key={}", key, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "파일을 읽을 수 없습니다.");
        }
    }

    /**
     * 저장 키를 돌려준다.
     *
     * <p>이 구현은 {@link #store} 에서 {@code url} 에 키를 그대로 담으므로 변환할 것이 없다.
     * 로컬 구현에서 S3 로 바꾸면 <b>예전에 저장된 행은 {@code /files/...} 를 들고 있다.</b>
     * 그 접두사를 떼어 주지 않으면 이전 첨부를 영영 읽지 못하므로 함께 처리한다.
     */
    @Override
    public String toKey(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) {
            return publicUrl;
        }
        return publicUrl.startsWith("/files/") ? publicUrl.substring("/files/".length()) : publicUrl;
    }

    @Override
    public void delete(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception e) {
            // 삭제 실패로 요청을 깨뜨리지 않는다. 행은 이미 지워졌고, 남은 오브젝트는 가리키는
            // 것이 없어 접근 경로가 없다. 로컬 구현과 같은 판단이다.
            log.error("오브젝트 삭제 실패 - key={}", key, e);
        }
    }
}
