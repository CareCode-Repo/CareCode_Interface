package com.carecode.core.config;

import com.carecode.core.monitoring.QueryCountFilter;
import com.carecode.core.monitoring.QueryCountInspector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.util.Map;

/** 쿼리 수 측정. 기본은 꺼져 있고 app.monitoring.query-count.enabled=true 일 때만 동작한다. */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.monitoring.query-count.enabled", havingValue = "true")
public class QueryCountConfig {

    @Value("${app.monitoring.query-count.threshold:20}")
    private int threshold;

    /** StatementInspector 는 SessionFactory 생성 시점에 주입해야 하므로 프로퍼티로 넣는다. */
    @Bean
    public HibernatePropertiesCustomizer queryCountInspectorCustomizer() {
        log.info("쿼리 수 측정 활성화 - 임계치 {}건", threshold);
        return (Map<String, Object> props) ->
                props.put("hibernate.session_factory.statement_inspector", new QueryCountInspector());
    }

    /** 가장 바깥에서 감싸야 요청 전체의 쿼리를 센다. */
    @Bean
    public FilterRegistrationBean<QueryCountFilter> queryCountFilterRegistration() {
        FilterRegistrationBean<QueryCountFilter> registration =
                new FilterRegistrationBean<>(new QueryCountFilter(threshold));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
