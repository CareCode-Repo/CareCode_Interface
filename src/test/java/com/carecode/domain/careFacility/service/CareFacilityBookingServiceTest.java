package com.carecode.domain.careFacility.service;

import com.carecode.core.exception.CareServiceException;
import com.carecode.domain.careFacility.dto.request.CreateBookingRequest;
import com.carecode.domain.careFacility.entity.CareFacility;
import com.carecode.domain.careFacility.entity.CareFacilityBooking;
import com.carecode.domain.careFacility.repository.CareFacilityBookingRepository;
import com.carecode.domain.careFacility.repository.CareFacilityRepository;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.entity.UserRole;
import com.carecode.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 예약 겹침 검증 회귀 테스트. 이전 구현은 시작 시각 ±1시간만 비교해서 (1) 기존 예약의 종료 시각을 무시했고, (2) 취소된 예약도 충돌로 셌으며 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("시설 예약 - 겹침 검증")
class CareFacilityBookingServiceTest {

    private static final Long FACILITY_ID = 10L;

    @Mock private CareFacilityBookingRepository bookingRepository;
    @Mock private CareFacilityRepository careFacilityRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserDetails userDetails;

    @InjectMocks private CareFacilityBookingService bookingService;

    private CareFacility facility;

    @BeforeEach
    void setUp() {
        facility = CareFacility.builder()
                .id(FACILITY_ID)
                .name("행복 어린이집")
                .capacity(2)
                .build();

        User user = User.builder()
                .id(1L).userId("u-1").email("parent@example.com")
                .name("보호자").role(UserRole.PARENT)
                .build();

        when(userDetails.getUsername()).thenReturn("u-1");
        when(userRepository.findByUserId("u-1")).thenReturn(Optional.of(user));
        when(careFacilityRepository.findById(FACILITY_ID)).thenReturn(Optional.of(facility));
        when(bookingRepository.save(any(CareFacilityBooking.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("겹치는 예약이 정원 미만이면 예약된다")
    void allowsBookingWhenBelowCapacity() {
        when(bookingRepository.countOverlappingBookings(eq(FACILITY_ID), any(), any(), isNull()))
                .thenReturn(1L); // 정원 2 중 1건 사용

        assertThatCode(() -> bookingService.createBooking(FACILITY_ID, futureRequest(), userDetails))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("겹치는 예약이 정원에 도달하면 거부한다")
    void rejectsBookingWhenCapacityReached() {
        when(bookingRepository.countOverlappingBookings(eq(FACILITY_ID), any(), any(), isNull()))
                .thenReturn(2L); // 정원 2 모두 사용

        assertThatThrownBy(() -> bookingService.createBooking(FACILITY_ID, futureRequest(), userDetails))
                .isInstanceOf(CareServiceException.class)
                .hasMessageContaining("예약 가능한 자리가 없습니다");

        verify(bookingRepository, never()).save(any(CareFacilityBooking.class));
    }

    @Test
    @DisplayName("겹침 판정에 요청한 시작·종료 시각을 그대로 사용한다")
    void passesRequestedIntervalToOverlapQuery() {
        when(bookingRepository.countOverlappingBookings(eq(FACILITY_ID), any(), any(), isNull()))
                .thenReturn(0L);

        CreateBookingRequest request = futureRequest();
        bookingService.createBooking(FACILITY_ID, request, userDetails);

        ArgumentCaptor<LocalDateTime> start = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> end = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(bookingRepository).countOverlappingBookings(
                eq(FACILITY_ID), start.capture(), end.capture(), isNull());

        // ±1시간 하드코딩이 아니라 요청 구간 그대로여야 한다
        org.assertj.core.api.Assertions.assertThat(start.getValue()).isEqualTo(request.getStartTime());
        org.assertj.core.api.Assertions.assertThat(end.getValue()).isEqualTo(request.getEndTime());
    }

    @Test
    @DisplayName("종료 시각이 시작 시각보다 빠르면 거부한다")
    void rejectsInvertedInterval() {
        CreateBookingRequest request = futureRequest();
        request.setEndTime(request.getStartTime().minusHours(1));

        assertThatThrownBy(() -> bookingService.createBooking(FACILITY_ID, request, userDetails))
                .isInstanceOf(CareServiceException.class)
                .hasMessageContaining("종료 시간은 시작 시간보다");
    }

    @Test
    @DisplayName("과거 시간은 예약할 수 없다")
    void rejectsPastBooking() {
        CreateBookingRequest request = futureRequest();
        request.setStartTime(LocalDateTime.now().minusDays(1));
        request.setEndTime(LocalDateTime.now().minusDays(1).plusHours(2));

        assertThatThrownBy(() -> bookingService.createBooking(FACILITY_ID, request, userDetails))
                .isInstanceOf(CareServiceException.class)
                .hasMessageContaining("과거 시간");
    }

    private CreateBookingRequest futureRequest() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        CreateBookingRequest request = new CreateBookingRequest();
        request.setStartTime(start);
        request.setEndTime(start.plusHours(8));
        request.setBookingType("TEMPORARY");
        request.setChildName("아이");
        request.setChildAge(3);
        request.setParentName("보호자");
        request.setParentPhone("010-0000-0000");
        return request;
    }
}
