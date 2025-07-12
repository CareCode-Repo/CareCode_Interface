package com.carecode.domain.notification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 알림 템플릿 엔티티
 * 
 * 알림 발송 시 사용할 수 있는 미리 정의된 템플릿을 관리.
 * 
 * 주요 기능:
 * - 알림 템플릿 정보 관리 (제목, 내용, 타입, 설명)
 * - 템플릿 활성화/비활성화 상태 관리
 * - 해당 템플릿을 사용한 알림들 추적
 * - 템플릿별 알림 발송 통계 및 모니터링
 * 
 * @author CareCode Team
 * @since 1.0.0
 * @version 1.0.0
 */
@Entity
@Table(name = "notification_templates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class NotificationTemplate {

    /**
     * 알림 템플릿 고유 식별자 (Primary Key) 
     * @since 1.0.0
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 템플릿 코드
     * @since 1.0.0
     */
    @Column(name = "template_code", nullable = false, unique = true, length = 50)
    private String templateCode;

    /**
     * 알림 제목 템플릿
     * @since 1.0.0
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * 알림 내용 템플릿
     * @since 1.0.0
     */
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * 템플릿 타입
     * @since 1.0.0
     */
    @Column(name = "template_type", length = 50)
    private String templateType;

    /**
     * 템플릿에 대한 설명
     * 
     * 예시: "신규 사용자 가입 시 발송되는 환영 이메일"
     * 
     * @since 1.0.0
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 템플릿 활성화 상태
     * 
     * true: 활성화 (사용 가능)
     * false: 비활성화 (사용 불가)
     * 기본값: true
     * 
     * @since 1.0.0
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * 템플릿 생성 시간
     * 
     * JPA Auditing을 통해 자동 설정됨
     * 
     * @since 1.0.0
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 템플릿 정보 수정 시간
     * 
     * JPA Auditing을 통해 자동 업데이트됨
     * 
     * @since 1.0.0
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 이 템플릿을 사용하여 발송된 알림들
     * 
     * One-to-Many 관계: 하나의 템플릿은 여러 알림에서 사용될 수 있음
     * 
     * cascade = CascadeType.ALL: 템플릿 삭제 시 관련 알림도 함께 삭제
     * orphanRemoval = true: 알림이 템플릿에서 제거되면 DB에서도 삭제
     * 
     * 예시:
     * - WELCOME_EMAIL 템플릿: 환영 이메일로 발송된 모든 알림들
     * - PASSWORD_RESET 템플릿: 비밀번호 재설정으로 발송된 모든 알림들
     * 
     * @since 1.0.0
     */
    @OneToMany(mappedBy = "notificationTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications = new ArrayList<>();

    /**
     * 알림 템플릿 생성자
     * 
     * @param templateCode 템플릿 코드 (예: "WELCOME_EMAIL")
     * @param title 알림 제목 (예: "환영합니다!")
     * @param content 알림 내용 (예: "안녕하세요! CareCode에 오신 것을 환영합니다.")
     * @param templateType 템플릿 타입 (예: "EMAIL")
     * @param description 템플릿 설명 (예: "신규 사용자 가입 시 발송되는 환영 이메일")
     * 
     * @since 1.0.0
     */
    @Builder
    public NotificationTemplate(String templateCode, String title, String content,
                              String templateType, String description) {
        this.templateCode = templateCode;
        this.title = title;
        this.content = content;
        this.templateType = templateType;
        this.description = description;
    }

    /**
     * 템플릿 정보 업데이트
     * 
     * @param title 새로운 알림 제목
     * @param content 새로운 알림 내용
     * @param templateType 새로운 템플릿 타입
     * @param description 새로운 템플릿 설명
     * 
     * @since 1.0.0
     */
    public void updateTemplate(String title, String content, String templateType, String description) {
        this.title = title;
        this.content = content;
        this.templateType = templateType;
        this.description = description;
    }

    /**
     * 템플릿 비활성화
     * 
     * 비활성화된 템플릿은 새로운 알림 발송에 사용할 수 없음
     * 
     * @since 1.0.0
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 템플릿 활성화
     * 
     * 활성화된 템플릿은 새로운 알림 발송에 사용 가능
     * 
     * @since 1.0.0
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 이 템플릿을 사용하여 발송된 알림 개수 조회
     * 
     * @return 알림 개수
     * @since 1.0.0
     */
    public int getNotificationCount() {
        return notifications.size();
    }

    /**
     * 템플릿이 활성 상태인지 확인
     * 
     * @return 활성 상태 여부
     * @since 1.0.0
     */
    public boolean isTemplateActive() {
        return isActive;
    }

    /**
     * 템플릿 타입별 표시명 반환
     * 
     * @return 템플릿 타입별 한글 표시명
     * @since 1.0.0
     */
    public String getTemplateTypeDisplayName() {
        switch (templateType != null ? templateType.toUpperCase() : "") {
            case "EMAIL":
                return "이메일";
            case "PUSH":
                return "푸시 알림";
            case "SMS":
                return "SMS";
            case "SYSTEM":
                return "시스템";
            default:
                return templateType != null ? templateType : "기타";
        }
    }

    /**
     * 템플릿 사용 가능 여부 확인
     * 
     * 활성화된 템플릿만 사용 가능
     * 
     * @return 사용 가능 여부
     * @since 1.0.0
     */
    public boolean isUsable() {
        return isActive;
    }
} 