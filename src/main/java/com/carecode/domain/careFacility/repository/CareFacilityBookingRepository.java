package com.carecode.domain.careFacility.repository;

import com.carecode.domain.careFacility.entity.CareFacilityBooking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 육아 시설 예약 리포지토리
 */
@Repository
public interface CareFacilityBookingRepository extends JpaRepository<CareFacilityBooking, Long> {

    // 사용자별 예약 목록 조회
    List<CareFacilityBooking> findByUserIdOrderByStartTimeDesc(String userId);

    // 사용자별 예약 목록 조회 (페이징)
    Page<CareFacilityBooking> findByUserIdOrderByStartTimeDesc(String userId, Pageable pageable);

    // 시설별 예약 목록 조회
    List<CareFacilityBooking> findByFacilityIdOrderByStartTimeAsc(Long facilityId);

    // 상태별 예약 목록 조회
    List<CareFacilityBooking> findByStatusOrderByStartTimeAsc(CareFacilityBooking.BookingStatus status);

    // 사용자별 상태별 예약 목록 조회
    List<CareFacilityBooking> findByUserIdAndStatusOrderByStartTimeDesc(String userId, CareFacilityBooking.BookingStatus status);


    // 시설별 상태별 예약 목록 조회
    List<CareFacilityBooking> findByFacilityIdAndStatusOrderByStartTimeAsc(Long facilityId, CareFacilityBooking.BookingStatus status);

    // 예약 타입별 예약 목록 조회
    List<CareFacilityBooking> findByBookingTypeOrderByStartTimeAsc(CareFacilityBooking.BookingType bookingType);

    // 날짜 범위별 예약 목록 조회
    @Query("SELECT cb FROM CareFacilityBooking cb WHERE cb.startTime BETWEEN :startDate AND :endDate ORDER BY cb.startTime ASC")
    List<CareFacilityBooking> findByStartTimeBetween(@Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);


    // 시설별 날짜 범위별 예약 목록 조회
    @Query("SELECT cb FROM CareFacilityBooking cb WHERE cb.facility.id = :facilityId AND cb.startTime BETWEEN :startDate AND :endDate ORDER BY cb.startTime ASC")
    List<CareFacilityBooking> findByFacilityIdAndStartTimeBetween(@Param("facilityId") Long facilityId,
                                                                  @Param("startDate") LocalDateTime startDate,
                                                                  @Param("endDate") LocalDateTime endDate);

    // 사용자별 예약 수 조회
    long countByUserId(String userId);

    // 시설별 예약 수 조회
    long countByFacilityId(Long facilityId);

    // 상태별 예약 수 조회
    long countByStatus(CareFacilityBooking.BookingStatus status);

    // 사용자별 상태별 예약 수 조회
    long countByUserIdAndStatus(String userId, CareFacilityBooking.BookingStatus status);

    // 시설별 상태별 예약 수 조회
    long countByFacilityIdAndStatus(Long facilityId, CareFacilityBooking.BookingStatus status);

    // 예약 타입별 예약 수 조회
    long countByBookingType(CareFacilityBooking.BookingType bookingType);

    // 오늘 예약 목록 조회
    @Query("SELECT cb FROM CareFacilityBooking cb WHERE DATE(cb.startTime) = CURRENT_DATE ORDER BY cb.startTime ASC")
    List<CareFacilityBooking> findTodayBookings();

    // 시설별 오늘 예약 목록 조회
    @Query("SELECT cb FROM CareFacilityBooking cb WHERE cb.facility.id = :facilityId AND DATE(cb.startTime) = CURRENT_DATE ORDER BY cb.startTime ASC")
    List<CareFacilityBooking> findTodayBookingsByFacility(@Param("facilityId") Long facilityId);

    // 사용자별 오늘 예약 목록 조회
    @Query("SELECT cb FROM CareFacilityBooking cb WHERE cb.userId = :userId AND DATE(cb.startTime) = CURRENT_DATE ORDER BY cb.startTime ASC")
    List<CareFacilityBooking> findTodayBookingsByUser(@Param("userId") String userId);

