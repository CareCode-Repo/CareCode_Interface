package com.carecode.domain.notification.factory;

import com.carecode.domain.notification.strategy.NotificationStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 알림 전략 팩토리
 * 알림 타입에 따라 적절한 전략을 반환
 */
@Slf4j
@Component
public class NotificationStrategyFactory {
    
    private final List<NotificationStrategy> strategies;
    private final Map<String, NotificationStrategy> strategyMap;
    
    public NotificationStrategyFactory(List<NotificationStrategy> strategies) {
        this.strategies = strategies;
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(
                    NotificationStrategy::getNotificationType,
                    Function.identity()
                ));
    }
    
    /**
     * 알림 타입에 따른 전략 반환
     */
    public NotificationStrategy getStrategy(String notificationType) {
        NotificationStrategy strategy = strategyMap.get(notificationType.toUpperCase());
        
        if (strategy == null) {
            log.warn("지원하지 않는 알림 타입: {}", notificationType);
            // 기본 전략 반환 (시스템 알림)
            return strategyMap.get("SYSTEM");
        }
        
        return strategy;
    }
    
    /**
     * 지원하는 모든 알림 타입 반환
     */
    public List<String> getSupportedNotificationTypes() {
        return strategies.stream()
                .map(NotificationStrategy::getNotificationType)
                .collect(Collectors.toList());
    }
    
    /**
     * 전략 존재 여부 확인
     */
    public boolean supportsNotificationType(String notificationType) {
        return strategyMap.containsKey(notificationType.toUpperCase());
    }
} 