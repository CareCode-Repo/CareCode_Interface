package com.carecode.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // 자격 증명 허용
        config.setAllowCredentials(true);
        
        // 허용할 Origins 설정
        config.addAllowedOriginPattern("http://localhost:3000"); // Next.js 개발 서버
        config.addAllowedOriginPattern("http://127.0.0.1:3000");
        config.addAllowedOriginPattern("http://localhost:*");
        config.addAllowedOriginPattern("*"); // 모든 오리진 허용 (개발용)
        
        // 허용할 헤더
        config.addAllowedHeader("*");
        
        // 허용할 HTTP 메서드
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS");
        config.addAllowedMethod("PATCH");
        
        // 노출할 헤더 (클라이언트에서 접근 가능)
        config.addExposedHeader("Authorization");
        config.addExposedHeader("X-Refresh-Token");
        config.addExposedHeader("X-New-User");
        
        // 모든 경로에 CORS 설정 적용
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
