package com.carecode.domain.policy.service;

import com.carecode.core.analytics.EventLogger;
import com.carecode.domain.notification.entity.Notification;
import com.carecode.domain.notification.repository.NotificationRepository;
import com.carecode.domain.notification.sender.NotificationDispatcher;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.entity.PolicyDeadlineNotice;
import com.carecode.domain.policy.repository.PolicyDeadlineNoticeRepository;
import com.carecode.domain.policy.repository.PolicyRepository;
import com.carecode.domain.user.entity.Child;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.ChildRepository;
import com.carecode.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("신청 마감 임박 알림")
class PolicyDeadlineNotifierTest {

    private PolicyRepository policyRepository;
    private UserRepository userRepository;
    private ChildRepository childRepository;
    private PolicyDeadlineNoticeRepository noticeRepository;
    private PolicyDeadlineNotifier notifier;

    @BeforeEach
    void setUp() {
        policyRepository = mock(PolicyRepository.class);
        userRepository = mock(UserRepository.class);
        childRepository = mock(ChildRepository.class);
        noticeRepository = mock(PolicyDeadlineNoticeRepository.class);
        when(noticeRepository.findNotifiedUserIds(anyLong(), any(LocalDate.class)))
                .thenReturn(List.of());

        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        notifier = new PolicyDeadlineNotifier(policyRepository, noticeRepository, userRepository, childRepository,
                notificationRepository, mock(NotificationDispatcher.class), mock(EventLogger.class));
        ReflectionTestUtils.setField(notifier, "leadDaysRaw", "7,1");

        givenUser(User.builder().id(1L).name("보호자").address("충청북도 청주시 흥덕구").build());
        givenChildren(child(24));
    }

    @Test
    @DisplayName("마감 7일 전이면 알린다")
    void notifiesSevenDaysBefore() {
        givenPolicies(policyDueIn(7));

        var result = notifier.notifyUpcomingDeadlines();

        assertThat(result.getPoliciesDueSoon()).isEqualTo(1);
        assertThat(result.getNotificationsSent()).isEqualTo(1);
    }

    @Test
    @DisplayName("마감 1일 전이면 마지막으로 한 번 더 알린다")
    void notifiesOneDayBefore() {
        givenPolicies(policyDueIn(1));

        assertThat(notifier.notifyUpcomingDeadlines().getNotificationsSent()).isEqualTo(1);
    }

    @Test
    @DisplayName("지정하지 않은 날에는 보내지 않아 중복 발송이 없다")
    void silentOnOtherDays() {
        givenPolicies(policyDueIn(5));

        var result = notifier.notifyUpcomingDeadlines();

        assertThat(result.getPoliciesDueSoon()).isZero();
        assertThat(result.getNotificationsSent()).isZero();
    }

    @Test
    @DisplayName("이미 마감된 정책은 알리지 않는다")
    void silentAfterDeadline() {
        givenPolicies(policyDueIn(-3));

        assertThat(notifier.notifyUpcomingDeadlines().getNotificationsSent()).isZero();
    }

    @Test
    @DisplayName("다른 지역 정책은 보내지 않는다")
    void skipsOtherRegions() {
        Policy policy = policyDueIn(7);
        policy.setTargetRegion("제주특별자치도");
        givenPolicies(policy);

        assertThat(notifier.notifyUpcomingDeadlines().getNotificationsSent()).isZero();
    }

    @Test
    @DisplayName("대상 연령을 벗어난 아이만 있으면 보내지 않는다")
    void skipsWhenNoChildInAgeRange() {
        Policy policy = policyDueIn(7);
        policy.setTargetAgeMin(0);
        policy.setTargetAgeMax(12);
        givenPolicies(policy);
        givenChildren(child(36));

        assertThat(notifier.notifyUpcomingDeadlines().getNotificationsSent()).isZero();
    }

    @Test
    @DisplayName("소득을 입력하지 않았으면 배제하지 않는다")
    void includesUsersWithUnknownIncome() {
        Policy policy = policyDueIn(7);
        policy.setIncomeThresholdPercent(150);
        givenPolicies(policy);
        // 소득 미입력을 탈락으로 처리하면 받을 수 있었던 지원금이 통째로 사라진다.
        givenUser(User.builder().id(1L).name("보호자").address("충청북도 청주시").build());

        assertThat(notifier.notifyUpcomingDeadlines().getNotificationsSent()).isEqualTo(1);
    }

    @Test
    @DisplayName("소득이 기준을 넘으면 보내지 않는다")
    void skipsUsersAboveIncomeThreshold() {
        Policy policy = policyDueIn(7);
        policy.setIncomeThresholdPercent(150);
        givenPolicies(policy);
        givenUser(User.builder().id(1L).name("보호자").address("충청북도 청주시").incomePercent(200).build());

        assertThat(notifier.notifyUpcomingDeadlines().getNotificationsSent()).isZero();
    }

    @Test
    @DisplayName("자녀가 없으면 보내지 않는다")
    void skipsUsersWithoutChildren() {
        givenPolicies(policyDueIn(7));
        givenChildren();

        assertThat(notifier.notifyUpcomingDeadlines().getNotificationsSent()).isZero();
    }

    @Test
    @DisplayName("마감일이 없는 정책은 대상이 아니다")
    void skipsPoliciesWithoutDeadline() {
        Policy policy = policyDueIn(7);
        policy.setApplicationEndDate(null);
        givenPolicies(policy);

        assertThat(notifier.notifyUpcomingDeadlines().getPoliciesDueSoon()).isZero();
    }

    @Test
    @DisplayName("오늘 이미 받은 사람에게는 다시 보내지 않는다")
    void doesNotResendOnSameDay() {
        givenPolicies(policyDueIn(7));
        // 스케줄러가 하루에 두 번 돌거나 배포 중 인스턴스가 두 대여도 한 번만 나가야 한다.
        when(noticeRepository.findNotifiedUserIds(anyLong(), any(LocalDate.class)))
                .thenReturn(List.of(1L));

        assertThat(notifier.notifyUpcomingDeadlines().getNotificationsSent()).isZero();
    }

    @Test
    @DisplayName("발송하면 이력을 남긴다")
    void recordsNoticeWhenSent() {
        givenPolicies(policyDueIn(7));

        notifier.notifyUpcomingDeadlines();

        verify(noticeRepository).save(any(PolicyDeadlineNotice.class));
    }

    private void givenPolicies(Policy... policies) {
        Page<Policy> page = new PageImpl<>(List.of(policies));
        when(policyRepository.findByIsActiveTrueOrderByPriorityDescViewCountDesc(any(Pageable.class)))
                .thenReturn(page);
    }

    private void givenUser(User user) {
        when(userRepository.findByIsActiveTrue()).thenReturn(List.of(user));
    }

    private void givenChildren(Child... children) {
        when(childRepository.findByUserIdOrderByCreatedAtDesc(anyLong())).thenReturn(List.of(children));
    }

    private Policy policyDueIn(int days) {
        Policy policy = new Policy();
        policy.setId(1L);
        policy.setTitle("청주시 출산장려금");
        policy.setTargetRegion("충청북도 청주시");
        policy.setBenefitAmount(1_000_000);
        policy.setApplicationEndDate(LocalDate.now().plusDays(days));
        policy.setIsActive(true);
        return policy;
    }

    private Child child(int months) {
        return Child.builder()
                .id(1L)
                .name("아이")
                .birthDate(LocalDate.now().minusMonths(months))
                .build();
    }
}
