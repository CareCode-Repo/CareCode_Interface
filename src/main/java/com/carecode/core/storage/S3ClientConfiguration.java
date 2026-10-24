package com.carecode.core.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * S3 호환 클라이언트.
 *
 * <p>엔드포인트와 path-style 을 설정으로 받기 때문에 <b>구현 하나로 S3·MinIO·R2 를 모두
 * 쓴다.</b> 어디에 저장하는지는 배포 환경이 정한다.
 *
 * <table>
 *   <tr><th>용도</th><th>설정</th></tr>
 *   <tr><td>AWS S3</td><td>엔드포인트 비움 + 리전 + IAM 역할(자격증명 생략)</td></tr>
 *   <tr><td>MinIO</td><td>엔드포인트 지정 + path-style 켜기 + 액세스 키/시크릿</td></tr>
 * </table>
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3ClientConfiguration {

    /**
     * @param endpoint    MinIO 등 S3 호환 서버 주소. 비우면 AWS S3 로 간다
     * @param region      AWS 리전. MinIO 는 아무 값이나 받지만 SDK 가 요구해서 넣는다
     * @param accessKey   비우면 {@link DefaultCredentialsProvider} 를 쓴다(EC2 IAM 역할·환경변수·프로파일)
     * @param pathStyle   {@code true} 면 {@code http://host/bucket/key}. MinIO 는 가상 호스트
     *                    방식(버킷을 서브도메인으로)을 쓰려면 DNS 가 필요해서 보통 켠다
     */
    @Bean
    public S3Client s3Client(
            @Value("${app.storage.s3.endpoint:}") String endpoint,
            @Value("${app.storage.s3.region:ap-northeast-2}") String region,
            @Value("${app.storage.s3.access-key:}") String accessKey,
            @Value("${app.storage.s3.secret-key:}") String secretKey,
            @Value("${app.storage.s3.path-style:false}") boolean pathStyle) {

        var builder = S3Client.builder()
                .region(Region.of(region))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(pathStyle)
                        .build());

        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        // 키를 주지 않으면 SDK 의 기본 탐색을 쓴다. 운영 S3 는 EC2 IAM 역할을 쓰는 쪽이
        // 안전하다 — 자격증명이 환경변수나 .env 에 남지 않는다.
        if (!accessKey.isBlank() && !secretKey.isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        log.info("S3 호환 클라이언트 초기화: endpoint={}, region={}, pathStyle={}, 정적 자격증명={}",
                endpoint.isBlank() ? "(AWS 기본)" : endpoint, region, pathStyle, !accessKey.isBlank());

        return builder.build();
    }
}
