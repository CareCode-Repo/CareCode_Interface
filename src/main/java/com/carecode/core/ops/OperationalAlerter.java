package com.carecode.core.ops;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 운영 이상을 Slack 으로 알린다. 웹훅이 없으면 로그만 남기고 조용히 비활성 상태로 동작한다. */
@Slf4j
@Component
public class OperationalAlerter {

    /** 같은 알림이 쏟아지면 아무도 안 보게 된다. 키별로 이 간격 안에는 한 번만 보낸다. */
    private static final Duration COOLDOWN = Duration.ofMinutes(30);

    private final RestTemplate restTemplate;
    private final String webhookUrl;
    private final String environment;
    private final Map<String, LocalDateTime> lastSentAt = new ConcurrentHashMap<>();

    public OperationalAlerter(RestTemplate restTemplate,
                              @Value("${app.ops.slack-webhook-url:}") String webhookUrl,
                              @Value("${spring.profiles.active:local}") String environment) {
        this.restTemplate = restTemplate;
        this.webhookUrl = webhookUrl;
        this.environment = environment;
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.info("운영 알림 웹훅이 설정되지 않아 로그로만 남깁니다.");
        }
    }

    public boolean isEnabled() {
        return webhookUrl != null && !webhookUrl.isBlank();
    }

    /**
     * @param key   중복 억제 기준. 같은 원인은 같은 키를 쓴다.
     * @param title 한 줄 요약
     */
    @Async("analyticsExecutor")
    public void alert(String key, String title, String detail) {
        if (isSuppressed(key)) {
            log.debug("알림 억제 (쿨다운) - key={}", key);
            return;
        }
        log.error("[운영알림] {} - {}", title, detail);

        if (!isEnabled()) {
            return;
        }
        try {
            String text = String.format("*[%s] %s*\n```%s```", environment, title, truncate(detail));
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity(webhookUrl,
                    new HttpEntity<>(Map.of("text", text), headers), String.class);
        } catch (Exception e) {
            // 알림 실패가 서비스에 영향을 주면 안 된다.
            log.warn("운영 알림 전송 실패: {}", e.getMessage());
        }
    }

    private boolean isSuppressed(String key) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime previous = lastSentAt.get(key);
        if (previous != null && previous.plus(COOLDOWN).isAfter(now)) {
            return true;
        }
        lastSentAt.put(key, now);
        return false;
    }

    private String truncate(String detail) {
        if (detail == null) {
            return "";
        }
        return detail.length() <= 1500 ? detail : detail.substring(0, 1500) + "...";
    }
}
