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

/** 알림 템플릿 엔티티 */
@Entity
@Table(name = "TBL_NOTIFICATION_TEMPLATES")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class NotificationTemplate {

    // 알림 템플릿 고유 식별자 (Primary Key) @since 1.0.0
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    // 템플릿 코드 @since 1.0.0
    @Column(name = "TEMPLATE_CODE", nullable = false, unique = true, length = 50)
    private String templateCode;

    // 알림 제목 템플릿 @since 1.0.0
    @Column(name = "TITLE", nullable = false, length = 200)
    private String title;

    // 알림 내용 템플릿 @since 1.0.0
    @Column(name = "CONTENT", columnDefinition = "TEXT", nullable = false)
    private String content;

    // 템플릿 타입 @since 1.0.0
    @Column(name = "TEMPLATE_TYPE", length = 50)
    private String templateType;

    // 템플릿에 대한 설명 예시: "신규 사용자 가입 시 발송되는 환영 이메일" @since 1.0.0
    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    // 템플릿 활성화 상태 true: 활성화 (사용 가능) false: 비활성화 (사용 불가) 기본값: true @since 1.0.0
    @Column(name = "IS_ACTIVE", nullable = false)
    private Boolean isActive = true;

    // 템플릿 생성 시간 JPA Auditing을 통해 자동 설정됨 @since 1.0.0
    @CreatedDate
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 템플릿 정보 수정 시간 JPA Auditing을 통해 자동 업데이트됨 @since 1.0.0
    @LastModifiedDate
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    // 알림 템플릿 생성자 @param templateCode 템플릿 코드 (예: "WELCOME_EMAIL") @param title 알림 제목 (예: "환영합니다!")
    @Builder
    public NotificationTemplate(String templateCode, String title, String content,
                              String templateType, String description) {
        this.templateCode = templateCode;
        this.title = title;
        this.content = content;
        this.templateType = templateType;
        this.description = description;
    }

    // 템플릿 정보 업데이트 @param title 새로운 알림 제목 @param content 새로운 알림 내용 @param templateType 새로운 템플릿 타입
    public void updateTemplate(String title, String content, String templateType, String description) {
        this.title = title;
        this.content = content;
        this.templateType = templateType;
        this.description = description;
    }

    // 템플릿 비활성화 비활성화된 템플릿은 새로운 알림 발송에 사용할 수 없음 @since 1.0.0
    public void deactivate() {
        this.isActive = false;
    }

    // 템플릿 활성화 활성화된 템플릿은 새로운 알림 발송에 사용 가능 @since 1.0.0
    public void activate() {
        this.isActive = true;
    }

    // 템플릿이 활성 상태인지 확인 @return 활성 상태 여부 @since 1.0.0
    public boolean isTemplateActive() {
        return isActive;
    }

    // 템플릿 타입별 표시명 반환 @return 템플릿 타입별 한글 표시명 @since 1.0.0
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

    // 템플릿 사용 가능 여부 확인 활성화된 템플릿만 사용 가능 @return 사용 가능 여부 @since 1.0.0
    public boolean isUsable() {
        return isActive;
    }
} 