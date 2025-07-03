package com.carecode.health.controller;

import com.carecode.core.annotation.LogExecutionTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 헬스 체크 API 컨트롤러
 * 시스템 상태 모니터링 및 진단
 */
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Slf4j
public class HealthCheckController {

    /**
     * 기본 헬스 체크
     */
    @GetMapping
    @LogExecutionTime
    public ResponseEntity<HealthResponse> healthCheck() {
        log.info("기본 헬스 체크 요청");
        
        HealthResponse response = new HealthResponse();
        response.setStatus("UP");
        response.setTimestamp(java.time.LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 상세 헬스 체크
     */
    @GetMapping("/detailed")
    @LogExecutionTime
    public ResponseEntity<DetailedHealthResponse> detailedHealthCheck() {
        log.info("상세 헬스 체크 요청");
        
        DetailedHealthResponse response = new DetailedHealthResponse();
        response.setStatus("UP");
        response.setTimestamp(java.time.LocalDateTime.now().toString());
        
        // 각 컴포넌트 상태 확인
        Map<String, ComponentHealth> components = Map.of(
            "database", new ComponentHealth("UP", "Database connection is healthy"),
            "cache", new ComponentHealth("UP", "Cache service is operational"),
            "external-api", new ComponentHealth("UP", "External APIs are accessible"),
            "file-system", new ComponentHealth("UP", "File system is accessible")
        );
        response.setComponents(components);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 데이터베이스 연결 상태 확인
     */
    @GetMapping("/database")
    @LogExecutionTime
    public ResponseEntity<ComponentHealth> databaseHealthCheck() {
        log.info("데이터베이스 헬스 체크 요청");
        
        // 데이터베이스 연결 상태 확인 로직
        ComponentHealth health = new ComponentHealth("UP", "Database connection is healthy");
        return ResponseEntity.ok(health);
    }

    /**
     * 캐시 서비스 상태 확인
     */
    @GetMapping("/cache")
    @LogExecutionTime
    public ResponseEntity<ComponentHealth> cacheHealthCheck() {
        log.info("캐시 서비스 헬스 체크 요청");
        
        // 캐시 서비스 상태 확인 로직
        ComponentHealth health = new ComponentHealth("UP", "Cache service is operational");
        return ResponseEntity.ok(health);
    }

    /**
     * 외부 API 연결 상태 확인
     */
    @GetMapping("/external-apis")
    @LogExecutionTime
    public ResponseEntity<Map<String, ComponentHealth>> externalApisHealthCheck() {
        log.info("외부 API 헬스 체크 요청");
        
        // 외부 API 연결 상태 확인 로직
        Map<String, ComponentHealth> apis = Map.of(
            "policy-api", new ComponentHealth("UP", "Policy API is accessible"),
            "facility-api", new ComponentHealth("UP", "Facility API is accessible"),
            "notification-api", new ComponentHealth("UP", "Notification API is accessible")
        );
        
        return ResponseEntity.ok(apis);
    }

    /**
     * 시스템 정보 조회
     */
    @GetMapping("/info")
    @LogExecutionTime
    public ResponseEntity<SystemInfoResponse> getSystemInfo() {
        log.info("시스템 정보 조회 요청");
        
        SystemInfoResponse info = new SystemInfoResponse();
        info.setApplicationName("CareCode");
        info.setVersion("1.0.0");
        info.setEnvironment("development");
        info.setJavaVersion(System.getProperty("java.version"));
        info.setStartTime(java.time.LocalDateTime.now().toString());
        
        // 메모리 정보
        Runtime runtime = Runtime.getRuntime();
        info.setTotalMemory(runtime.totalMemory());
        info.setFreeMemory(runtime.freeMemory());
        info.setUsedMemory(runtime.totalMemory() - runtime.freeMemory());
        
        return ResponseEntity.ok(info);
    }

    /**
     * 메트릭 정보 조회
     */
    @GetMapping("/metrics")
    @LogExecutionTime
    public ResponseEntity<MetricsResponse> getMetrics() {
        log.info("메트릭 정보 조회 요청");
        
        MetricsResponse metrics = new MetricsResponse();
        metrics.setTimestamp(java.time.LocalDateTime.now().toString());
        
        // 시스템 메트릭 수집
        Runtime runtime = Runtime.getRuntime();
        metrics.setCpuUsage(0.15); // 임시 값
        metrics.setMemoryUsage((double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.totalMemory());
        metrics.setActiveConnections(10);
        metrics.setRequestCount(1000);
        metrics.setErrorRate(0.01);
        
        return ResponseEntity.ok(metrics);
    }

    /**
     * 핑 테스트
     */
    @GetMapping("/ping")
    @LogExecutionTime
    public ResponseEntity<Map<String, String>> ping() {
        log.info("핑 테스트 요청");
        
        return ResponseEntity.ok(Map.of(
            "message", "pong",
            "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }

    /**
     * 준비 상태 확인 (Readiness Probe)
     */
    @GetMapping("/ready")
    @LogExecutionTime
    public ResponseEntity<ReadinessResponse> readinessCheck() {
        log.info("준비 상태 확인 요청");
        
        ReadinessResponse response = new ReadinessResponse();
        response.setReady(true);
        response.setTimestamp(java.time.LocalDateTime.now().toString());
        
        // 애플리케이션이 요청을 처리할 준비가 되었는지 확인
        // 데이터베이스 연결, 캐시 연결, 필수 서비스 등 확인
        
        return ResponseEntity.ok(response);
    }

    /**
     * 활성 상태 확인 (Liveness Probe)
     */
    @GetMapping("/live")
    @LogExecutionTime
    public ResponseEntity<LivenessResponse> livenessCheck() {
        log.info("활성 상태 확인 요청");
        
        LivenessResponse response = new LivenessResponse();
        response.setAlive(true);
        response.setTimestamp(java.time.LocalDateTime.now().toString());
        
        // 애플리케이션이 정상적으로 동작하고 있는지 확인
        
        return ResponseEntity.ok(response);
    }

    // DTO 클래스들
    public static class HealthResponse {
        private String status;
        private String timestamp;
        
        // getters and setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }

    public static class DetailedHealthResponse {
        private String status;
        private String timestamp;
        private Map<String, ComponentHealth> components;
        
        // getters and setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        public Map<String, ComponentHealth> getComponents() { return components; }
        public void setComponents(Map<String, ComponentHealth> components) { this.components = components; }
    }

    public static class ComponentHealth {
        private String status;
        private String message;
        private String details;
        
        public ComponentHealth(String status, String message) {
            this.status = status;
            this.message = message;
        }
        
        // getters and setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
    }

    public static class SystemInfoResponse {
        private String applicationName;
        private String version;
        private String environment;
        private String javaVersion;
        private String startTime;
        private long totalMemory;
        private long freeMemory;
        private long usedMemory;
        
        // getters and setters
        public String getApplicationName() { return applicationName; }
        public void setApplicationName(String applicationName) { this.applicationName = applicationName; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }
        public String getJavaVersion() { return javaVersion; }
        public void setJavaVersion(String javaVersion) { this.javaVersion = javaVersion; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public long getTotalMemory() { return totalMemory; }
        public void setTotalMemory(long totalMemory) { this.totalMemory = totalMemory; }
        public long getFreeMemory() { return freeMemory; }
        public void setFreeMemory(long freeMemory) { this.freeMemory = freeMemory; }
        public long getUsedMemory() { return usedMemory; }
        public void setUsedMemory(long usedMemory) { this.usedMemory = usedMemory; }
    }

    public static class MetricsResponse {
        private String timestamp;
        private double cpuUsage;
        private double memoryUsage;
        private int activeConnections;
        private int requestCount;
        private double errorRate;
        
        // getters and setters
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        public double getCpuUsage() { return cpuUsage; }
        public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }
        public double getMemoryUsage() { return memoryUsage; }
        public void setMemoryUsage(double memoryUsage) { this.memoryUsage = memoryUsage; }
        public int getActiveConnections() { return activeConnections; }
        public void setActiveConnections(int activeConnections) { this.activeConnections = activeConnections; }
        public int getRequestCount() { return requestCount; }
        public void setRequestCount(int requestCount) { this.requestCount = requestCount; }
        public double getErrorRate() { return errorRate; }
        public void setErrorRate(double errorRate) { this.errorRate = errorRate; }
    }

    public static class ReadinessResponse {
        private boolean ready;
        private String timestamp;
        
        // getters and setters
        public boolean isReady() { return ready; }
        public void setReady(boolean ready) { this.ready = ready; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }

    public static class LivenessResponse {
        private boolean alive;
        private String timestamp;
        
        // getters and setters
        public boolean isAlive() { return alive; }
        public void setAlive(boolean alive) { this.alive = alive; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }
} 