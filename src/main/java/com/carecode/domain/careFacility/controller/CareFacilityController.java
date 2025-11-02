package com.carecode.domain.careFacility.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.annotation.RequireAuthentication;
import com.carecode.core.annotation.ValidateLocation;
import com.carecode.core.annotation.ValidateChildAge;
import com.carecode.core.controller.BaseController;
import com.carecode.domain.careFacility.dto.CareFacilityRequest;
import com.carecode.domain.careFacility.dto.CareFacilityResponse;
import com.carecode.domain.careFacility.dto.CareFacilityBookingDto;
import com.carecode.domain.careFacility.entity.FacilityType;
import com.carecode.domain.careFacility.service.CareFacilityService;
import com.carecode.domain.careFacility.service.CareFacilityBookingService;
import com.carecode.domain.careFacility.app.CareFacilityFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import com.carecode.core.handler.ApiSuccess;
import java.util.Date;

/**
 * 육아 시설 컨트롤러
 * 육아 시설 관련 REST API 엔드포인트 제공
 */
@Slf4j
@RestController
@RequestMapping("/facilities")
@RequiredArgsConstructor
@Tag(name = "육아 시설", description = "육아 시설 관련 API")
public class CareFacilityController extends BaseController {
    
    private final CareFacilityFacade careFacilityFacade;
    
    /**
     * 전체 시설 목록 조회
     */
    @GetMapping
    @LogExecutionTime
    @Operation(summary = "전체 시설 목록 조회", description = "등록된 모든 육아 시설 목록을 조회합니다.")
    public ResponseEntity<List<CareFacilityResponse.CareFacility>> getAllFacilities() {
        log.info("전체 시설 목록 조회 요청");
        List<CareFacilityResponse.CareFacility> facilities = careFacilityFacade.getAllCareFacilities();
        return ResponseEntity.ok(facilities);
    }
    
    /**
     * 시설 ID로 시설 조회
     */
    @GetMapping("/{id}")
    @LogExecutionTime
    @Operation(summary = "시설 상세 조회", description = "시설 ID로 특정 육아 시설의 상세 정보를 조회합니다.")
    public ResponseEntity<CareFacilityResponse.CareFacility> getFacilityById(
            @Parameter(description = "시설 ID", required = true) @PathVariable Long id) {
        log.info("시설 조회 요청 - ID: {}", id);
        CareFacilityResponse.CareFacility facility = careFacilityFacade.getCareFacilityById(id);
        return ResponseEntity.ok(facility);
    }
    
    /**
     * 시설 유형별 조회
     */
    @GetMapping("/type/{facilityType}")
    @LogExecutionTime
    @Operation(summary = "시설 유형별 조회", description = "특정 유형의 육아 시설 목록을 조회합니다.")
    public ResponseEntity<List<CareFacilityResponse.CareFacility>> getFacilitiesByType(
            @Parameter(description = "시설 유형 (KINDERGARTEN: 유치원, DAYCARE: 어린이집, PLAYGROUP: 놀이방, NURSERY: 보육원, OTHER: 기타)", required = true) 
            @PathVariable FacilityType facilityType) {
        log.info("시설 유형별 조회 요청 - 유형: {}", facilityType);
        List<CareFacilityResponse.CareFacility> facilities = careFacilityFacade.getCareFacilitiesByType(facilityType);
        return ResponseEntity.ok(facilities);
    }
    
    /**
     * 지역별 시설 조회
     */
    @GetMapping("/location/{location}")
    @LogExecutionTime
    @ValidateLocation
    @Operation(summary = "지역별 시설 조회", description = "특정 지역의 육아 시설 목록을 조회합니다.")
    public ResponseEntity<List<CareFacilityResponse.CareFacility>> getFacilitiesByLocation(
            @Parameter(description = "지역명", required = true) @PathVariable String location) {
        log.info("지역별 시설 조회 요청 - 지역: {}", location);
        List<CareFacilityResponse.CareFacility> facilities = careFacilityFacade.getCareFacilitiesByLocation(location);
        return ResponseEntity.ok(facilities);
    }
    
