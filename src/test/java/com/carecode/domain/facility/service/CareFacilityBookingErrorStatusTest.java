package com.carecode.domain.facility.service;

import com.carecode.core.exception.BusinessException;
import com.carecode.core.exception.ErrorCode;
import com.carecode.domain.facility.dto.request.CreateBookingRequest;
import com.carecode.domain.facility.entity.CareFacility;
import com.carecode.domain.facility.entity.CareFacilityBooking;
import com.carecode.domain.facility.repository.CareFacilityBookingRepository;
import com.carecode.domain.facility.repository.CareFacilityRepository;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 예약이 거절됐을 때 <b>누구의 잘못인지</b> 상태 코드가 말해야 한다.
 *
 * <p>예약 거절은 전부 {@code CareServiceException} 을 코드 없이 던졌다. 전역 핸들러는 그 예외의
 * 코드 문자열로 상태를 정하는데, 코드를 주지 않으면 {@code CARE_SERVICE_ERROR} 가 되고 매핑에
 * 없으므로 <b>500</b> 이 나간다. 그래서 다음이 전부 "서버 오류" 로 보고됐다.
 *
 * <ul>
 *   <li>이미 취소한 예약을 또 취소 — 사용자가 두 번 눌렀을 뿐이다</li>
 *   <li>끝나는 시각이 시작보다 앞 — 입력이 잘못됐다</li>
 *   <li>그 시간에 자리가 없음 — 정상적인 거절이다. 다른 시간을 고르면 된다</li>
 * </ul>
 *
 * <p>500 은 클라이언트에게 "서버가 고장났으니 다시 해 보라" 는 뜻이고, 운영에는 장애로 쌓인다.
 * 정원이 찬 인기 시설일수록 더 많이 쌓이므로 <b>정상 동작이 장애 지표를 오염시킨다.</b>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("예약 거절의 책임 소재")
class CareFacilityBookingErrorStatusTest {

    private static final String USER_ID = "u-1";
    private static final Long FACILITY_ID = 10L;

    @Mock CareFacilityBookingRepository bookingRepository;
    @Mock CareFacilityRepository careFacilityRepository;
    @Mock UserRepository userRepository;

    @InjectMocks CareFacilityBookingService bookingService;

    private CareFacility facility;

    @BeforeEach
    void setUp() {
        facility = CareFacility.builder()
                .id(FACILITY_ID)
                .name("햇살어린이집")
                .capacity(1)
                .build();
    }

    private CareFacilityBooking bookingWith(CareFacilityBooking.BookingStatus status) {
        CareFacilityBooking booking = new CareFacilityBooking();
        booking.setId(1L);
        booking.setUserId(USER_ID);
        booking.setFacility(facility);
        booking.setStatus(status);
        booking.setStartTime(LocalDateTime.now().plusDays(1));
        booking.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        return booking;
    }

    /** 상태 때문에 지금 할 수 없는 요청은 409 다. 요청 자체는 멀쩡하고 대상의 상태가 맞지 않다. */
    @Nested
    @DisplayName("상태가 맞지 않아 거절할 때는 409")
    class StateConflicts {

        @Test
        @DisplayName("이미 취소한 예약을 또 취소하면 409 - 두 번 누른 것뿐이다")
        void cancellingTwiceIsConflict() {
            when(bookingRepository.findById(1L))
                    .thenReturn(Optional.of(bookingWith(CareFacilityBooking.BookingStatus.CANCELLED)));

            assertThatThrownBy(() -> bookingService.cancelBooking(1L, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getHttpStatus())
                            .isEqualTo(HttpStatus.CONFLICT));
        }

        @Test
        @DisplayName("완료된 예약은 취소할 수 없고 409 다")
        void cancellingCompletedIsConflict() {
            when(bookingRepository.findById(1L))
                    .thenReturn(Optional.of(bookingWith(CareFacilityBooking.BookingStatus.COMPLETED)));

            assertThatThrownBy(() -> bookingService.cancelBooking(1L, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getHttpStatus())
                            .isEqualTo(HttpStatus.CONFLICT));
        }
    }

    /** 입력이 잘못된 요청은 400 이다. 서버가 다시 시도해도 같은 결과다. */
    @Nested
    @DisplayName("입력이 잘못됐을 때는 400")
    class BadInput {

        private CreateBookingRequest request(LocalDateTime start, LocalDateTime end) {
            CreateBookingRequest r = new CreateBookingRequest();
            r.setBookingType("VISIT");
            r.setStartTime(start);
            r.setEndTime(end);
            r.setChildName("아이");
            r.setChildAge(3);
            return r;
        }

        private void givenFacilityAndUser() {
            when(careFacilityRepository.findById(FACILITY_ID)).thenReturn(Optional.of(facility));
            lenient().when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(
                    User.builder().id(1L).userId(USER_ID).email("a@b.c").name("보호자").build()));
        }

        @Test
        @DisplayName("끝나는 시각이 시작보다 앞이면 400")
        void endBeforeStartIsBadRequest() {
            givenFacilityAndUser();
            LocalDateTime start = LocalDateTime.now().plusDays(1);

            assertThatThrownBy(() -> bookingService.createBooking(
                    FACILITY_ID, request(start, start.minusHours(1)), USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getHttpStatus())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("과거 시간으로 예약하면 400")
        void pastTimeIsBadRequest() {
            givenFacilityAndUser();
            LocalDateTime start = LocalDateTime.now().minusDays(1);

            assertThatThrownBy(() -> bookingService.createBooking(
                    FACILITY_ID, request(start, start.plusHours(1)), USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getHttpStatus())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        /**
         * 자리가 없는 것은 입력 오류가 아니라 <b>정상적인 거절</b>이다. 409 로 돌려줘야
         * 클라이언트가 "다른 시간을 고르라" 고 안내할 수 있다.
         */
        @Test
        @DisplayName("그 시간에 자리가 없으면 409 - 정상 거절이지 장애가 아니다")
        void noCapacityIsConflict() {
            givenFacilityAndUser();
            LocalDateTime start = LocalDateTime.now().plusDays(1);
            when(bookingRepository.countOverlappingBookings(anyLong(), any(), any(), any()))
                    .thenReturn(1L);

            assertThatThrownBy(() -> bookingService.createBooking(
                    FACILITY_ID, request(start, start.plusHours(1)), USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.BOOKING_ALREADY_EXISTS));
        }
    }
}
