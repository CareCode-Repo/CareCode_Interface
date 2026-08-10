package com.carecode.core.devtools;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** 기동 시 샘플 데이터를 적재한다. */
@Slf4j
@Component
@Profile("!prod")
@ConditionalOnProperty(name = "app.dev.seed-sample-data", havingValue = "true")
@RequiredArgsConstructor
public class SampleDataRunner implements ApplicationRunner {

    private final SamplePolicySeeder policySeeder;
    private final SampleFacilitySeeder facilitySeeder;

    @Override
    public void run(ApplicationArguments args) {
        try {
            int policies = policySeeder.seed();
            int facilities = facilitySeeder.seed();

            if (policies == 0 && facilities == 0) {
                log.info("샘플 데이터가 이미 적재되어 있습니다.");
                return;
            }
            log.warn("샘플 데이터를 적재했습니다 - 정책 {}건, 시설 {}건. "
                    + "실제 지원 금액이 아니므로 운영 데이터와 혼동하지 마세요. "
                    + "제거하려면 DELETE /api/admin/dev/sample-data 를 호출하세요.", policies, facilities);
        } catch (Exception e) {
            // 샘플 적재 실패가 애플리케이션 기동을 막지 않도록 한다.
            log.error("샘플 데이터 적재 실패", e);
        }
    }
}
