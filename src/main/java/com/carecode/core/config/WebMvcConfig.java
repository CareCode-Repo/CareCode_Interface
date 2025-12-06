package com.carecode.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 설정
 * API 버전 관리 등을 포함합니다.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    // API 버전 관리는 ApiVersionConfig를 통해 처리됩니다.
    // 필요시 추가 설정을 여기에 구현할 수 있습니다.
}

