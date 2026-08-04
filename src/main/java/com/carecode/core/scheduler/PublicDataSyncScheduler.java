package com.carecode.core.scheduler;

import com.carecode.core.client.sync.GovernmentBenefitSyncService;
import com.carecode.core.client.sync.NationwideChildcareFacilitySyncService;
import com.carecode.core.client.sync.PediatricHospitalSyncService;
import com.carecode.core.client.sync.SyncResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 공공데이터 주기 동기화.
 *
 * <p>이전에는 관리자가 API 를 직접 호출해야 했고, 페이지 상한에 걸리면 조용히 멈췄다.
 * 여기서는 정기적으로 돌리고 결과(신규/갱신/실패/중단 사유)를 로그로 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PublicDataSyncScheduler {

    private final NationwideChildcareFacilitySyncService facilitySyncService;
    private final GovernmentBenefitSyncService benefitSyncService;
    private final PediatricHospitalSyncService hospitalSyncService;

    /**
     * 전국 어린이집 동기화. 매주 월요일 새벽 3시.
     * 시설 정보는 변동이 잦지 않아 주 1회로 충분하고, 트래픽 한도(일 10,000건)도 아낄 수 있다.
     */
    @Scheduled(cron = "${app.scheduler.public-data.facility-cron:0 0 3 * * MON}", zone = "Asia/Seoul")
    public void syncChildcareFacilities() {
        SyncResult result = facilitySyncService.sync();
        logResult("전국 어린이집", result);
    }

    /**
     * 정부 지원 서비스(보조금24) 동기화. 매일 새벽 3시 30분.
     * 정책은 신청 기간이 있어 시설보다 자주 확인한다.
     */
    @Scheduled(cron = "${app.scheduler.public-data.benefit-cron:0 30 3 * * *}", zone = "Asia/Seoul")
    public void syncGovernmentBenefits() {
        SyncResult result = benefitSyncService.sync();
        logResult("정부 지원 서비스", result);
    }

    /**
     * 소아청소년과 병원 동기화. 매주 화요일 새벽 3시.
     * 시설 동기화와 같은 날 돌리면 일일 트래픽 한도를 함께 소진하므로 하루 띄운다.
     */
    @Scheduled(cron = "${app.scheduler.public-data.hospital-cron:0 0 3 * * TUE}", zone = "Asia/Seoul")
    public void syncPediatricHospitals() {
        SyncResult result = hospitalSyncService.sync();
        logResult("소아청소년과 병원", result);
    }

    private void logResult(String label, SyncResult result) {
        if (!result.isCompleted()) {
            log.warn("{} 동기화 미완료 - {}", label, result);
            return;
        }
        if (result.getTotalProcessed() == 0 && result.getFailed() == 0) {
            log.debug("{} 동기화: 변경 없음", label);
            return;
        }
        log.info("{} 동기화 완료 - {}", label, result);
    }
}