    // 오늘 예약 수 조회
    @Query("SELECT COUNT(cb) FROM CareFacilityBooking cb WHERE DATE(cb.startTime) = CURRENT_DATE")
    long countTodayBookings();

    // 이번 주 예약 수 조회
    @Query("SELECT COUNT(cb) FROM CareFacilityBooking cb WHERE cb.startTime >= :weekStart AND cb.startTime <= :weekEnd")
    long countThisWeekBookings(@Param("weekStart") LocalDateTime weekStart, @Param("weekEnd") LocalDateTime weekEnd);

    // 이번 달 예약 수 조회
    @Query("SELECT COUNT(cb) FROM CareFacilityBooking cb WHERE cb.startTime >= :monthStart AND cb.startTime <= :monthEnd")
    long countThisMonthBookings(@Param("monthStart") LocalDateTime monthStart, @Param("monthEnd") LocalDateTime monthEnd);

    // 취소된 예약 목록 조회
    List<CareFacilityBooking> findByStatusAndCancelledAtBetweenOrderByCancelledAtDesc(CareFacilityBooking.BookingStatus status, 
                                                                                     LocalDateTime startDate, 
                                                                                     LocalDateTime endDate);

    // 관리자용 복합 검색
    @Query("SELECT cb FROM CareFacilityBooking cb WHERE " +
           "(:facilityId IS NULL OR cb.facility.id = :facilityId) AND " +
           "(:userId IS NULL OR cb.userId = :userId) AND " +
           "(:bookingType IS NULL OR cb.bookingType = :bookingType) AND " +
           "(:status IS NULL OR cb.status = :status) AND " +
           "(:startDate IS NULL OR cb.startTime >= :startDate) AND " +
           "(:endDate IS NULL OR cb.startTime <= :endDate) AND " +
           "(:keyword IS NULL OR cb.facility.name LIKE %:keyword% OR cb.userId LIKE %:keyword% OR cb.childName LIKE %:keyword%) " +
           "ORDER BY cb.createdAt DESC")
    Page<CareFacilityBooking> findBySearchCriteria(@Param("facilityId") Long facilityId,
                                                  @Param("userId") String userId,
                                                  @Param("bookingType") CareFacilityBooking.BookingType bookingType,
                                                  @Param("status") CareFacilityBooking.BookingStatus status,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate,
                                                  @Param("keyword") String keyword,
                                                  Pageable pageable);


    // 시설별 예약 통계
    @Query("SELECT cb.facility.id, cb.facility.name, COUNT(cb) FROM CareFacilityBooking cb GROUP BY cb.facility.id, cb.facility.name ORDER BY COUNT(cb) DESC")
    List<Object[]> getFacilityBookingStats();

    // 일별 예약 수 통계
    @Query("SELECT DATE(cb.startTime), COUNT(cb) FROM CareFacilityBooking cb WHERE cb.startTime >= :startDate GROUP BY DATE(cb.startTime) ORDER BY DATE(cb.startTime) DESC")
    List<Object[]> getDailyBookingCounts(@Param("startDate") LocalDate startDate);

    // 이번 주 예약 수 조회 (기본값)
    default long countThisWeekBookings() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekStart = now.toLocalDate().atStartOfDay().with(java.time.DayOfWeek.MONDAY);
        LocalDateTime weekEnd = weekStart.plusDays(7).minusNanos(1);
        return countThisWeekBookings(weekStart, weekEnd);
    }

    // 이번 달 예약 수 조회 (기본값)
    default long countThisMonthBookings() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = monthStart.plusMonths(1).minusNanos(1);
        return countThisMonthBookings(monthStart, monthEnd);
    }

    // 일별 예약 수 조회 (기본값 - 최근 30일)
    default List<Object[]> getDailyBookingCounts() {
        LocalDate startDate = LocalDate.now().minusDays(30);
        return getDailyBookingCounts(startDate);
    }
} 