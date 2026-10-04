package com.carecode.domain.notification.entity;

import com.carecode.domain.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 푸시를 받을 기기 하나.
 *
 * <p>전에는 토큰을 알림 설정 행의 한 칸에 두었다. 그래서 웹에서 알림을 켜 둔 사람이 앱에서
 * 다시 켜면 앞의 토큰을 덮어썼고, 한쪽은 아무 말 없이 알림을 받지 못했다. 토큰은 사용자의
 * 성질이 아니라 <b>기기의 성질</b>이라 행을 따로 둔다.
 */
@Entity
@Table(name = "TBL_PUSH_DEVICE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    /** FCM 등록 토큰. 기기를 지우고 다시 깔면 새 토큰이 나온다. */
    @Column(name = "TOKEN", nullable = false, length = 512, unique = true)
    private String token;

    /** WEB / ANDROID / IOS. 발송 경로를 가르지는 않지만 "알림이 안 와요" 를 추적하려면 필요하다. */
    @Column(name = "DEVICE_TYPE", length = 20)
    private String deviceType;

    @Column(name = "APP_VERSION", length = 40)
    private String appVersion;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;
}
