package com.carecode.core.config;

import org.springframework.context.annotation.Configuration;

/**
 * API 버전 관리 설정
 * 
 * 현재는 BaseController에서 /api/v1 경로를 사용하고 있습니다.
 * 향후 버전 관리가 필요할 경우:
 * 1. URL 경로 기반: /api/v1/health, /api/v2/health
 * 2. 헤더 기반: API-Version: v1
 * 3. 별도 Controller 클래스: HealthControllerV1, HealthControllerV2
 * 
 * @ApiVersion 어노테이션은 향후 확장을 위해 준비되었습니다.
 */
@Configuration
public class ApiVersionConfig {
    
    // 현재는 BaseController에서 /api/v1을 사용
    // 향후 버전 관리가 필요할 때 여기에 구현
}

