package com.carecode.domain.admin.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.annotation.RequireAuthentication;
import com.carecode.core.annotation.RequireAdminRole;
import com.carecode.core.controller.BaseController;
import com.carecode.core.exception.CareServiceException;
import com.carecode.domain.careFacility.dto.CareFacilityBookingDto;
import com.carecode.domain.admin.service.CareFacilityBookingAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 관리자용 육아 시설 예약 관리 컨트롤러
 * Thymeleaf 기반 관리자 대시보드 제공
 */
@Slf4j
@Controller
@RequestMapping("/admin/facilities/bookings")
@RequiredArgsConstructor
@RequireAdminRole
public class AdminCareFacilityBookingController extends BaseController {

    private final CareFacilityBookingAdminService adminBookingService;

    /**
     * 예약 목록 페이지
     */
    @GetMapping
    @LogExecutionTime
    public String bookingList(@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer size,
                              @RequestParam(required = false) Long facilityId, @RequestParam(required = false) String status,
                              @RequestParam(required = false) String keyword, Model model) {
        CareFacilityBookingDto.AdminBookingSearchRequest request = CareFacilityBookingDto.AdminBookingSearchRequest.builder()
            .page(page)
            .size(size)
            .facilityId(facilityId)
            .status(status)
            .keyword(keyword)
            .build();
            
        CareFacilityBookingDto.AdminBookingSearchResponse response = adminBookingService.searchBookings(request);
            
        model.addAttribute("bookings", response.getBookings());
        model.addAttribute("pagination", response);
        model.addAttribute("searchRequest", request);

        return "admin/facilities/bookings/list";
    }

    /**
     * 예약 상세 페이지
     */
    @GetMapping("/{bookingId}")
    @LogExecutionTime
    public String bookingDetail(@PathVariable Long bookingId, Model model) {
        CareFacilityBookingDto.AdminBookingDetailResponse booking = adminBookingService.getBookingDetail(bookingId);
        model.addAttribute("booking", booking);
        return "admin/facilities/bookings/detail";
    }

    /**
     * 예약 상태 변경
     */
    @PostMapping("/{bookingId}/status")
    @LogExecutionTime
    public String updateBookingStatus(@PathVariable Long bookingId, @RequestParam String status,
                                      @RequestParam(required = false) String reason,
                                      RedirectAttributes redirectAttributes) {

        CareFacilityBookingDto.AdminStatusUpdateRequest request = CareFacilityBookingDto.AdminStatusUpdateRequest.builder()
            .status(status)
            .reason(reason)
            .build();

        adminBookingService.updateBookingStatus(bookingId, request);
        redirectAttributes.addFlashAttribute("success", "예약 상태가 성공적으로 변경되었습니다.");

        return "redirect:/admin/facilities/bookings/" + bookingId;
    }

    /**
     * 예약 삭제
     */
    @PostMapping("/{bookingId}/delete")
    @LogExecutionTime
    public String deleteBooking(@PathVariable Long bookingId, RedirectAttributes redirectAttributes) {
        adminBookingService.deleteBooking(bookingId);
        redirectAttributes.addFlashAttribute("success", "예약이 성공적으로 삭제되었습니다.");

        return "redirect:/admin/facilities/bookings";
    }

    /**
     * 예약 통계 페이지
     */
    @GetMapping("/statistics")
    @LogExecutionTime
    public String bookingStatistics(Model model) {
        CareFacilityBookingDto.AdminBookingStatsResponse stats = adminBookingService.getBookingStats();
        model.addAttribute("stats", stats);
        return "admin/facilities/bookings/statistics";
    }

    /**
     * 오늘의 예약 페이지
     */
    @GetMapping("/today")
    @LogExecutionTime
    public String todayBookings(@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer size, Model model) {
        CareFacilityBookingDto.AdminBookingSearchRequest request = CareFacilityBookingDto.AdminBookingSearchRequest.builder()
                .page(page)
                .size(size)
                .build();
            
        // 오늘 날짜로 검색
        request.setStartDate(java.time.LocalDateTime.now().toLocalDate().atStartOfDay());
        request.setEndDate(java.time.LocalDateTime.now().toLocalDate().atTime(23, 59, 59));
            
        CareFacilityBookingDto.AdminBookingSearchResponse response = adminBookingService.searchBookings(request);
            
        model.addAttribute("bookings", response.getBookings());
        model.addAttribute("pagination", response);
        model.addAttribute("isToday", true);
            
        return "admin/facilities/bookings/today";
    }

