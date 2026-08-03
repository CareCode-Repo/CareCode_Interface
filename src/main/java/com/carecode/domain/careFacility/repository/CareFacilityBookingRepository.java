package com.carecode.domain.careFacility.repository;

import com.carecode.domain.careFacility.entity.CareFacilityBooking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    // 시설별 예약 목록 조회
    List<CareFacilityBooking> findByFacilityIdOrderByStartTimeAsc(Long facilityId);

    // 시설별 날짜 범위별 예약 목록 조회
    @Query("SELECT cb FROM CareFacilityBooking cb WHERE cb.facility.id = :facilityId AND cb.startTime BETWEEN :startDate AND :endDate ORDER BY cb.startTime ASC")
    List<CareFacilityBooking> findByFacilityIdAndStartTimeBetween(@Param("facilityId") Long facilityId,
                                                                  @Param("startDate") LocalDateTime startDate,
                                                                  @Param("endDate") LocalDateTime endDate);

    /**
     * 주어진 구간과 실제로 겹치는 유효 예약 수.
     *
     * <p>겹침 판정은 {@code 기존.start < 신규.end AND 기존.end > 신규.start} 이다.
     * 시작 시각만 비교하면 09:00~18:00 종일 예약이 있어도 11:00 예약이 통과된다.
     * 취소/거절된 예약은 자리를 차지하지 않으므로 제외한다.
     *
     * @param excludeBookingId 예약 수정 시 자기 자신을 충돌로 세지 않기 위한 제외 ID (신규 생성 시 null)
     */
    // PESSIMISTIC_WRITE: 검사와 저장 사이에 다른 트랜잭션이 같은 구간을 예약하는 경쟁 조건을 막는다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT COUNT(cb) FROM CareFacilityBooking cb " +
           "WHERE cb.facility.id = :facilityId " +
           "AND cb.status <> com.carecode.domain.careFacility.entity.CareFacilityBooking.BookingStatus.CANCELLED " +
           "AND (:excludeBookingId IS NULL OR cb.id <> :excludeBookingId) " +
           "AND cb.startTime < :newEnd AND cb.endTime > :newStart")
    long countOverlappingBookings(@Param("facilityId") Long facilityId,
                                  @Param("newStart") LocalDateTime newStart,
                                  @Param("newEnd") LocalDateTime newEnd,
                                  @Param("excludeBookingId") Long excludeBookingId);

    // 상태별 예약 수 조회
    long countByStatus(CareFacilityBooking.BookingStatus status);

    // 예약 타입별 예약 수 조회
    long countByBookingType(CareFacilityBooking.BookingType bookingType);

    // 오늘 예약 목록 조회
    // 주의: HQL 에 DATE(...) 함수는 없다. Hibernate 6 에서는 파싱 단계에서 실패해
    // 리포지토리 빈 생성이 깨지고 애플리케이션이 기동되지 않는다.
    // 범위 비교로 바꾸면 startTime 인덱스도 그대로 탈 수 있다.
    @Query("SELECT cb FROM CareFacilityBooking cb " +
           "WHERE cb.startTime >= :dayStart AND cb.startTime < :dayEnd ORDER BY cb.startTime ASC")
    List<CareFacilityBooking> findBookingsBetween(@Param("dayStart") LocalDateTime dayStart,
                                                  @Param("dayEnd") LocalDateTime dayEnd);

    // 시설별 오늘 예약 목록 조회
    @Query("SELECT cb FROM CareFacilityBooking cb WHERE cb.facility.id = :facilityId " +
           "AND cb.startTime >= :dayStart AND cb.startTime < :dayEnd ORDER BY cb.startTime ASC")
    List<CareFacilityBooking> findBookingsByFacilityBetween(@Param("facilityId") Long facilityId,
                                                            @Param("dayStart") LocalDateTime dayStart,
                                                            @Param("dayEnd") LocalDateTime dayEnd);

    // 오늘 예약 수 조회
    @Query("SELECT COUNT(cb) FROM CareFacilityBooking cb " +
           "WHERE cb.startTime >= :dayStart AND cb.startTime < :dayEnd")
    long countBookingsBetween(@Param("dayStart") LocalDateTime dayStart,
                              @Param("dayEnd") LocalDateTime dayEnd);

    default List<CareFacilityBooking> findTodayBookings() {
        LocalDate today = LocalDate.now();
        return findBookingsBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay());
    }

    default List<CareFacilityBooking> findTodayBookingsByFacility(Long facilityId) {
        LocalDate today = LocalDate.now();
        return findBookingsByFacilityBetween(facilityId, today.atStartOfDay(), today.plusDays(1).atStartOfDay());
    }

    default long countTodayBookings() {
        LocalDate today = LocalDate.now();
        return countBookingsBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay());
    }

    // 이번 주 예약 수 조회
    @Query("SELECT COUNT(cb) FROM CareFacilityBooking cb WHERE cb.startTime >= :weekStart AND cb.startTime <= :weekEnd")
    long countThisWeekBookings(@Param("weekStart") LocalDateTime weekStart, @Param("weekEnd") LocalDateTime weekEnd);

    // 이번 달 예약 수 조회
    @Query("SELECT COUNT(cb) FROM CareFacilityBooking cb WHERE cb.startTime >= :monthStart AND cb.startTime <= :monthEnd")
    long countThisMonthBookings(@Param("monthStart") LocalDateTime monthStart, @Param("monthEnd") LocalDateTime monthEnd);

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
    // DATE(...) 대신 HQL 표준인 cast(... as date) 를 쓴다. 파라미터 타입도 비교 대상과 맞춘다.
    @Query("SELECT cast(cb.startTime as date), COUNT(cb) FROM CareFacilityBooking cb " +
           "WHERE cb.startTime >= :startDate " +
           "GROUP BY cast(cb.startTime as date) ORDER BY cast(cb.startTime as date) DESC")
    List<Object[]> getDailyBookingCounts(@Param("startDate") LocalDateTime startDate);

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
        return getDailyBookingCounts(LocalDate.now().minusDays(30).atStartOfDay());
    }
} 