package com.carecode.core.config;

import com.carecode.core.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/** 웹 계층 공통 설정. RateLimitInterceptor 는 @Component 로 빈 등록만 되어 있고 인터셉터 체인에는 연결되어 있지 않아 동작하지 않는 상태였다 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Value("${app.storage.local.root:./uploads}")
    private String storageRoot;

    @Value("${app.storage.public-base-url:/files}")
    private String publicBaseUrl;

    /** 정적으로 열어 두는 하위 디렉터리. 이 밖의 업로드물은 인증·소유권을 거치는 경로로만 나간다. */
    private static final String PUBLIC_SUBDIRECTORY = "profile-images";

    /**
     * 업로드된 파일 중 <b>프로필 이미지만</b> 정적 리소스로 서빙한다.
     *
     * <p>전에는 업로드 루트 전체({@code /files/**})를 서빙했다. 보안 설정이
     * {@code /files/profile-images/**} 만 공개로 두고 나머지는 인증을 요구했지만,
     * <b>인증만으로는 부족했다</b> — 인증을 거치는 다운로드 경로
     * ({@code GET /health/records/{recordId}/attachments/{id}/download})는 소유권까지 확인하는데,
     * 정적 경로는 로그인한 아무나 통과시켰다. 주소를 알게 된 다른 회원이 남의 진단서를 그대로
     * 받아갈 수 있었다. 파일명이 UUID 라 추측은 어렵지만, 추측이 유일한 방어라면 방어가 아니다.
     *
     * <p>프로필 이미지는 공개로 둔다. {@code <img src>} 로 불러가므로 인증 헤더를 붙일 수 없고,
     * 원래 화면에 노출되는 값이다.
     *
     * <p>S3 로 전환하면 이 매핑 대신 버킷의 공개 프리픽스를 쓰고, 첨부는 서명 URL 로 내려준다.
     * 그때도 <b>경계는 같다</b> — 첨부는 소유권을 확인한 뒤에만 주소가 나간다.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(storageRoot).resolve(PUBLIC_SUBDIRECTORY)
                .toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler(publicBaseUrl + "/" + PUBLIC_SUBDIRECTORY + "/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/actuator/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/api-docs/**",
                        "/v3/api-docs/**",
                        "/error",
                        "/favicon.ico",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/static/**"
                );
    }
}
