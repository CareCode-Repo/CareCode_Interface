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

    /** 업로드된 파일을 정적 리소스로 서빙한다. (S3 로 전환하면 이 매핑 대신 버킷 URL 을 그대로 쓰면 된다.) */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(storageRoot).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler(publicBaseUrl + "/**")
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
