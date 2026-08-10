package com.carecode.domain.notification.repository;

import com.carecode.domain.notification.entity.NotificationPreference;
import com.carecode.domain.notification.entity.Notification;
import com.carecode.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** 알림 설정 리포지토리 */
@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    // 사용자별 알림 설정 목록 조회
    List<NotificationPreference> findByUserOrderByNotificationType(User user);

    // 사용자와 알림 타입으로 설정 조회
    Optional<NotificationPreference> findByUserAndNotificationType(User user, Notification.NotificationType notificationType);

    /**
     * 사용자에게 등록된 디바이스 토큰.
     *
     * 토큰은 기기의 성질이지 알림 유형의 성질이 아닌데 설정 행마다 들고 있다. 등록은 SYSTEM 행에만
     * 쓰므로 유형별 행에서만 찾으면 SYSTEM 이 아닌 알림은 푸시가 나가지 않는다. 유형과 무관하게 찾는다.
     */
    @Query("SELECT np.deviceToken FROM NotificationPreference np "
            + "WHERE np.user = :user AND np.deviceToken IS NOT NULL AND np.deviceToken <> '' "
            + "ORDER BY np.updatedAt DESC")
    List<String> findDeviceTokensByUser(@Param("user") User user);

    // 사용자별 활성화된 이메일 알림 설정 조회
    @Query("SELECT np FROM NotificationPreference np WHERE np.user = :user AND np.emailEnabled = true")
    List<NotificationPreference> findEmailEnabledByUser(@Param("user") User user);

    // 사용자별 활성화된 푸시 알림 설정 조회
    @Query("SELECT np FROM NotificationPreference np WHERE np.user = :user AND np.pushEnabled = true")
    List<NotificationPreference> findPushEnabledByUser(@Param("user") User user);

    // 사용자별 활성화된 SMS 알림 설정 조회
    @Query("SELECT np FROM NotificationPreference np WHERE np.user = :user AND np.smsEnabled = true")
    List<NotificationPreference> findSmsEnabledByUser(@Param("user") User user);

    // 사용자별 활성화된 인앱 알림 설정 조회
    @Query("SELECT np FROM NotificationPreference np WHERE np.user = :user AND np.inAppEnabled = true")
    List<NotificationPreference> findInAppEnabledByUser(@Param("user") User user);

    // 특정 알림 타입의 활성화된 설정 조회
    @Query("SELECT np FROM NotificationPreference np WHERE np.notificationType = :notificationType AND (np.emailEnabled = true OR np.pushEnabled = true OR np.smsEnabled = true OR np.inAppEnabled = true)")
    List<NotificationPreference> findEnabledByNotificationType(@Param("notificationType") Notification.NotificationType notificationType);

    // 사용자별 설정 수 조회
    long countByUser(User user);

    // 알림 타입별 설정 수 조회
    long countByNotificationType(Notification.NotificationType notificationType);
} 