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

    /** 시설에 정원 정보가 없을 때 적용할 동시 예약 허용 건수. */
    private static final int DEFAULT_CONCURRENT_BOOKING_LIMIT = 1;

    private final CareFacilityBookingRepository bookingRepository;
    private final CareFacilityRepository careFacilityRepository;
    private final UserRepository userRepository;


    // 예약 생성

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
        validateBookingTime(careFacility, request.getStartTime(), request.getEndTime(), null);

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


    // 예약 조회

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


    // 사용자별 예약 목록 조회

    @LogExecutionTime
    public List<BookingResponse> getUserBookings(UserDetails userDetails) {
        User user = userRepository.findByUserId(userDetails.getUsername())
                .orElseThrow(() -> new CareServiceException("사용자를 찾을 수 없습니다: " + userDetails.getUsername()));

        List<CareFacilityBooking> bookings = bookingRepository.findByUserIdOrderByStartTimeDesc(user.getUserId());

        return bookings.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }


    // 시설별 예약 목록 조회

    @LogExecutionTime
    public List<BookingResponse> getFacilityBookings(Long facilityId) {
        List<CareFacilityBooking> bookings = bookingRepository.findByFacilityIdOrderByStartTimeAsc(facilityId);

        return bookings.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }


    // 예약 상태 업데이트

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


    // 예약 취소

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


    // 예약 수정

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

        // 예약 시간이 바뀌면 다시 검증한다. 종료 시각만 바뀌어도 겹침 여부가 달라지므로 둘 다 비교한다.
        boolean timeChanged = !booking.getStartTime().equals(request.getStartTime())
                || !java.util.Objects.equals(booking.getEndTime(), request.getEndTime());
        if (timeChanged) {
            validateBookingTime(booking.getFacility(), request.getStartTime(), request.getEndTime(), booking.getId());
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


    // 오늘 예약 목록 조회

    @LogExecutionTime
    public List<BookingResponse> getTodayBookings() {
        List<CareFacilityBooking> bookings = bookingRepository.findTodayBookings();

        return bookings.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }


    // 시설별 오늘 예약 목록 조회

    @LogExecutionTime
    public List<BookingResponse> getTodayBookingsByFacility(Long facilityId) {
        List<CareFacilityBooking> bookings = bookingRepository.findTodayBookingsByFacility(facilityId);

        return bookings.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }


    // 예약 시간 중복 확인

    /**
     * 예약 가능 여부 검증.
     *
     * <p>이전 구현은 시작 시각 ±1시간만 비교해서 (1) 기존 예약의 종료 시각을 무시했고,
     * (2) 취소된 예약도 충돌로 셌으며, (3) 시설 정원과 무관하게 1건만 있어도 막았다.
     * 여기서는 실제 구간 겹침을 보고, 겹치는 유효 예약 수가 정원 미만일 때만 허용한다.
     */
    private void validateBookingTime(CareFacility facility,
                                     LocalDateTime startTime,
                                     LocalDateTime endTime,
                                     Long excludeBookingId) {
        if (startTime == null || endTime == null) {
            throw new CareServiceException("예약 시작/종료 시간은 필수입니다.");
        }
        if (!endTime.isAfter(startTime)) {
            throw new CareServiceException("예약 종료 시간은 시작 시간보다 뒤여야 합니다.");
        }
        if (startTime.isBefore(LocalDateTime.now())) {
            throw new CareServiceException("과거 시간으로는 예약할 수 없습니다.");
        }

        long overlapping = bookingRepository.countOverlappingBookings(
                facility.getId(), startTime, endTime, excludeBookingId);

        int capacity = facility.getCapacity() != null && facility.getCapacity() > 0
                ? facility.getCapacity()
                : DEFAULT_CONCURRENT_BOOKING_LIMIT;

        if (overlapping >= capacity) {
            throw new CareServiceException("해당 시간에 예약 가능한 자리가 없습니다. 다른 시간을 선택해주세요.");
        }
    }

    // DTO 변환
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