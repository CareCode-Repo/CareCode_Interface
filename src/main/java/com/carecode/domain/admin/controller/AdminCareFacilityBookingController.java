package com.carecode.domain.admin.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.controller.BaseController;
import com.carecode.domain.admin.dto.AdminBookingDetailResponse;
import com.carecode.domain.admin.dto.AdminBookingSearchRequest;
import com.carecode.domain.admin.dto.AdminBookingSearchResponse;
import com.carecode.domain.admin.dto.AdminBookingStatsResponse;
import com.carecode.domain.admin.dto.AdminStatusUpdateRequest;
import com.carecode.domain.admin.service.CareFacilityBookingAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/** 관리자용 육아 시설 예약 관리 API. 접근 제어는 SecurityConfig 의 /api/admin/** → hasRole("ADMIN") 규칙이 담당한다. */
@Slf4j
@RestController
@RequestMapping("/api/admin/facilities/bookings")
@RequiredArgsConstructor
@Tag(name = "어드민 - 시설 예약", description = "관리자 전용 예약 관리 API")
public class AdminCareFacilityBookingController extends BaseController {

    private final CareFacilityBookingAdminService adminBookingService;

    @GetMapping
    @LogExecutionTime
    @Operation(summary = "예약 목록 조회")
    public ResponseEntity<AdminBookingSearchResponse> bookingList(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Long facilityId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {

        AdminBookingSearchRequest request = AdminBookingSearchRequest.builder()
                .page(page)
                .size(size)
                .facilityId(facilityId)
                .status(status)
                .keyword(keyword)
                .build();

        return ResponseEntity.ok(adminBookingService.searchBookings(request));
    }

    @GetMapping("/{bookingId}")
    @LogExecutionTime
    @Operation(summary = "예약 상세 조회")
    public ResponseEntity<AdminBookingDetailResponse> bookingDetail(@PathVariable Long bookingId) {
        return ResponseEntity.ok(adminBookingService.getBookingDetail(bookingId));
    }

    @PatchMapping("/{bookingId}/status")
    @LogExecutionTime
    @Operation(summary = "예약 상태 변경")
    public ResponseEntity<AdminBookingDetailResponse> updateBookingStatus(
            @PathVariable Long bookingId,
            @Valid @RequestBody AdminStatusUpdateRequest request) {

        adminBookingService.updateBookingStatus(bookingId, request);
        return ResponseEntity.ok(adminBookingService.getBookingDetail(bookingId));
    }

    @DeleteMapping("/{bookingId}")
    @LogExecutionTime
    @Operation(summary = "예약 삭제")
    public ResponseEntity<Void> deleteBooking(@PathVariable Long bookingId) {
        adminBookingService.deleteBooking(bookingId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/statistics")
    @LogExecutionTime
    @Operation(summary = "예약 통계 조회")
    public ResponseEntity<AdminBookingStatsResponse> bookingStatistics() {
        return ResponseEntity.ok(adminBookingService.getBookingStats());
    }

    @GetMapping("/today")
    @LogExecutionTime
    @Operation(summary = "오늘의 예약 조회")
    public ResponseEntity<AdminBookingSearchResponse> todayBookings(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ResponseEntity.ok(adminBookingService.searchBookings(todayRequest(page, size)));
    }

    @GetMapping("/facility/{facilityId}")
    @LogExecutionTime
    @Operation(summary = "시설별 예약 조회")
    public ResponseEntity<AdminBookingSearchResponse> facilityBookings(
            @PathVariable Long facilityId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        AdminBookingSearchRequest request = AdminBookingSearchRequest.builder()
                .facilityId(facilityId)
                .page(page)
                .size(size)
                .build();

        return ResponseEntity.ok(adminBookingService.searchBookings(request));
    }

    @GetMapping("/status/{status}")
    @LogExecutionTime
    @Operation(summary = "상태별 예약 조회")
    public ResponseEntity<AdminBookingSearchResponse> statusBookings(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        AdminBookingSearchRequest request = AdminBookingSearchRequest.builder()
                .status(status)
                .page(page)
                .size(size)
                .build();

        return ResponseEntity.ok(adminBookingService.searchBookings(request));
    }

    @GetMapping("/dashboard")
    @LogExecutionTime
    @Operation(summary = "예약 대시보드 요약", description = "통계, 최근 예약, 오늘의 예약을 함께 반환")
    public ResponseEntity<Map<String, Object>> bookingDashboard() {
        AdminBookingSearchRequest recentRequest = AdminBookingSearchRequest.builder()
                .page(0)
                .size(10)
                .build();

        return ResponseEntity.ok(Map.of(
                "stats", adminBookingService.getBookingStats(),
                "recentBookings", adminBookingService.searchBookings(recentRequest).getBookings(),
                "todayBookings", adminBookingService.searchBookings(todayRequest(0, 5)).getBookings()
        ));
    }

    private AdminBookingSearchRequest todayRequest(Integer page, Integer size) {
        AdminBookingSearchRequest request = AdminBookingSearchRequest.builder()
                .page(page)
                .size(size)
                .build();
        LocalDate today = LocalDate.now();
        request.setStartDate(today.atStartOfDay());
        request.setEndDate(today.atTime(23, 59, 59));
        return request;
    }
}
