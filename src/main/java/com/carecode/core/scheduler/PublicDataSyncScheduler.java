package com.carecode.core.scheduler;

import com.carecode.core.client.sync.GovernmentBenefitSyncService;
import com.carecode.core.client.sync.KindergartenSyncService;
import com.carecode.core.client.sync.NationwideChildcareFacilitySyncService;
import com.carecode.core.client.sync.PediatricHospitalSyncService;
import com.carecode.core.client.sync.SyncResult;
import com.carecode.core.geocoding.FacilityGeocodingService;
import com.carecode.core.ops.OperationalAlerter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 공공데이터 주기 동기화. */
@Slf4j
@Component
@RequiredArgsConstructor
public class PublicDataSyncScheduler {

    private final NationwideChildcareFacilitySyncService facilitySyncService;
    private final KindergartenSyncService kindergartenSyncService;
    private final GovernmentBenefitSyncService benefitSyncService;
    private final PediatricHospitalSyncService hospitalSyncService;
    private final FacilityGeocodingService geocodingService;
    private final OperationalAlerter alerter;

    /** 전국 어린이집 동기화. */
    @Scheduled(cron = "${app.scheduler.public-data.facility-cron:0 0 3 * * MON}", zone = "Asia/Seoul")
    public void syncChildcareFacilities() {
        SyncResult result = facilitySyncService.sync();
        logResult("전국 어린이집", result);
    }

    /** 전국 유치원 동기화. 어린이집 작업과 겹치지 않게 시간을 벌린다. */
    @Scheduled(cron = "${app.scheduler.public-data.kindergarten-cron:0 0 4 * * MON}", zone = "Asia/Seoul")
    public void syncKindergartens() {
        SyncResult result = kindergartenSyncService.sync();
        logResult("전국 유치원", result);
    }

    /** 정부 지원 서비스(보조금24) 동기화. */
    @Scheduled(cron = "${app.scheduler.public-data.benefit-cron:0 30 3 * * *}", zone = "Asia/Seoul")
    public void syncGovernmentBenefits() {
        SyncResult result = benefitSyncService.sync();
        logResult("정부 지원 서비스", result);
    }

    /** 소아청소년과 병원 동기화. */
    @Scheduled(cron = "${app.scheduler.public-data.hospital-cron:0 0 3 * * TUE}", zone = "Asia/Seoul")
    public void syncPediatricHospitals() {
        SyncResult result = hospitalSyncService.sync();
        logResult("소아청소년과 병원", result);
    }

    /** 좌표 보정. 동기화가 끝난 뒤 돌아야 새로 들어온 시설이 대상에 포함된다. */
    @Scheduled(cron = "${app.scheduler.public-data.geocoding-cron:0 0 5 * * *}", zone = "Asia/Seoul")
    public void fillMissingCoordinates() {
        var result = geocodingService.fillMissingCoordinates();
        log.info("시설 좌표 보정 - {}", result);
    }

    private void logResult(String label, SyncResult result) {
        if (!result.isCompleted()) {
            log.warn("{} 동기화 미완료 - {}", label, result);
            alerter.alert("sync-" + label, label + " 동기화 미완료", result.toString());
            return;
        }
        if (result.getFailed() > 0) {
            alerter.alert("sync-failed-" + label,
                    label + " 동기화 중 " + result.getFailed() + "건 실패", result.toString());
        }
        if (result.getTotalProcessed() == 0 && result.getFailed() == 0) {
            log.debug("{} 동기화: 변경 없음", label);
            return;
        }
        log.info("{} 동기화 완료 - {}", label, result);
    }
}
