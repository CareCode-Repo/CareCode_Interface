package com.carecode.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * API 관련 설정 클래스
 */
@Configuration
@ConfigurationProperties(prefix = "app.api")
public class ApiConfig {
    
    private String baseUrl = "/api/v1";
    private String version = "v1";
    private String title = "맘편한 API";
    private String description = "육아 지원 플랫폼 맘편한의 REST API";
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
} 