package com.carecode.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class AuditConfig {
    // AuditorAwareImpl이 @Component로 등록되어 있으므로 별도 빈 정의 불필요
}