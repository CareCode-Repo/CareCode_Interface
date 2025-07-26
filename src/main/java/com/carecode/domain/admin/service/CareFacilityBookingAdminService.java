package com.carecode.domain.admin.service;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.exception.CareServiceException;
import com.carecode.domain.careFacility.dto.CareFacilityBookingDto;
import com.carecode.domain.careFacility.entity.CareFacilityBooking;
import com.carecode.domain.careFacility.repository.CareFacilityBookingRepository;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 관리자용 육아 시설 예약 서비스 클래스
 * 관리자 전용 예약 관리 기능을 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CareFacilityBookingAdminService {

    private final CareFacilityBookingRepository bookingRepository;
    private final UserRepository userRepository;

    /**
     * 전체 예약 목록 조회 (페이징)
     */
    @LogExecutionTime
    public CareFacilityBookingDto.AdminBookingSearchResponse getAllBookings(
            CareFacilityBookingDto.AdminBookingSearchRequest request) {
        log.info("관리자 예약 목록 조회: {}", request);
        
        try {
            Pageable pageable = PageRequest.of(
                    request.getPage() != null ? request.getPage() : 0,
                    request.getSize() != null ? request.getSize() : 20,
                    Sort.by(Sort.Direction.DESC, "createdAt")
            );
            
            Page<CareFacilityBooking> bookingPage = bookingRepository.findAll(pageable);
            
            List<CareFacilityBookingDto.AdminBookingListResponse> bookings = bookingPage.getContent().stream()
                    .map(this::convertToAdminListResponse)
                    .collect(Collectors.toList());
            
            return CareFacilityBookingDto.AdminBookingSearchResponse.builder()
                    .bookings(bookings)
                    .totalElements(bookingPage.getTotalElements())
                    .totalPages(bookingPage.getTotalPages())
                    .currentPage(bookingPage.getNumber())
                    .pageSize(bookingPage.getSize())
                    .hasNext(bookingPage.hasNext())
                    .hasPrevious(bookingPage.hasPrevious())
                    .build();
                    
        } catch (Exception e) {
            log.error("관리자 예약 목록 조회 실패: {}", e.getMessage(), e);
            throw new CareServiceException("예약 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 조건별 예약 검색
     */
    @LogExecutionTime
    public CareFacilityBookingDto.AdminBookingSearchResponse searchBookings(
            CareFacilityBookingDto.AdminBookingSearchRequest request) {
        log.info("관리자 예약 검색: {}", request);
        
        try {
            Pageable pageable = PageRequest.of(
                    request.getPage() != null ? request.getPage() : 0,
                    request.getSize() != null ? request.getSize() : 20,
                    Sort.by(Sort.Direction.DESC, "createdAt")
            );
            
            // String을 enum으로 변환
            CareFacilityBooking.BookingType bookingType = null;
            if (request.getBookingType() != null && !request.getBookingType().trim().isEmpty()) {
                try {
                    bookingType = CareFacilityBooking.BookingType.valueOf(request.getBookingType().toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("유효하지 않은 예약 타입: {}", request.getBookingType());
                }
            }
            
            CareFacilityBooking.BookingStatus status = null;
            if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
                try {
                    status = CareFacilityBooking.BookingStatus.valueOf(request.getStatus().toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("유효하지 않은 예약 상태: {}", request.getStatus());
                }
            }
            
            // 검색 조건에 따른 쿼리 실행
            Page<CareFacilityBooking> bookingPage = bookingRepository.findBySearchCriteria(
                    request.getFacilityId(),
                    request.getUserId(),
                    bookingType,
                    status,
                    request.getStartDate(),
                    request.getEndDate(),
                    request.getKeyword(),
                    pageable
            );
            
            List<CareFacilityBookingDto.AdminBookingListResponse> bookings = bookingPage.getContent().stream()
                    .map(this::convertToAdminListResponse)
                    .collect(Collectors.toList());
            
            return CareFacilityBookingDto.AdminBookingSearchResponse.builder()
                    .bookings(bookings)
                    .totalElements(bookingPage.getTotalElements())
                    .totalPages(bookingPage.getTotalPages())
                    .currentPage(bookingPage.getNumber())
                    .pageSize(bookingPage.getSize())
                    .hasNext(bookingPage.hasNext())
                    .hasPrevious(bookingPage.hasPrevious())
                    .build();
                    
        } catch (Exception e) {
            log.error("관리자 예약 검색 실패: {}", e.getMessage(), e);
            throw new CareServiceException("예약 검색 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 예약 상세 조회
     */
    @LogExecutionTime
    public CareFacilityBookingDto.AdminBookingDetailResponse getBookingDetail(Long bookingId) {
        log.info("관리자 예약 상세 조회: 예약ID={}", bookingId);
        
        try {
            CareFacilityBooking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new CareServiceException("예약을 찾을 수 없습니다: " + bookingId));
            
            return convertToAdminDetailResponse(booking);
        } catch (Exception e) {
            log.error("관리자 예약 상세 조회 실패: {}", e.getMessage(), e);
            throw new CareServiceException("예약 상세 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 예약 상태 변경
     */
    @LogExecutionTime
    @Transactional
    public CareFacilityBookingDto.AdminBookingDetailResponse updateBookingStatus(
            Long bookingId, CareFacilityBookingDto.AdminStatusUpdateRequest request) {
        log.info("관리자 예약 상태 변경: 예약ID={}, 상태={}", bookingId, request.getStatus());
        
        try {
            CareFacilityBooking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new CareServiceException("예약을 찾을 수 없습니다: " + bookingId));
            
            CareFacilityBooking.BookingStatus newStatus = CareFacilityBooking.BookingStatus.valueOf(request.getStatus());
            
            switch (newStatus) {
                case CONFIRMED -> booking.confirm();
                case COMPLETED -> booking.complete();
                case CANCELLED -> booking.cancel(request.getReason() != null ? request.getReason() : "관리자에 의해 취소됨");
                default -> booking.setStatus(newStatus);
            }
            
            CareFacilityBooking savedBooking = bookingRepository.save(booking);
            return convertToAdminDetailResponse(savedBooking);
        } catch (Exception e) {
            log.error("관리자 예약 상태 변경 실패: {}", e.getMessage(), e);
            throw new CareServiceException("예약 상태 변경 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 예약 삭제
     */
    @LogExecutionTime
    @Transactional
    public void deleteBooking(Long bookingId) {
        log.info("관리자 예약 삭제: 예약ID={}", bookingId);
        
        try {
            CareFacilityBooking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new CareServiceException("예약을 찾을 수 없습니다: " + bookingId));
            
            bookingRepository.delete(booking);
        } catch (Exception e) {
            log.error("관리자 예약 삭제 실패: {}", e.getMessage(), e);
            throw new CareServiceException("예약 삭제 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 예약 통계 조회
     */
    @LogExecutionTime
    public CareFacilityBookingDto.AdminBookingStatsResponse getBookingStats() {
        log.info("관리자 예약 통계 조회");
        
        try {
            // 전체 통계
            long totalBookings = bookingRepository.count();
            long pendingBookings = bookingRepository.countByStatus(CareFacilityBooking.BookingStatus.PENDING);
            long confirmedBookings = bookingRepository.countByStatus(CareFacilityBooking.BookingStatus.CONFIRMED);
            long completedBookings = bookingRepository.countByStatus(CareFacilityBooking.BookingStatus.COMPLETED);
            long cancelledBookings = bookingRepository.countByStatus(CareFacilityBooking.BookingStatus.CANCELLED);
            
            // 기간별 통계
            long todayBookings = bookingRepository.countTodayBookings();
            long thisWeekBookings = bookingRepository.countThisWeekBookings();
            long thisMonthBookings = bookingRepository.countThisMonthBookings();
            
            // 완료율 계산
            double averageCompletionRate = totalBookings > 0 ? 
                    (double) completedBookings / totalBookings * 100 : 0.0;
            
            // 상태별 분포
            List<CareFacilityBookingDto.StatusDistribution> statusDistribution = getStatusDistribution();
            
            // 타입별 분포
            List<CareFacilityBookingDto.TypeDistribution> typeDistribution = getTypeDistribution();
            
            // 시설별 분포
            List<CareFacilityBookingDto.FacilityDistribution> facilityDistribution = getFacilityDistribution();
            
            // 일별 예약 수
            List<CareFacilityBookingDto.DailyBookingCount> dailyBookingCounts = getDailyBookingCounts();
            
            return CareFacilityBookingDto.AdminBookingStatsResponse.builder()
                    .totalBookings(totalBookings)
                    .pendingBookings(pendingBookings)
                    .confirmedBookings(confirmedBookings)
                    .completedBookings(completedBookings)
                    .cancelledBookings(cancelledBookings)
                    .todayBookings(todayBookings)
                    .thisWeekBookings(thisWeekBookings)
                    .thisMonthBookings(thisMonthBookings)
                    .averageCompletionRate(averageCompletionRate)
                    .statusDistribution(statusDistribution)
                    .typeDistribution(typeDistribution)
                    .facilityDistribution(facilityDistribution)
                    .dailyBookingCounts(dailyBookingCounts)
                    .build();
                    
        } catch (Exception e) {
            log.error("관리자 예약 통계 조회 실패: {}", e.getMessage(), e);
            throw new CareServiceException("예약 통계 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 상태별 분포 조회
     */
    private List<CareFacilityBookingDto.StatusDistribution> getStatusDistribution() {
        long total = bookingRepository.count();
        if (total == 0) return List.of();
        
        return List.of(
                createStatusDistribution("PENDING", "대기중", bookingRepository.countByStatus(CareFacilityBooking.BookingStatus.PENDING), total),
                createStatusDistribution("CONFIRMED", "확정", bookingRepository.countByStatus(CareFacilityBooking.BookingStatus.CONFIRMED), total),
                createStatusDistribution("COMPLETED", "완료", bookingRepository.countByStatus(CareFacilityBooking.BookingStatus.COMPLETED), total),
                createStatusDistribution("CANCELLED", "취소됨", bookingRepository.countByStatus(CareFacilityBooking.BookingStatus.CANCELLED), total)
        );
    }

    /**
     * 타입별 분포 조회
     */
    private List<CareFacilityBookingDto.TypeDistribution> getTypeDistribution() {
        long total = bookingRepository.count();
        if (total == 0) return List.of();
        
        return List.of(
                createTypeDistribution("VISIT", "방문", bookingRepository.countByBookingType(CareFacilityBooking.BookingType.VISIT), total),
                createTypeDistribution("REGULAR", "정기", bookingRepository.countByBookingType(CareFacilityBooking.BookingType.REGULAR), total),
                createTypeDistribution("TEMPORARY", "임시", bookingRepository.countByBookingType(CareFacilityBooking.BookingType.TEMPORARY), total)
        );
    }

    /**
     * 시설별 분포 조회
     */
    private List<CareFacilityBookingDto.FacilityDistribution> getFacilityDistribution() {
        List<Object[]> facilityStats = bookingRepository.getFacilityBookingStats();
        long total = bookingRepository.count();
        
        return facilityStats.stream()
                .map(stat -> CareFacilityBookingDto.FacilityDistribution.builder()
                        .facilityId((Long) stat[0])
                        .facilityName((String) stat[1])
                        .count((Long) stat[2])
                        .percentage(total > 0 ? (double) (Long) stat[2] / total * 100 : 0.0)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 일별 예약 수 조회
     */
    private List<CareFacilityBookingDto.DailyBookingCount> getDailyBookingCounts() {
        List<Object[]> dailyStats = bookingRepository.getDailyBookingCounts();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        return dailyStats.stream()
                .map(stat -> CareFacilityBookingDto.DailyBookingCount.builder()
                        .date(((LocalDate) stat[0]).format(formatter))
                        .count((Long) stat[1])
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 상태별 분포 생성
     */
    private CareFacilityBookingDto.StatusDistribution createStatusDistribution(String status, String display, long count, long total) {
        return CareFacilityBookingDto.StatusDistribution.builder()
                .status(status)
                .statusDisplay(display)
                .count(count)
                .percentage(total > 0 ? (double) count / total * 100 : 0.0)
                .build();
    }

    /**
     * 타입별 분포 생성
     */
    private CareFacilityBookingDto.TypeDistribution createTypeDistribution(String type, String display, long count, long total) {
        return CareFacilityBookingDto.TypeDistribution.builder()
                .bookingType(type)
                .bookingTypeDisplay(display)
                .count(count)
                .percentage(total > 0 ? (double) count / total * 100 : 0.0)
                .build();
    }

    /**
     * 관리자용 목록 응답 변환
     */
    private CareFacilityBookingDto.AdminBookingListResponse convertToAdminListResponse(CareFacilityBooking booking) {
        return CareFacilityBookingDto.AdminBookingListResponse.builder()
                .id(booking.getId())
                .facilityId(booking.getFacility().getId())
                .facilityName(booking.getFacility().getName())
                .userId(booking.getUserId())
                .userName(getUserName(booking.getUserId()))
                .childName(booking.getChildName())
                .bookingType(booking.getBookingType().name())
                .status(booking.getStatus().name())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .createdAt(booking.getCreatedAt())
                .build();
    }

    /**
     * 관리자용 상세 응답 변환
     */
    private CareFacilityBookingDto.AdminBookingDetailResponse convertToAdminDetailResponse(CareFacilityBooking booking) {
        User user = userRepository.findByUserId(booking.getUserId()).orElse(null);
        
        return CareFacilityBookingDto.AdminBookingDetailResponse.builder()
                .id(booking.getId())
                .facilityId(booking.getFacility().getId())
                .facilityName(booking.getFacility().getName())
                .facilityAddress(booking.getFacility().getAddress())
                .facilityPhone(booking.getFacility().getPhone())
                .userId(booking.getUserId())
                .userName(user != null ? user.getName() : "알 수 없음")
                .userEmail(user != null ? user.getEmail() : "알 수 없음")
                .childName(booking.getChildName())
                .childAge(booking.getChildAge())
                .parentName(booking.getParentName())
                .parentPhone(booking.getParentPhone())
                .bookingType(booking.getBookingType().name())
                .bookingTypeDisplay(booking.getBookingType().getDisplayName())
                .status(booking.getStatus().name())
                .statusDisplay(booking.getStatus().getDisplayName())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .actualStartTime(booking.getActualStartTime())
                .actualEndTime(booking.getActualEndTime())
                .specialRequirements(booking.getSpecialRequirements())
                .notes(booking.getNotes())
                .cancellationReason(booking.getCancellationReason())
                .cancelledAt(booking.getCancelledAt())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }

    /**
     * 사용자명 조회
     */
    private String getUserName(String userId) {
        return userRepository.findByUserId(userId)
                .map(User::getName)
                .orElse("알 수 없음");
    }
} 