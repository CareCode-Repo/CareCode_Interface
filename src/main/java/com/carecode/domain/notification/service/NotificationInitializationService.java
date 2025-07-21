package com.carecode.domain.notification.service;

import com.carecode.domain.notification.entity.Notification;
import com.carecode.domain.notification.repository.NotificationRepository;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 알림 초기화 서비스
 * 애플리케이션 시작 시 테스트용 알림을 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationInitializationService implements CommandLineRunner {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        createTestNotifications();
    }

    /**
     * 테스트용 알림 생성
     */
    @Transactional
    public void createTestNotifications() {
        try {
            // 테스트 사용자 조회
            Optional<User> testUser = userRepository.findByEmail("test1@carecode.com");
            if (testUser.isEmpty()) {
                log.info("테스트 사용자가 없어 알림을 생성하지 않습니다.");
                return;
            }

            User user = testUser.get();
            
            // 기존 알림이 있는지 확인
            if (notificationRepository.countByUserId(user.getId()) > 0) {
                log.info("이미 알림이 존재합니다. 추가 생성하지 않습니다.");
                return;
            }

            // 테스트 알림 생성
            createSystemNotification(user);
            createPolicyNotification(user);
            createCommunityNotification(user);

            log.info("테스트 알림 생성 완료");

        } catch (Exception e) {
            log.error("테스트 알림 생성 중 오류 발생: {}", e.getMessage());
        }
    }

    /**
     * 시스템 알림 생성
     */
    private void createSystemNotification(User user) {
        Notification systemNotification = Notification.builder()
                .user(user)
                .notificationType(Notification.NotificationType.SYSTEM)
                .title("시스템 알림")
                .message("새로운 기능이 추가되었습니다. 육아 정보 관리 기능을 확인해보세요.")
                .isRead(false)
                .build();

        notificationRepository.save(systemNotification);
        log.info("시스템 알림 생성 완료: {}", systemNotification.getTitle());
    }

    /**
     * 정책 알림 생성
     */
    private void createPolicyNotification(User user) {
        Notification policyNotification = Notification.builder()
                .user(user)
                .notificationType(Notification.NotificationType.POLICY)
                .title("새로운 육아 정책")
                .message("2024년 새로운 육아 지원 정책이 발표되었습니다. 자세한 내용을 확인해보세요.")
                .isRead(false)
                .build();

        notificationRepository.save(policyNotification);
        log.info("정책 알림 생성 완료: {}", policyNotification.getTitle());
    }

    /**
     * 커뮤니티 알림 생성
     */
    private void createCommunityNotification(User user) {
        Notification communityNotification = Notification.builder()
                .user(user)
                .notificationType(Notification.NotificationType.COMMUNITY)
                .title("커뮤니티 활동")
                .message("새로운 육아 팁이 공유되었습니다. 커뮤니티에서 확인해보세요.")
                .isRead(true)
                .build();

        notificationRepository.save(communityNotification);
        log.info("커뮤니티 알림 생성 완료: {}", communityNotification.getTitle());
    }
} 