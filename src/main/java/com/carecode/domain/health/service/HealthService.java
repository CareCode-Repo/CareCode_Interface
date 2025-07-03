package com.carecode.domain.health.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 헬스 체크 서비스 클래스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthService {
    
    /**
     * 시스템 상태 확인
     */
    public Map<String, Object> checkSystemHealth() {
        log.info("시스템 상태 확인");
        
        Map<String, Object> healthStatus = new HashMap<>();
        healthStatus.put("status", "UP");
        healthStatus.put("timestamp", System.currentTimeMillis());
        healthStatus.put("version", "1.0.0");
        
        return healthStatus;
    }
} 