package com.carecode.domain.health.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.domain.health.dto.request.ChildCreateRequest;
import com.carecode.domain.health.dto.response.ChildInfoResponse;
import com.carecode.domain.health.dto.response.SiblingOverviewResponse;
import com.carecode.domain.health.service.SiblingOverviewService;
import com.carecode.domain.health.dto.response.GrowthPointResponse;
import com.carecode.domain.health.dto.response.VaccinationScheduleResponse;
import com.carecode.domain.health.growth.GrowthMetric;
import com.carecode.domain.health.service.ChildService;
import com.carecode.domain.health.service.GrowthChartService;
import com.carecode.domain.health.service.VaccinationScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** 아이 정보 및 예방접종 일정 API. */
@Slf4j
@RestController
@RequestMapping("/children")
@RequiredArgsConstructor
@Tag(name = "아이 관리", description = "아이 등록·조회 및 예방접종 일정 API")
public class ChildController {

    private final ChildService childService;
    private final SiblingOverviewService siblingOverviewService;
    private final VaccinationScheduleService vaccinationScheduleService;
    private final GrowthChartService growthChartService;

    @PostMapping
    @LogExecutionTime
    @Operation(summary = "아이 등록",
            description = "아이를 등록하고 생년월일 기준 표준 예방접종 일정을 자동 생성")
    public ResponseEntity<ChildInfoResponse> createChild(@Valid @RequestBody ChildCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(childService.createChild(request));
    }

    @GetMapping
    @LogExecutionTime
    @Operation(summary = "내 아이 목록 조회")
    public ResponseEntity<List<ChildInfoResponse>> getMyChildren() {
        return ResponseEntity.ok(childService.getMyChildren());
    }

    @GetMapping("/{childId}")
    @LogExecutionTime
    @Operation(summary = "아이 상세 조회")
    public ResponseEntity<ChildInfoResponse> getChild(@PathVariable Long childId) {
        return ResponseEntity.ok(childService.getChild(childId));
    }

    @PutMapping("/{childId}")
    @LogExecutionTime
    @Operation(summary = "아이 정보 수정")
    public ResponseEntity<ChildInfoResponse> updateChild(@PathVariable Long childId,
                                                         @Valid @RequestBody ChildCreateRequest request) {
        return ResponseEntity.ok(childService.updateChild(childId, request));
    }

    @DeleteMapping("/{childId}")
    @LogExecutionTime
    @Operation(summary = "아이 삭제")
    public ResponseEntity<Void> deleteChild(@PathVariable Long childId) {
        childService.deleteChild(childId);
        return ResponseEntity.noContent().build();
    }

    // ====================
    // 예방접종 일정 ====================
    @GetMapping("/{childId}/vaccinations")
    @LogExecutionTime
    @Operation(summary = "예방접종 일정 조회", description = "표준 일정에 따른 접종 예정일과 완료 여부 반환")
    public ResponseEntity<List<VaccinationScheduleResponse>> getVaccinationSchedule(@PathVariable Long childId) {
        // 소유권 검증을 위해 아이 조회를 먼저 태운다.
        childService.getChild(childId);
        return ResponseEntity.ok(vaccinationScheduleService.getSchedule(childId));
    }

    @GetMapping("/{childId}/vaccinations/overdue")
    @LogExecutionTime
    @Operation(summary = "미접종(기한 경과) 목록 조회")
    public ResponseEntity<List<VaccinationScheduleResponse>> getOverdueVaccinations(@PathVariable Long childId) {
        childService.getChild(childId);
        return ResponseEntity.ok(vaccinationScheduleService.getOverdue(childId));
    }

    @PatchMapping("/{childId}/vaccinations/{scheduleId}/complete")
    @LogExecutionTime
    @Operation(summary = "접종 완료 처리")
    public ResponseEntity<VaccinationScheduleResponse> completeVaccination(
            @PathVariable Long childId,
            @PathVariable Long scheduleId,
            @Parameter(description = "실제 접종일 (미지정 시 오늘)")
            @RequestParam(required = false) LocalDate completedDate) {
        childService.getChild(childId);
        return ResponseEntity.ok(vaccinationScheduleService.markCompleted(scheduleId, completedDate));
    }

    // ====================
    // 성장 곡선 ====================
    @GetMapping("/{childId}/growth")
    @LogExecutionTime
    @Operation(summary = "성장 곡선 조회",
            description = "기록된 키/몸무게를 WHO 성장 표준과 비교한 백분위와 함께 반환"
                    + "백분위는 참고 지표이며 진단은 의료진 판단을 따릅니다.")
    public ResponseEntity<List<GrowthPointResponse>> getGrowthChart(
            @PathVariable Long childId,
            @Parameter(description = "지표 (WEIGHT 또는 HEIGHT)")
            @RequestParam(defaultValue = "WEIGHT") GrowthMetric metric) {
        return ResponseEntity.ok(growthChartService.getGrowthChart(childId, metric));
    }

    @GetMapping("/{childId}/growth/latest")
    @LogExecutionTime
    @Operation(summary = "최근 측정 백분위 조회")
    public ResponseEntity<GrowthPointResponse> getLatestGrowth(
            @PathVariable Long childId,
            @RequestParam(defaultValue = "WEIGHT") GrowthMetric metric) {
        return growthChartService.getLatestPercentile(childId, metric)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    // 형제자매 통합 조회
    @GetMapping("/overview")
    @Operation(summary = "자녀 통합 현황", description = "모든 자녀의 접종·대기·다자녀 혜택을 한 번에 조회")
    public ResponseEntity<SiblingOverviewResponse> overview() {
        return ResponseEntity.ok(siblingOverviewService.getOverview());
    }
}
