package com.carecode.core.storage;

import com.carecode.core.exception.BusinessException;
import com.carecode.core.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * S3 경로를 <b>실제 서버에 대고</b> 검증한다. MinIO 컨테이너를 쓰므로 AWS 자격증명이 필요 없다.
 *
 * <p>목으로 검증하면 "SDK 를 올바르게 불렀는가" 만 보게 된다. path-style 접근, 없는 키의 예외
 * 종류, 버킷 동작처럼 <b>실제로 틀리는 것들</b>은 서버가 있어야 드러난다.
 *
 * <p>같은 구현이 AWS S3 에도 붙는다 — 엔드포인트와 path-style 만 설정으로 다르다.
 */
@Testcontainers
@DisplayName("S3 호환 저장소 (MinIO)")
class S3FileStorageServiceTest {

    private static final String BUCKET = "carecode-test";

    /**
     * MinIO 이미지. {@code minio/minio} 가 기본이지만 받을 수 없는 환경이 있어 대체 이미지를
     * 쓸 수 있게 열어 둔다({@code -DminioImage=...} 또는 환경변수 {@code MINIO_IMAGE}).
     * 어느 쪽이든 S3 API 는 같다.
     */
    private static final org.testcontainers.utility.DockerImageName MINIO_IMAGE =
            org.testcontainers.utility.DockerImageName
                    .parse(System.getProperty("minioImage",
                            System.getenv().getOrDefault("MINIO_IMAGE", "minio/minio:RELEASE.2024-08-29T01-40-52Z")))
                    .asCompatibleSubstituteFor("minio/minio");

    /**
     * 테스트마다 컨테이너를 띄우면 느리다. 클래스 단위로 한 번 띄우고 키를 달리 쓴다.
     * (저장 키에 UUID 가 들어가므로 테스트 간 충돌이 없다.)
     */
    private static MinIOContainer minio;
    private static S3Client s3Client;
    private static S3FileStorageService storage;

