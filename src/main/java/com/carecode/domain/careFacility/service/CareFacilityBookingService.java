package com.carecode.domain.careFacility.service;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.exception.CareServiceException;
import com.carecode.domain.careFacility.dto.response.BookingResponse;
import com.carecode.domain.careFacility.dto.request.CreateBookingRequest;
import com.carecode.domain.careFacility.dto.request.UpdateBookingRequest;
import com.carecode.domain.careFacility.entity.CareFacility;
import com.carecode.domain.careFacility.entity.CareFacilityBooking;
import com.carecode.domain.careFacility.repository.CareFacilityBookingRepository;
import com.carecode.domain.careFacility.repository.CareFacilityRepository;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 육아 시설 예약 서비스 클래스
 * 시설 방문 및 상담 예약 기능을 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CareFacilityBookingService {

    private final CareFacilityBookingRepository bookingRepository;
    private final CareFacilityRepository careFacilityRepository;
    private final UserRepository userRepository;

    /**
     * 예약 생성
     */
    @LogExecutionTime
    @Transactional
    public BookingResponse createBooking(Long facilityId, CreateBookingRequest request, UserDetails userDetails) {
        // 시설 조회
        CareFacility careFacility = careFacilityRepository.findById(facilityId)
                .orElseThrow(() -> new CareServiceException("시설을 찾을 수 없습니다: " + facilityId));

        // 사용자 조회
        User user = userRepository.findByUserId(userDetails.getUsername())
                .orElseThrow(() -> new CareServiceException("사용자를 찾을 수 없습니다: " + userDetails.getUsername()));

        // 예약 시간 중복 확인
        validateBookingTime(facilityId, request.getStartTime());

        // 예약 생성
        CareFacilityBooking booking = new CareFacilityBooking(
                null, // id는 자동 생성
                careFacility,
                user.getUserId(),
                request.getChildName(),
                request.getChildAge(),
                request.getParentName(),
                request.getParentPhone(),
                CareFacilityBooking.BookingType.valueOf(request.getBookingType()),
                CareFacilityBooking.BookingStatus.PENDING,
                request.getStartTime(),
                request.getEndTime(),
                request.getSpecialRequirements(),
                request.getNotes(),
                null, // cancellationReason
                null, // cancelledAt
                null, // actualStartTime
                null, // actualEndTime
                null, // createdAt
                null  // updatedAt
        );

        CareFacilityBooking savedBooking = bookingRepository.save(booking);
        return convertToDto(savedBooking);
    }

    /**
     * 예약 조회
     */
    @LogExecutionTime
    public BookingResponse getBookingById(Long bookingId, UserDetails userDetails) {
        CareFacilityBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new CareServiceException("예약을 찾을 수 없습니다: " + bookingId));

        // 사용자 권한 확인 (예약자 본인 또는 관리자만 조회 가능)
        if (!booking.getUserId().equals(userDetails.getUsername())) {
            throw new CareServiceException("예약을 조회할 권한이 없습니다.");
        }

        return convertToDto(booking);
    }

    /**
     * 사용자별 예약 목록 조회
     */
    @LogExecutionTime
    public List<BookingResponse> getUserBookings(UserDetails userDetails) {
        User user = userRepository.findByUserId(userDetails.getUsername())
                .orElseThrow(() -> new CareServiceException("사용자를 찾을 수 없습니다: " + userDetails.getUsername()));

        List<CareFacilityBooking> bookings = bookingRepository.findByUserIdOrderByStartTimeDesc(user.getUserId());

        return bookings.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 시설별 예약 목록 조회
     */
    @LogExecutionTime
    public List<BookingResponse> getFacilityBookings(Long facilityId) {
        List<CareFacilityBooking> bookings = bookingRepository.findByFacilityIdOrderByStartTimeAsc(facilityId);

        return bookings.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 예약 상태 업데이트
     */
    @LogExecutionTime
    @Transactional
    public BookingResponse updateBookingStatus(Long bookingId, String status, UserDetails userDetails) {
            CareFacilityBooking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new CareServiceException("예약을 찾을 수 없습니다: " + bookingId));
            
            // 사용자 권한 확인 (예약자 본인 또는 관리자만 상태 변경 가능)
            if (!booking.getUserId().equals(userDetails.getUsername())) {
                throw new CareServiceException("예약 상태를 변경할 권한이 없습니다.");
            }
            
            CareFacilityBooking.BookingStatus newStatus = CareFacilityBooking.BookingStatus.valueOf(status);
            
            switch (newStatus) {
                case CONFIRMED -> booking.confirm();
                case COMPLETED -> booking.complete();
                case CANCELLED -> booking.cancel("사용자에 의해 취소됨");
                default -> booking.setStatus(newStatus);
            }
            
            CareFacilityBooking savedBooking = bookingRepository.save(booking);
            return convertToDto(savedBooking);
    }

    /**
     * 예약 취소
     */
    @LogExecutionTime
    @Transactional
    public void cancelBooking(Long bookingId, UserDetails userDetails) {
        CareFacilityBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new CareServiceException("예약을 찾을 수 없습니다: " + bookingId));

        // 사용자 권한 확인 (예약자 본인 또는 관리자만 취소 가능)
        if (!booking.getUserId().equals(userDetails.getUsername())) {
            throw new CareServiceException("예약을 취소할 권한이 없습니다.");
        }

        if (booking.getStatus() == CareFacilityBooking.BookingStatus.CANCELLED) {
            throw new CareServiceException("이미 취소된 예약입니다.");
        }

        if (booking.getStatus() == CareFacilityBooking.BookingStatus.COMPLETED) {
            throw new CareServiceException("이미 완료된 예약은 취소할 수 없습니다.");
        }

        booking.cancel("사용자에 의해 취소됨");
        bookingRepository.save(booking);
    }

    /**
     * 예약 수정
     */
    @LogExecutionTime
    @Transactional
    public BookingResponse updateBooking(Long bookingId, UpdateBookingRequest request, UserDetails userDetails) {
        CareFacilityBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new CareServiceException("예약을 찾을 수 없습니다: " + bookingId));

        // 사용자 권한 확인 (예약자 본인만 수정 가능)
        if (!booking.getUserId().equals(userDetails.getUsername())) {
            throw new CareServiceException("예약을 수정할 권한이 없습니다.");
        }

        if (booking.getStatus() == CareFacilityBooking.BookingStatus.CANCELLED) {
            throw new CareServiceException("취소된 예약은 수정할 수 없습니다.");
        }

        if (booking.getStatus() == CareFacilityBooking.BookingStatus.COMPLETED) {
            throw new CareServiceException("완료된 예약은 수정할 수 없습니다.");
        }

        // 예약 시간 변경 시 중복 확인
        if (!booking.getStartTime().equals(request.getStartTime())) {
            validateBookingTime(booking.getFacility().getId(), request.getStartTime());
        }

        // 예약 정보 업데이트
        booking.setBookingType(CareFacilityBooking.BookingType.valueOf(request.getBookingType()));
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());
        booking.setChildName(request.getChildName());
        booking.setChildAge(request.getChildAge());
        booking.setParentName(request.getParentName());
        booking.setParentPhone(request.getParentPhone());
        booking.setSpecialRequirements(request.getSpecialRequirements());
        booking.setNotes(request.getNotes());

        CareFacilityBooking savedBooking = bookingRepository.save(booking);
        return convertToDto(savedBooking);
    }

    /**
     * 오늘 예약 목록 조회
     */
    @LogExecutionTime
    public List<BookingResponse> getTodayBookings() {
        List<CareFacilityBooking> bookings = bookingRepository.findTodayBookings();

        return bookings.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 시설별 오늘 예약 목록 조회
     */
    @LogExecutionTime
    public List<BookingResponse> getTodayBookingsByFacility(Long facilityId) {
        List<CareFacilityBooking> bookings = bookingRepository.findTodayBookingsByFacility(facilityId);

        return bookings.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 예약 시간 중복 확인
     */
    private void validateBookingTime(Long facilityId, LocalDateTime scheduledDateTime) {
        // 예약 시간 전후 1시간 동안 중복 예약이 있는지 확인
        LocalDateTime startTime = scheduledDateTime.minusHours(1);
        LocalDateTime endTime = scheduledDateTime.plusHours(1);
        
        List<CareFacilityBooking> conflictingBookings = bookingRepository.findByFacilityIdAndStartTimeBetween(facilityId, startTime, endTime);
        
        if (!conflictingBookings.isEmpty()) {
            throw new CareServiceException("해당 시간에 이미 예약이 있습니다. 다른 시간을 선택해주세요.");
        }
    }

    /**
     * 예약 타입별 예약 목록 조회
     */
    @LogExecutionTime
    public List<BookingResponse> getBookingsByType(CareFacilityBooking.BookingType bookingType) {
        log.info("예약 타입별 예약 목록 조회: 타입={}", bookingType);
        
        List<CareFacilityBooking> bookings = bookingRepository.findByBookingTypeOrderByStartTimeAsc(bookingType);
        
        return bookings.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 취소된 예약 목록 조회 (기간별)
     */
    @LogExecutionTime
    public List<BookingResponse> getCancelledBookings(CareFacilityBooking.BookingStatus status, 
                                                       LocalDateTime startDate, 
                                                       LocalDateTime endDate) {
        log.info("취소된 예약 목록 조회: 상태={}, 시작일={}, 종료일={}", status, startDate, endDate);
        
        List<CareFacilityBooking> bookings = bookingRepository.findByStatusAndCancelledAtBetweenOrderByCancelledAtDesc(
                status, startDate, endDate);
        
        return bookings.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 관리자용 복합 검색
     */
    @LogExecutionTime
    public Page<BookingResponse> searchBookings(Long facilityId,
                                                 String userId,
                                                 CareFacilityBooking.BookingType bookingType,
                                                 CareFacilityBooking.BookingStatus status,
                                                 LocalDateTime startDate,
                                                 LocalDateTime endDate,
                                                 String keyword,
                                                 int page,
                                                 int size) {
        log.info("예약 복합 검색: 시설ID={}, 사용자ID={}, 타입={}, 상태={}, 키워드={}", 
                facilityId, userId, bookingType, status, keyword);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<CareFacilityBooking> bookings = bookingRepository.findBySearchCriteria(
                facilityId, userId, bookingType, status, startDate, endDate, keyword, pageable);
        
        return bookings.map(this::convertToDto);
    }

    /**
     * DTO 변환
     */
    private BookingResponse convertToDto(CareFacilityBooking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .facilityId(booking.getFacility().getId())
                .facilityName(booking.getFacility().getName())
                .userId(booking.getUserId())
                .childName(booking.getChildName())
                .childAge(booking.getChildAge())
                .parentName(booking.getParentName())
                .parentPhone(booking.getParentPhone())
                .bookingType(booking.getBookingType().name())
                .status(booking.getStatus().name())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .specialRequirements(booking.getSpecialRequirements())
                .notes(booking.getNotes())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
} 