    /**
     * 시설별 예약 페이지
     */
    @GetMapping("/facility/{facilityId}")
    @LogExecutionTime
    public String facilityBookings(@PathVariable Long facilityId, @RequestParam(defaultValue = "0") Integer page,
                                   @RequestParam(defaultValue = "20") Integer size, Model model) {
        CareFacilityBookingDto.AdminBookingSearchRequest request = CareFacilityBookingDto.AdminBookingSearchRequest.builder()
                .facilityId(facilityId)
                .page(page)
                .size(size)
                .build();

        CareFacilityBookingDto.AdminBookingSearchResponse response = adminBookingService.searchBookings(request);

        model.addAttribute("bookings", response.getBookings());
        model.addAttribute("pagination", response);
        model.addAttribute("facilityId", facilityId);

        return "admin/facilities/bookings/facility";
    }

    /**
     * 상태별 예약 페이지
     */
    @GetMapping("/status/{status}")
    @LogExecutionTime
    public String statusBookings(@PathVariable String status, @RequestParam(defaultValue = "0") Integer page,
                                 @RequestParam(defaultValue = "20") Integer size, Model model) {
        CareFacilityBookingDto.AdminBookingSearchRequest request = CareFacilityBookingDto.AdminBookingSearchRequest.builder()
                .status(status)
                .page(page)
                .size(size)
                .build();
            
        CareFacilityBookingDto.AdminBookingSearchResponse response = adminBookingService.searchBookings(request);
            
        model.addAttribute("bookings", response.getBookings());
        model.addAttribute("pagination", response);
        model.addAttribute("status", status);
            
        return "admin/facilities/bookings/status";
    }

    /**
     * 예약 생성 페이지 (관리자가 직접 예약 생성)
     */
    @GetMapping("/create")
    @LogExecutionTime
    public String createBookingForm(Model model) {
        model.addAttribute("bookingRequest", new CareFacilityBookingDto.CreateBookingRequest());
        return "admin/facilities/bookings/create";
    }

    /**
     * 예약 수정 페이지
     */
    @GetMapping("/{bookingId}/edit")
    @LogExecutionTime
    public String editBookingForm(@PathVariable Long bookingId, Model model) {
        CareFacilityBookingDto.AdminBookingDetailResponse booking = adminBookingService.getBookingDetail(bookingId);
        model.addAttribute("booking", booking);
        return "admin/facilities/bookings/edit";
    }

    /**
     * 대시보드 메인 페이지 (예약 요약)
     */
    @GetMapping("/dashboard")
    @LogExecutionTime
    public String bookingDashboard(Model model) {
        // 통계 정보
        CareFacilityBookingDto.AdminBookingStatsResponse stats = adminBookingService.getBookingStats();
        model.addAttribute("stats", stats);

        // 최근 예약 목록 (최근 10개)
        CareFacilityBookingDto.AdminBookingSearchRequest recentRequest = CareFacilityBookingDto.AdminBookingSearchRequest.builder()
                .page(0)
                .size(10)
                .build();
        CareFacilityBookingDto.AdminBookingSearchResponse recentBookings = adminBookingService.searchBookings(recentRequest);
        model.addAttribute("recentBookings", recentBookings.getBookings());

        // 오늘의 예약 (최근 5개)
        CareFacilityBookingDto.AdminBookingSearchRequest todayRequest = CareFacilityBookingDto.AdminBookingSearchRequest.builder()
                .page(0)
                .size(5)
                .build();
        todayRequest.setStartDate(java.time.LocalDateTime.now().toLocalDate().atStartOfDay());
        todayRequest.setEndDate(java.time.LocalDateTime.now().toLocalDate().atTime(23, 59, 59));

        CareFacilityBookingDto.AdminBookingSearchResponse todayBookings = adminBookingService.searchBookings(todayRequest);
        model.addAttribute("todayBookings", todayBookings.getBookings());

        return "admin/facilities/bookings/dashboard";
    }
}