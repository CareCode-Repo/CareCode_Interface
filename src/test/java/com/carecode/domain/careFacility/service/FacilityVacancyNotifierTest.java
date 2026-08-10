package com.carecode.domain.careFacility.service;

import com.carecode.core.analytics.EventLogger;
import com.carecode.domain.careFacility.entity.CareFacility;
import com.carecode.domain.careFacility.entity.FacilityCapacitySnapshot;
import com.carecode.domain.careFacility.entity.FacilityWaitlist;
import com.carecode.domain.careFacility.repository.CareFacilityRepository;
import com.carecode.domain.careFacility.repository.FacilityCapacitySnapshotRepository;
import com.carecode.domain.careFacility.repository.FacilityWaitlistRepository;
import com.carecode.domain.notification.entity.Notification;
import com.carecode.domain.notification.repository.NotificationRepository;
import com.carecode.domain.notification.sender.NotificationDispatcher;
import com.carecode.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("빈자리 알림")
class FacilityVacancyNotifierTest {

    private FacilityWaitlistRepository waitlistRepository;
    private FacilityCapacitySnapshotRepository snapshotRepository;
    private CareFacilityRepository facilityRepository;
    private NotificationRepository notificationRepository;
    private FacilityVacancyNotifier notifier;

    @BeforeEach
    void setUp() {
        waitlistRepository = mock(FacilityWaitlistRepository.class);
        snapshotRepository = mock(FacilityCapacitySnapshotRepository.class);
        facilityRepository = mock(CareFacilityRepository.class);
        notificationRepository = mock(NotificationRepository.class);

        when(facilityRepository.findById(anyLong())).thenReturn(Optional.of(
                CareFacility.builder().name("행복어린이집").isActive(true).build()));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        notifier = new FacilityVacancyNotifier(waitlistRepository, snapshotRepository,
                facilityRepository, notificationRepository,
                mock(NotificationDispatcher.class), mock(EventLogger.class));
        ReflectionTestUtils.setField(notifier, "minIntervalDays", 14);
        ReflectionTestUtils.setField(notifier, "minIncrease", 1);
    }

    @Test
    @DisplayName("빈자리가 늘면 대기자에게 알린다")
    void notifiesWhenVacancyIncreases() {
        givenWaitingFacility(1L, waiting());
        givenSnapshots(spots(0), spots(2));

        var result = notifier.notifyNewVacancies();

        assertThat(result.getNotificationsSent()).isEqualTo(1);
        assertThat(result.getFacilitiesWithVacancy()).isEqualTo(1);
    }

    @Test
    @DisplayName("빈자리가 그대로면 알리지 않는다")
    void silentWhenVacancyUnchanged() {
        givenWaitingFacility(1L, waiting());
        // 계속 자리가 있는 곳은 이미 알고 있다. 새로 난 자리만 알린다.
        givenSnapshots(spots(3), spots(3));

        assertThat(notifier.notifyNewVacancies().getNotificationsSent()).isZero();
    }

    @Test
    @DisplayName("빈자리가 줄면 알리지 않는다")
    void silentWhenVacancyDecreases() {
        givenWaitingFacility(1L, waiting());
        givenSnapshots(spots(4), spots(1));

        assertThat(notifier.notifyNewVacancies().getNotificationsSent()).isZero();
    }

    @Test
    @DisplayName("관측이 한 번뿐이면 늘었는지 알 수 없으므로 알리지 않는다")
    void silentWithSingleObservation() {
        givenWaitingFacility(1L, waiting());
        givenSnapshots(spots(5));

        assertThat(notifier.notifyNewVacancies().getNotificationsSent()).isZero();
    }

    @Test
    @DisplayName("최근에 이미 알린 사람에게는 다시 보내지 않는다")
    void doesNotRepeatWithinInterval() {
        FacilityWaitlist entry = waiting();
        entry.markVacancyNotified(LocalDate.now().minusDays(3));
        givenWaitingFacility(1L, entry);
        givenSnapshots(spots(0), spots(2));

        assertThat(notifier.notifyNewVacancies().getNotificationsSent()).isZero();
    }

    @Test
    @DisplayName("간격이 지났으면 다시 알린다")
    void notifiesAgainAfterInterval() {
        FacilityWaitlist entry = waiting();
        entry.markVacancyNotified(LocalDate.now().minusDays(30));
        givenWaitingFacility(1L, entry);
        givenSnapshots(spots(0), spots(2));

        assertThat(notifier.notifyNewVacancies().getNotificationsSent()).isEqualTo(1);
    }

    @Test
    @DisplayName("정원·현원만 있어도 빈자리를 계산한다")
    void derivesVacancyFromCapacity() {
        givenWaitingFacility(1L, waiting());
        givenSnapshots(capacityOnly(50, 50), capacityOnly(50, 47));

        assertThat(notifier.notifyNewVacancies().getNotificationsSent()).isEqualTo(1);
    }

    @Test
    @DisplayName("대기자가 없으면 아무 시설도 확인하지 않는다")
    void skipsWhenNobodyIsWaiting() {
        when(waitlistRepository.findFacilityIdsWithWaiting()).thenReturn(List.of());

        var result = notifier.notifyNewVacancies();

        assertThat(result.getFacilitiesChecked()).isZero();
        assertThat(result.getNotificationsSent()).isZero();
    }

    private void givenWaitingFacility(Long facilityId, FacilityWaitlist... entries) {
        when(waitlistRepository.findFacilityIdsWithWaiting()).thenReturn(List.of(facilityId));
        when(waitlistRepository.findWaiting(facilityId)).thenReturn(List.of(entries));
    }

    private void givenSnapshots(FacilityCapacitySnapshot... snapshots) {
        when(snapshotRepository.findHistory(anyLong(), any(LocalDate.class)))
                .thenReturn(List.of(snapshots));
    }

    private FacilityWaitlist waiting() {
        return FacilityWaitlist.builder()
                .facilityId(1L)
                .user(User.builder().id(1L).name("보호자").build())
                .appliedAt(LocalDate.now().minusMonths(2))
                .status(FacilityWaitlist.WaitStatus.WAITING)
                .build();
    }

    private FacilityCapacitySnapshot spots(int availableSpots) {
        return FacilityCapacitySnapshot.builder()
                .facilityId(1L)
                .observedDate(LocalDate.now())
                .availableSpots(availableSpots)
                .build();
    }

    private FacilityCapacitySnapshot capacityOnly(int capacity, int enrollment) {
        return FacilityCapacitySnapshot.builder()
                .facilityId(1L)
                .observedDate(LocalDate.now())
                .capacity(capacity)
                .currentEnrollment(enrollment)
                .build();
    }
}