    /**
     * 연령대별 시설 조회
     */
    @GetMapping("/age")
    @LogExecutionTime
    @ValidateChildAge
    @Operation(summary = "연령대별 시설 조회", description = "특정 연령대에 적합한 육아 시설 목록을 조회합니다.")
    public ResponseEntity<List<CareFacilityResponse.CareFacility>> getFacilitiesByAgeRange(
            @Parameter(description = "최소 연령", required = true) @RequestParam Integer minAge, 
            @Parameter(description = "최대 연령", required = true) @RequestParam Integer maxAge) {
        log.info("연령대별 시설 조회 요청 - 최소연령: {}, 최대연령: {}", minAge, maxAge);
        List<CareFacilityResponse.CareFacility> facilities = careFacilityFacade.getCareFacilitiesByAgeRange(minAge, maxAge);
        return ResponseEntity.ok(facilities);
    }
    
    /**
     * 운영 시간별 시설 조회
     */
    @GetMapping("/operating-hours")
    @LogExecutionTime
    @Operation(summary = "운영 시간별 시설 조회", description = "특정 운영 시간을 가진 육아 시설 목록을 조회합니다.")
    public ResponseEntity<List<CareFacilityResponse.CareFacility>> getFacilitiesByOperatingHours(
            @Parameter(description = "운영 시간", required = true) @RequestParam String operatingHours) {
        log.info("운영 시간별 시설 조회 요청 - 운영시간: {}", operatingHours);
        List<CareFacilityResponse.CareFacility> facilities = careFacilityFacade.getCareFacilitiesByOperatingHours(operatingHours);
        return ResponseEntity.ok(facilities);
    }
    
    /**
     * 인기 시설 조회 (평점 기준)
     */
    @GetMapping("/popular")
    @LogExecutionTime
    @Operation(summary = "인기 시설 조회", description = "평점 기준으로 인기 있는 육아 시설 목록을 조회합니다.")
    public ResponseEntity<List<CareFacilityResponse.CareFacility>> getPopularFacilities(
            @Parameter(description = "조회할 시설 수", example = "10") @RequestParam(defaultValue = "10") Integer limit) {
        log.info("인기 시설 조회 요청 - 제한: {}", limit);
        List<CareFacilityResponse.CareFacility> facilities = careFacilityFacade.getPopularCareFacilities(limit);
        return ResponseEntity.ok(facilities);
    }
    
    /**
     * 신규 시설 조회
     */
    @GetMapping("/new")
    @LogExecutionTime
    @Operation(summary = "신규 시설 조회", description = "최근 등록된 육아 시설 목록을 조회합니다.")
    public ResponseEntity<List<CareFacilityResponse.CareFacility>> getNewFacilities(
            @Parameter(description = "조회할 시설 수", example = "10") @RequestParam(defaultValue = "10") Integer limit) {
        log.info("신규 시설 조회 요청 - 제한: {}", limit);
        List<CareFacilityResponse.CareFacility> facilities = careFacilityFacade.getNewCareFacilities(limit);
        return ResponseEntity.ok(facilities);
    }
    
    /**
     * 위치 기반 시설 검색 (반경 내)
     */
    @GetMapping("/radius")
    @LogExecutionTime
    @ValidateLocation
    @Operation(summary = "반경 내 시설 검색", description = "특정 위치 기준 반경 내의 육아 시설을 검색합니다.")
    public ResponseEntity<List<CareFacilityResponse.CareFacility>> getFacilitiesWithinRadius(
            @Parameter(description = "위도", required = true) @RequestParam Double latitude,
            @Parameter(description = "경도", required = true) @RequestParam Double longitude,
            @Parameter(description = "반경 (km)", required = true) @RequestParam Double radius) {
        log.info("반경 내 시설 검색 요청 - 위도: {}, 경도: {}, 반경: {}", latitude, longitude, radius);
        List<CareFacilityResponse.CareFacility> facilities = careFacilityFacade.getCareFacilitiesWithinRadius(latitude, longitude, radius);
        return ResponseEntity.ok(facilities);
    }
    
