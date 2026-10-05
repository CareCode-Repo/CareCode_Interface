package com.carecode.domain.admin.controller;

import com.carecode.external.publicdata.sync.GovernmentBenefitSyncService;
import com.carecode.external.publicdata.sync.KindergartenSyncService;
import com.carecode.external.publicdata.sync.NationwideChildcareFacilitySyncService;
import com.carecode.external.publicdata.sync.PediatricHospitalSyncService;
import com.carecode.external.publicdata.sync.SyncResult;
import com.carecode.external.geocoding.FacilityGeocodingService;
import com.carecode.core.ops.sync.SyncJob;
import com.carecode.core.ops.sync.SyncRunTracker;
import com.carecode.domain.facility.service.FacilityVacancyNotifier;
import com.carecode.domain.policy.service.PolicyDeadlineNotifier;
import com.carecode.domain.admin.dto.response.DeadlineNotifyResponse;
import com.carecode.domain.admin.dto.response.GeocodingResultResponse;
import com.carecode.domain.admin.dto.response.SyncResultResponse;
import com.carecode.domain.admin.dto.response.VacancyNotifyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/** 공공데이터 수동 동기화 API. */
@RestController
@RequestMapping("/api/admin/public-data")
@RequiredArgsConstructor
@Tag(name = "어드민 - 공공데이터", description = "공공데이터 수동 동기화 API")
public class AdminPublicDataController {

    private final NationwideChildcareFacilitySyncService facilitySyncService;
    private final KindergartenSyncService kindergartenSyncService;
    private final GovernmentBenefitSyncService benefitSyncService;
    private final PediatricHospitalSyncService hospitalSyncService;
    private final FacilityGeocodingService geocodingService;
    private final FacilityVacancyNotifier vacancyNotifier;
    private final PolicyDeadlineNotifier policyDeadlineNotifier;
    // 수동 실행도 데이터를 갱신하므로 신선도 이력에 남긴다. 남기지 않으면 방금 돌린 동기화를
    // 신선도 지표가 모르고 "낡음" 으로 알린다.
    private final SyncRunTracker tracker;

    @PostMapping("/facilities/sync")
    @Operation(summary = "전국 어린이집 동기화", description = "시설 코드 기준으로 갱신")
    public ResponseEntity<SyncResultResponse> syncFacilities() {
        java.time.LocalDateTime startedAt = java.time.LocalDateTime.now();
        SyncResult result = facilitySyncService.sync();
        tracker.recordSyncResult(SyncJob.CHILDCARE_FACILITIES, startedAt, result);
        return ResponseEntity.ok(SyncResultResponse.from(result));
    }

    @PostMapping("/kindergartens/sync")
    @Operation(summary = "전국 유치원 동기화", description = "유치원명·주소 기준으로 갱신")
    public ResponseEntity<SyncResultResponse> syncKindergartens() {
        java.time.LocalDateTime startedAt = java.time.LocalDateTime.now();
        SyncResult result = kindergartenSyncService.sync();
        tracker.recordSyncResult(SyncJob.KINDERGARTENS, startedAt, result);
        return ResponseEntity.ok(SyncResultResponse.from(result));
    }

    @PostMapping("/benefits/sync")
    @Operation(summary = "정부 지원 서비스 동기화", description = "육아 관련 서비스만 정책으로 갱신")
    public ResponseEntity<SyncResultResponse> syncBenefits() {
        java.time.LocalDateTime startedAt = java.time.LocalDateTime.now();
        SyncResult result = benefitSyncService.sync();
        tracker.recordSyncResult(SyncJob.GOVERNMENT_BENEFITS, startedAt, result);
        return ResponseEntity.ok(SyncResultResponse.from(result));
    }

    @PostMapping("/hospitals/sync")
    @Operation(summary = "소아청소년과 병원 동기화", description = "요양기호 기준으로 갱신")
    public ResponseEntity<SyncResultResponse> syncHospitals() {
        java.time.LocalDateTime startedAt = java.time.LocalDateTime.now();
        SyncResult result = hospitalSyncService.sync();
        tracker.recordSyncResult(SyncJob.PEDIATRIC_HOSPITALS, startedAt, result);
        return ResponseEntity.ok(SyncResultResponse.from(result));
    }

    @PostMapping("/facilities/geocode")
    @Operation(summary = "시설 좌표 보정", description = "좌표 없는 시설의 주소를 좌표로 변환")
    public ResponseEntity<GeocodingResultResponse> geocode() {
        java.time.LocalDateTime startedAt = java.time.LocalDateTime.now();
        var result = geocodingService.fillMissingCoordinates();
        tracker.recordSuccess(SyncJob.FACILITY_GEOCODING, startedAt,
                result.getResolved(), result.getFailed(), result.getSkippedReason());
        return ResponseEntity.ok(GeocodingResultResponse.from(result));
    }

    /**
     * 빈자리 알림 수동 실행.
     *
     * <p>스케줄러는 하루 한 번만 돌아서, 발송이 안 나갔을 때 원인을 확인하려면
     * 다음 날까지 기다려야 한다. 확인한 시설 수까지 돌려주므로 대기자가 없어서인지
     * 자리가 안 나서인지 구분할 수 있다.
     */
    @PostMapping("/facilities/notify-vacancy")
    @Operation(summary = "빈자리 알림 실행", description = "대기자가 있는 시설에 새로 난 자리를 알린다")
    public ResponseEntity<VacancyNotifyResponse> notifyVacancies() {
        return ResponseEntity.ok(VacancyNotifyResponse.from(vacancyNotifier.notifyNewVacancies()));
    }

    @PostMapping("/policies/notify-deadline")
    @Operation(summary = "마감 임박 알림 실행", description = "신청 마감이 임박한 지원금을 대상자에게 알린다")
    public ResponseEntity<DeadlineNotifyResponse> notifyDeadlines() {
        return ResponseEntity.ok(DeadlineNotifyResponse.from(policyDeadlineNotifier.notifyUpcomingDeadlines()));
    }
}
