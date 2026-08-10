package com.carecode.core.scheduler;

import com.carecode.domain.careFacility.entity.CareFacilityBooking;
import com.carecode.domain.careFacility.repository.CareFacilityBookingRepository;
import com.carecode.domain.notification.entity.Notification;
import com.carecode.domain.notification.service.NotificationCreationService;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** 시설 예약 전날 리마인더. */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingReminderScheduler {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("M월 d일 HH:mm");

    private final CareFacilityBookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final NotificationCreationService notificationCreationService;

    /** 매일 오후 6시에 다음날 예약을 안내한다. */
    @Scheduled(cron = "${app.scheduler.booking.cron:0 0 18 * * *}", zone = "Asia/Seoul")
    @Transactional(readOnly = true)
    public void sendBookingReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        List<CareFacilityBooking> bookings = bookingRepository.findBookingsBetween(
                tomorrow.atStartOfDay(), tomorrow.plusDays(1).atStartOfDay());

        int sent = 0;
        for (CareFacilityBooking booking : bookings) {
            // 취소된 예약은 안내하지 않는다.
            if (booking.getStatus() == CareFacilityBooking.BookingStatus.CANCELLED) {
                continue;
            }
            try {
                User user = userRepository.findByUserId(booking.getUserId()).orElse(null);
                if (user == null) {
                    continue;
                }

                String facilityName = booking.getFacility() != null ? booking.getFacility().getName() : "시설";
                String message = String.format("내일 %s 예약이 있습니다. (%s, %s)",
                        facilityName,
                        booking.getStartTime().format(TIME_FORMAT),
                        booking.getChildName() != null ? booking.getChildName() : "");

                notificationCreationService.createAndSend(
                        user, Notification.NotificationType.SYSTEM, "예약 안내", message);
                sent++;
            } catch (Exception e) {
                log.error("예약 리마인더 발송 실패 - bookingId={}", booking.getId(), e);
            }
        }

        log.info("예약 리마인더 발송 완료 - 대상={}, 성공={}", bookings.size(), sent);
    }
}