    /**
     * 복합 조건으로 시설 검색 (페이징)
     */
    @PostMapping("/search")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "복합 조건 시설 검색", description = "다양한 조건으로 육아 시설을 검색합니다.")
    public ResponseEntity<CareFacilityResponse.CareFacilityList> searchFacilities(
            @Parameter(description = "검색 조건", required = true) @RequestBody CareFacilityRequest.Search requestDto) {
        log.info("복합 조건 시설 검색 요청 - {}", requestDto);
        CareFacilityResponse.CareFacilityList response = careFacilityFacade.searchCareFacilities(requestDto);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 시설 조회수 증가
     */
    @PostMapping("/{id}/view")
    @LogExecutionTime
    @Operation(summary = "시설 조회수 증가", description = "특정 시설의 조회수를 증가시킵니다.")
    public ResponseEntity<ApiSuccess> incrementViewCount(
            @Parameter(description = "시설 ID", required = true) @PathVariable Long id) {
        log.info("시설 조회수 증가 요청 - ID: {}", id);
        careFacilityFacade.incrementViewCount(id);
        return ResponseEntity.ok(ApiSuccess.builder().timestamp(new Date()).message(SUCCESS_MESSAGE).build());
    }
    
    /**
     * 시설 평점 업데이트
     */
    @PostMapping("/{id}/rating")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "시설 평점 업데이트", description = "특정 시설의 평점을 업데이트합니다.")
    public ResponseEntity<ApiSuccess> updateRating(
            @Parameter(description = "시설 ID", required = true) @PathVariable Long id,
            @Parameter(description = "평점 (0.0 ~ 5.0)", required = true) @RequestParam Double rating) {
        log.info("시설 평점 업데이트 요청 - ID: {}, 평점: {}", id, rating);
        careFacilityFacade.updateRating(id, rating);
        return ResponseEntity.ok(ApiSuccess.builder().timestamp(new Date()).message(SUCCESS_MESSAGE).build());
    }
    
    /**
     * 시설 통계 조회
     */
    @GetMapping("/statistics")
    @LogExecutionTime
    @Operation(summary = "시설 통계 조회", description = "육아 시설 관련 통계 정보를 조회합니다.")
    public ResponseEntity<CareFacilityResponse.CareFacilityStats> getFacilityStatistics() {
        log.info("시설 통계 조회 요청");
        CareFacilityResponse.CareFacilityStats stats = careFacilityFacade.getFacilityStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 예약 생성
     */
    @PostMapping("/{facilityId}/bookings")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "시설 예약 생성", description = "특정 육아 시설에 예약을 생성합니다.")
    public ResponseEntity<CareFacilityBookingDto.BookingResponse> createBooking(
            @Parameter(description = "시설 ID", required = true) @PathVariable Long facilityId,
            @Parameter(description = "예약 정보", required = true) @RequestBody CareFacilityBookingDto.CreateBookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        CareFacilityBookingDto.BookingResponse booking = careFacilityFacade.createBooking(facilityId, request, userDetails);
        return ResponseEntity.ok(booking);
    }
    
    /**
     * 예약 상세 조회
     */
    @GetMapping("/bookings/{bookingId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "예약 상세 조회", description = "특정 예약의 상세 정보를 조회합니다.")
    public ResponseEntity<CareFacilityBookingDto.BookingResponse> getBookingById(
            @Parameter(description = "예약 ID", required = true) @PathVariable Long bookingId,
            @AuthenticationPrincipal UserDetails userDetails) {
        CareFacilityBookingDto.BookingResponse booking = careFacilityFacade.getBookingById(bookingId, userDetails);
        return ResponseEntity.ok(booking);
    }
    
    /**
     * 사용자별 예약 목록 조회
     */
    @GetMapping("/bookings/user")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "사용자별 예약 목록 조회", description = "현재 로그인한 사용자의 예약 목록을 조회합니다.")
    public ResponseEntity<List<CareFacilityBookingDto.BookingResponse>> getUserBookings(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<CareFacilityBookingDto.BookingResponse> bookings = careFacilityFacade.getUserBookings(userDetails);
        return ResponseEntity.ok(bookings);
    }
    
    /**
     * 시설별 예약 목록 조회
     */
    @GetMapping("/{facilityId}/bookings")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "시설별 예약 목록 조회", description = "특정 시설의 예약 목록을 조회합니다.")
    public ResponseEntity<List<CareFacilityBookingDto.BookingResponse>> getFacilityBookings(
            @Parameter(description = "시설 ID", required = true) @PathVariable Long facilityId) {
        List<CareFacilityBookingDto.BookingResponse> bookings = careFacilityFacade.getFacilityBookings(facilityId);
        return ResponseEntity.ok(bookings);
    }
    
    /**
     * 예약 상태 업데이트
     */
    @PutMapping("/bookings/{bookingId}/status")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "예약 상태 업데이트", description = "예약의 상태를 업데이트합니다.")
    public ResponseEntity<CareFacilityBookingDto.BookingResponse> updateBookingStatus(
            @Parameter(description = "예약 ID", required = true) @PathVariable Long bookingId,
            @Parameter(description = "새로운 상태", required = true) @RequestParam String status,
            @AuthenticationPrincipal UserDetails userDetails) {
        CareFacilityBookingDto.BookingResponse booking = careFacilityFacade.updateBookingStatus(bookingId, status, userDetails);
        return ResponseEntity.ok(booking);
    }
    
    /**
     * 예약 취소
     */
    @DeleteMapping("/bookings/{bookingId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "예약 취소", description = "예약을 취소합니다.")
    public ResponseEntity<ApiSuccess> cancelBooking(
            @Parameter(description = "예약 ID", required = true) @PathVariable Long bookingId,
            @AuthenticationPrincipal UserDetails userDetails) {
        careFacilityFacade.cancelBooking(bookingId, userDetails);
        return ResponseEntity.ok(ApiSuccess.builder().timestamp(new Date()).message("예약이 성공적으로 취소되었습니다.").build());
    }
    
    /**
     * 예약 수정
     */
    @PutMapping("/bookings/{bookingId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "예약 수정", description = "기존 예약 정보를 수정합니다.")
    public ResponseEntity<CareFacilityBookingDto.BookingResponse> updateBooking(
            @Parameter(description = "예약 ID", required = true) @PathVariable Long bookingId,
            @Parameter(description = "수정할 예약 정보", required = true) @RequestBody CareFacilityBookingDto.UpdateBookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        CareFacilityBookingDto.BookingResponse booking = careFacilityFacade.updateBooking(bookingId, request, userDetails);
        return ResponseEntity.ok(booking);
    }
    
    /**
     * 오늘의 예약 조회
     */
    @GetMapping("/bookings/today")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "오늘의 예약 조회", description = "오늘 날짜의 예약 목록을 조회합니다.")
    public ResponseEntity<List<CareFacilityBookingDto.BookingResponse>> getTodayBookings() {
        log.info("오늘의 예약 조회 요청");
        List<CareFacilityBookingDto.BookingResponse> bookings = careFacilityFacade.getTodayBookings();
        return ResponseEntity.ok(bookings);
    }
    
    /**
     * 시설별 오늘의 예약 조회
     */
    @GetMapping("/{facilityId}/bookings/today")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "시설별 오늘의 예약 조회", description = "특정 시설의 오늘 예약 목록을 조회합니다.")
    public ResponseEntity<List<CareFacilityBookingDto.BookingResponse>> getTodayBookingsByFacility(
            @Parameter(description = "시설 ID", required = true) @PathVariable Long facilityId) {
        List<CareFacilityBookingDto.BookingResponse> bookings = careFacilityFacade.getTodayBookingsByFacility(facilityId);
        return ResponseEntity.ok(bookings);
    }
} 