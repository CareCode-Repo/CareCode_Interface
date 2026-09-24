package com.carecode.core.scheduler;

import com.carecode.core.client.sync.GovernmentBenefitSyncService;
import com.carecode.core.client.sync.KindergartenSyncService;
import com.carecode.core.client.sync.NationwideChildcareFacilitySyncService;
import com.carecode.core.client.sync.PediatricHospitalSyncService;
import com.carecode.core.geocoding.FacilityGeocodingService;
import com.carecode.domain.careFacility.service.FacilityVacancyNotifier;
import com.carecode.domain.policy.service.BenefitReportSolicitor;
import com.carecode.domain.policy.service.PolicyChangeNotifier;
import com.carecode.domain.policy.service.PolicyDeadlineNotifier;
import com.carecode.core.ops.sync.SyncJob;
import com.carecode.core.ops.sync.SyncRunTracker;
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
    private final PolicyChangeNotifier policyChangeNotifier;
    private final PolicyDeadlineNotifier policyDeadlineNotifier;
    private final FacilityVacancyNotifier vacancyNotifier;
    private final BenefitReportSolicitor reportSolicitor;
    // 실행 이력·신선도 지표·실패 알림을 한곳에서 담당한다.
    private final SyncRunTracker tracker;

    /** 전국 어린이집 동기화. */
    @Scheduled(cron = "${app.scheduler.public-data.facility-cron:0 0 3 * * MON}", zone = "Asia/Seoul")
    public void syncChildcareFacilities() {
        tracker.track(SyncJob.CHILDCARE_FACILITIES, facilitySyncService::sync);
    }

    /** 전국 유치원 동기화. 어린이집 작업과 겹치지 않게 시간을 벌린다. */
    @Scheduled(cron = "${app.scheduler.public-data.kindergarten-cron:0 0 4 * * MON}", zone = "Asia/Seoul")
    public void syncKindergartens() {
        tracker.track(SyncJob.KINDERGARTENS, kindergartenSyncService::sync);
    }

    /** 정부 지원 서비스(보조금24) 동기화. */
    @Scheduled(cron = "${app.scheduler.public-data.benefit-cron:0 30 3 * * *}", zone = "Asia/Seoul")
    public void syncGovernmentBenefits() {
        tracker.track(SyncJob.GOVERNMENT_BENEFITS, benefitSyncService::sync);
    }

    /** 소아청소년과 병원 동기화. */
    @Scheduled(cron = "${app.scheduler.public-data.hospital-cron:0 0 3 * * TUE}", zone = "Asia/Seoul")
    public void syncPediatricHospitals() {
        tracker.track(SyncJob.PEDIATRIC_HOSPITALS, hospitalSyncService::sync);
    }

    /** 정책 변경 알림. 동기화가 끝난 뒤 돌아야 그날 바뀐 내용이 잡힌다. */
    @Scheduled(cron = "${app.scheduler.public-data.policy-change-cron:0 0 9 * * *}", zone = "Asia/Seoul")
    public void notifyPolicyChanges() {
        tracker.track(SyncJob.POLICY_CHANGE_NOTICE, policyChangeNotifier::notifyPendingChanges);
    }

    /**
     * 빈자리 알림. 시설 동기화로 새 정원이 들어온 뒤에 돌아야 그날 난 자리가 잡힌다.
     * 대기 걸어둔 사람이 이 앱을 다시 열 가장 강한 이유다.
     */
    @Scheduled(cron = "${app.scheduler.public-data.vacancy-cron:0 30 9 * * *}", zone = "Asia/Seoul")
    public void notifyFacilityVacancies() {
        tracker.track(SyncJob.FACILITY_VACANCY_NOTICE, vacancyNotifier::notifyNewVacancies);
    }

    /**
     * 신청 마감 임박 알림. 놓친 뒤에 알려주는 것보다 놓치기 전에 막는 편이 낫다.
     * 마감일까지 남은 일수로 판단하므로 매일 돌아야 D-7·D-1 을 놓치지 않는다.
     */
    @Scheduled(cron = "${app.scheduler.public-data.policy-deadline-cron:0 0 10 * * *}", zone = "Asia/Seoul")
    public void notifyPolicyDeadlines() {
        tracker.track(SyncJob.POLICY_DEADLINE_NOTICE, policyDeadlineNotifier::notifyUpcomingDeadlines);
    }

    /** 실수령액 제보 요청. 매일 보내면 소음이라 주 1회만 묻는다. */
    @Scheduled(cron = "${app.scheduler.public-data.report-ask-cron:0 0 10 * * WED}", zone = "Asia/Seoul")
    public void solicitBenefitReports() {
        tracker.track(SyncJob.BENEFIT_REPORT_SOLICIT, reportSolicitor::solicitReports);
    }

    /** 좌표 보정. 동기화가 끝난 뒤 돌아야 새로 들어온 시설이 대상에 포함된다. */
    @Scheduled(cron = "${app.scheduler.public-data.geocoding-cron:0 0 5 * * *}", zone = "Asia/Seoul")
    public void fillMissingCoordinates() {
        tracker.track(SyncJob.FACILITY_GEOCODING, geocodingService::fillMissingCoordinates);
    }

}
