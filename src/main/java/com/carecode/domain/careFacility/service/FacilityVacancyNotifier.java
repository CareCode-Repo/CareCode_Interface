package com.carecode.domain.careFacility.service;

import com.carecode.core.analytics.EventLogger;
import com.carecode.core.analytics.EventType;
import com.carecode.domain.careFacility.entity.CareFacility;
import com.carecode.domain.careFacility.entity.FacilityCapacitySnapshot;
import com.carecode.domain.careFacility.entity.FacilityWaitlist;
import com.carecode.domain.careFacility.repository.CareFacilityRepository;
import com.carecode.domain.careFacility.repository.FacilityCapacitySnapshotRepository;
import com.carecode.domain.careFacility.repository.FacilityWaitlistRepository;
import com.carecode.domain.notification.entity.Notification;
import com.carecode.domain.notification.repository.NotificationRepository;
import com.carecode.domain.notification.sender.NotificationDispatcher;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 대기 걸어둔 시설에 자리가 나면 알린다.
 *
 * <p>정원 스냅샷은 자리가 났다는 사실을 알고 있었고 대기 명단도 있었는데 둘이 이어져 있지
 * 않아서, 지금까지는 사용자가 직접 들어와 확인해야만 알 수 있었다. 학부모가 이 앱을 다시 열
 * 가장 강한 이유가 바로 이 알림이다.
 *
 * <p>판단 기준은 "빈자리가 늘었는가" 다. 빈자리가 계속 있는 시설은 이미 알고 있을 테니
 * 새로 생긴 자리만 알린다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FacilityVacancyNotifier {

    /** 직전 관측을 찾기 위해 거슬러 올라갈 기간. 동기화는 주 단위라 넉넉히 잡는다. */
    private static final int LOOKBACK_DAYS = 30;

    /** 같은 사람에게 다시 빈자리를 알리기까지의 최소 간격. */
    @Value("${app.facility-vacancy.min-interval-days:14}")
    private int minIntervalDays;

    /** 이 수 이상 늘어야 알린다. 1자리 오르내림까지 알리면 스팸이 된다. */
    @Value("${app.facility-vacancy.min-increase:1}")
    private int minIncrease;

    private final FacilityWaitlistRepository waitlistRepository;
    private final FacilityCapacitySnapshotRepository snapshotRepository;
    private final CareFacilityRepository facilityRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationDispatcher dispatcher;
    private final EventLogger eventLogger;

    @Getter
    public static class VacancyNotifyResult {
        private int facilitiesChecked;
        private int facilitiesWithVacancy;
        private int notificationsSent;

        @Override
        public String toString() {
            return String.format("시설 %d곳 확인, 자리 발생 %d곳, 알림 %d건",
                    facilitiesChecked, facilitiesWithVacancy, notificationsSent);
        }
    }

    @Transactional
    public VacancyNotifyResult notifyNewVacancies() {
        VacancyNotifyResult result = new VacancyNotifyResult();

        List<Long> facilityIds = waitlistRepository.findFacilityIdsWithWaiting();
        if (facilityIds.isEmpty()) {
            return result;
        }

        for (Long facilityId : facilityIds) {
            result.facilitiesChecked++;
            try {
                int sent = notifyIfVacancyAppeared(facilityId);
                if (sent > 0) {
                    result.facilitiesWithVacancy++;
                    result.notificationsSent += sent;
                }
            } catch (Exception e) {
                // 한 시설의 실패가 나머지 대기자들의 알림을 막아서는 안 된다.
                log.warn("빈자리 알림 실패 - facilityId={}, 사유={}", facilityId, e.getMessage());
            }
        }

        log.info("빈자리 알림 - {}", result);
        return result;
    }

    private int notifyIfVacancyAppeared(Long facilityId) {
        List<FacilityCapacitySnapshot> history = snapshotRepository.findHistory(
                facilityId, LocalDate.now().minusDays(LOOKBACK_DAYS));

        // 관측이 한 번뿐이면 늘었는지 줄었는지 알 수 없다.
        if (history.size() < 2) {
            return 0;
        }

        FacilityCapacitySnapshot latest = history.get(history.size() - 1);
        FacilityCapacitySnapshot previous = history.get(history.size() - 2);

        int increase = vacancyIncrease(previous, latest);
        if (increase < minIncrease) {
            return 0;
        }

        CareFacility facility = facilityRepository.findById(facilityId).orElse(null);
        if (facility == null || !Boolean.TRUE.equals(facility.getIsActive())) {
            return 0;
        }

        LocalDate observedDate = latest.getObservedDate();
        String title = String.format("%s에 자리가 났습니다", facility.getName());
        String message = buildMessage(facility, latest, increase);

        int sent = 0;
        for (FacilityWaitlist entry : waitlistRepository.findWaiting(facilityId)) {
            if (!entry.canNotifyVacancy(observedDate, minIntervalDays)) {
                continue;
            }

            Notification notification = notificationRepository.save(Notification.builder()
                    .user(entry.getUser())
                    .notificationType(Notification.NotificationType.FACILITY)
                    .title(title)
                    .message(message)
                    .createdAt(LocalDateTime.now())
                    .build());

            dispatcher.dispatchAsync(notification);
            eventLogger.log(EventType.NOTIFICATION_SENT, entry.getUser().getId(),
                    String.valueOf(notification.getId()), "FACILITY_VACANCY");

            entry.markVacancyNotified(observedDate);
            sent++;
        }
        return sent;
    }

    /**
     * 빈자리가 얼마나 늘었는지.
     *
     * <p>공공데이터는 빈자리를 직접 주기도 하고 정원·현원만 주기도 한다. 둘 다 없으면
     * 판단할 근거가 없으므로 0으로 본다.
     */
    private int vacancyIncrease(FacilityCapacitySnapshot before, FacilityCapacitySnapshot after) {
        Integer beforeSpots = availableSpots(before);
        Integer afterSpots = availableSpots(after);

        if (beforeSpots == null || afterSpots == null) {
            return 0;
        }
        return afterSpots - beforeSpots;
    }

    private Integer availableSpots(FacilityCapacitySnapshot snapshot) {
        if (snapshot.getAvailableSpots() != null) {
            return snapshot.getAvailableSpots();
        }
        if (snapshot.getCapacity() != null && snapshot.getCurrentEnrollment() != null) {
            return snapshot.getCapacity() - snapshot.getCurrentEnrollment();
        }
        return null;
    }

    /**
     * 관측 사실만 쓰고 넘겨짚지 않는다.
     *
     * <p>공공데이터는 시설 전체 정원만 주므로 어느 반에 자리가 났는지는 알 수 없다.
     * 반이 다르면 헛걸음이라 그 한계를 문구에 그대로 밝힌다.
     */
    private String buildMessage(CareFacility facility, FacilityCapacitySnapshot latest, int increase) {
        Integer spots = availableSpots(latest);
        return String.format(
                "대기 등록해 두신 %s의 빈자리가 %d자리 늘어 현재 %d자리입니다. (%s 관측 기준) "
                        + "시설 전체 기준이라 해당 반에 자리가 있는지는 시설에 확인해 보세요.",
                facility.getName(), increase, spots == null ? increase : spots, latest.getObservedDate());
    }
}
