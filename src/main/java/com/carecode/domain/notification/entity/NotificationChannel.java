package com.carecode.domain.notification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 알림 채널 엔티티
 * 
 * 알림을 전송할 수 있는 다양한 채널 정보를 관리
 * @author CareCode Team
 * @since 1.0.0
 */
@Entity
@Table(name = "TBL_NOTIFICATION_CHANNEL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class NotificationChannel {

    /**
     * 알림 채널 고유 식별자 (Primary Key)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    /**
     * 알림 채널 이름
     */
    @Column(name = "NAME", nullable = false)
    private String name;

    /**
     * 알림 채널에 대한 설명
     */
    @Column(name = "DESCRIPTION")
    private String description;

    /**
     * 알림 채널 생성 시간
     */
    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 알림 채널 정보 수정 시간
     */
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    /**
     * 이 채널을 통해 전송된 알림들
     */
    @OneToMany(mappedBy = "notificationChannel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications = new ArrayList<>();

    /**
     * 알림 채널 생성자
     * 
     * @param name 채널 이름 (예: "EMAIL", "PUSH", "SMS")
     * @param description 채널 설명 (예: "이메일을 통한 알림 전송")
     */
    @Builder
    public NotificationChannel(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * 알림 채널 정보 업데이트
     * 
     * @param name 새로운 채널 이름
     * @param description 새로운 채널 설명
     */
    public void updateChannel(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * 이 채널을 통해 전송된 알림 개수 조회
     * 
     * @return 알림 개수
     */
    public int getNotificationCount() {
        return notifications.size();
    }

    /**
     * 채널이 활성 상태인지 확인
     * (현재는 항상 true, 향후 isActive 필드 추가 시 활용)
     * 
     * @return 활성 상태 여부
     */
    public boolean isActive() {
        return true; // 향후 isActive 필드 추가 시 수정
    }

    /**
     * 채널 타입별 표시명 반환
     * 
     * @return 채널 타입별 한글 표시명
     */
    public String getDisplayName() {
        switch (name.toUpperCase()) {
            case "EMAIL":
                return "이메일";
            case "PUSH":
                return "푸시 알림";
            case "SMS":
                return "SMS";
            case "WEBHOOK":
                return "웹훅";
            default:
                return name;
        }
    }
} 