    /** PNG 시그니처. 시그니처 검사를 통과해야 저장까지 간다. */
    private static final byte[] PNG_HEADER = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A
    };

    @BeforeAll
    static void startMinio() {
        try {
            minio = new MinIOContainer(MINIO_IMAGE)
                    // 대체 이미지는 비권한 사용자로 돌아 /data 에 쓸 수 없다. 테스트 컨테이너라 root 로 띄운다.
                    .withCreateContainerCmdModifier(cmd -> cmd.withUser("0"));
            minio.start();
        } catch (RuntimeException e) {
            // 이미지를 받을 수 없는 환경(사내 레지스트리 제약 등)에서 스위트 전체를 깨뜨리지 않는다.
            // CI 는 공식 이미지를 받을 수 있으므로 거기서 실제로 검증된다.
            minio = null;
            Assumptions.abort("MinIO 이미지를 준비할 수 없어 건너뜁니다 (" + MINIO_IMAGE + "): " + e.getMessage());
        }

        s3Client = S3Client.builder()
                .endpointOverride(URI.create(minio.getS3URL()))
                .region(Region.of("us-east-1"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(minio.getUserName(), minio.getPassword())))
                // MinIO 는 버킷을 서브도메인으로 쓰려면 DNS 가 필요하다. 켜지 않으면 연결이 실패한다.
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();

        s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());

        // 검사는 UploadPolicy 가 한다. 스캐너는 통과시키는 구현을 쓴다(검사 자체는 그쪽 테스트가 본다).
        storage = new S3FileStorageService(s3Client, BUCKET,
                new UploadPolicy(10 * 1024 * 1024, new NoOpFileScanner()));
    }

    @AfterAll
    static void stopMinio() {
        if (s3Client != null) {
            s3Client.close();
        }
        if (minio != null) {
            minio.stop();
        }
    }

    private MockMultipartFile pngFile(String name) {
        byte[] content = new byte[64];
        System.arraycopy(PNG_HEADER, 0, content, 0, PNG_HEADER.length);
        return new MockMultipartFile("file", name, "image/png", content);
    }

    @Test
    @DisplayName("올리고 같은 바이트를 다시 읽는다")
    void storeThenLoad() throws IOException {
        StoredFile stored = storage.store(pngFile("진단서.png"), "health-records");

        assertThat(stored.getKey()).startsWith("health-records/").endsWith(".png");
        assertThat(stored.getOriginalFilename()).isEqualTo("진단서.png");
        assertThat(stored.getSize()).isEqualTo(64);

        byte[] loaded = storage.load(stored.getKey()).getContentAsByteArray();
        assertThat(loaded).hasSize(64);
        assertThat(java.util.Arrays.copyOf(loaded, PNG_HEADER.length)).isEqualTo(PNG_HEADER);
    }

    /**
     * 저장 키에 원본 파일명이 들어가면 경로 조작과 충돌 위험이 있다. UUID 로 대체하는 규칙이
     * 저장소 구현을 바꿔도 유지되는지 본다.
     */
    @Test
    @DisplayName("원본 파일명은 키에 들어가지 않는다")
    void keyDoesNotContainOriginalName() {
        StoredFile stored = storage.store(pngFile("../../etc/passwd.png"), "health-records");

        assertThat(stored.getKey()).doesNotContain("..").doesNotContain("passwd");
    }

    @Test
    @DisplayName("없는 키는 404 로 끝낸다")
    void missingKeyIsNotFound() {
        assertThatThrownBy(() -> storage.load("health-records/2026/01/01/nope.png"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("삭제하면 다시 읽히지 않는다")
    void deleteRemovesObject() {
        StoredFile stored = storage.store(pngFile("처방전.png"), "health-records");
        storage.delete(stored.getKey());

        assertThatThrownBy(() -> storage.load(stored.getKey()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /** 없는 키를 지워도 예외를 던지지 않는다 — 인터페이스가 그렇게 약속한다. */
    @Test
    @DisplayName("없는 키 삭제는 조용히 넘어간다")
    void deletingMissingKeyIsSilent() {
        storage.delete("health-records/2026/01/01/nope.png");
    }

    /**
     * 저장 위치가 바뀌어도 받아들이는 기준은 같아야 한다. 검사를 {@link UploadPolicy} 로 뺀
     * 이유가 이것이다 — 새 구현이 검사를 건너뛸 수 없다.
     */
    @Test
    @DisplayName("확장자와 내용이 다른 파일은 S3 경로에서도 거부한다")
    void rejectsContentMismatch() {
        MockMultipartFile html = new MockMultipartFile(
                "file", "evil.png", "image/png", "<html>script</html>".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> storage.store(html, "health-records"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("허용하지 않는 확장자는 S3 경로에서도 거부한다")
    void rejectsDisallowedExtension() {
        MockMultipartFile exe = new MockMultipartFile(
                "file", "payload.exe", "application/octet-stream", new byte[] {0x4D, 0x5A});

        assertThatThrownBy(() -> storage.store(exe, "health-records"))
                .isInstanceOf(BusinessException.class);
    }

    /**
     * 로컬에서 S3 로 바꾸면 예전에 저장된 행은 {@code /files/...} 를 들고 있다. 그 접두사를
     * 떼어 주지 않으면 이전 첨부를 영영 읽지 못한다.
     */
    @Test
    @DisplayName("예전 /files/ URL 도 키로 되돌린다")
    void convertsLegacyPublicUrl() {
        String key = "health-records/2026/08/29/abc.png";

        assertThat(storage.toKey("/files/" + key)).isEqualTo(key);
        assertThat(storage.toKey(key)).isEqualTo(key);
    }

    /** 직접 올린 오브젝트도 읽는다 — 마이그레이션으로 밀어 넣은 파일이 그 경우다. */
    @Test
    @DisplayName("애플리케이션을 거치지 않고 올린 오브젝트도 읽는다")
    void readsExternallyUploadedObject() throws IOException {
        String key = "profile-images/migrated/avatar.png";
        s3Client.putObject(
                PutObjectRequest.builder().bucket(BUCKET).key(key).build(),
                RequestBody.fromBytes("migrated".getBytes(StandardCharsets.UTF_8)));

        assertThat(storage.load(key).getContentAsByteArray())
                .isEqualTo("migrated".getBytes(StandardCharsets.UTF_8));
    }
}
