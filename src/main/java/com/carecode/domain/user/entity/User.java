package com.carecode.domain.user.entity;

import com.carecode.domain.facility.entity.Review;
import com.carecode.domain.community.entity.Post;
import com.carecode.domain.health.entity.HealthRecord;
import com.carecode.domain.notification.entity.Notification;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 사용자 엔티티 */
@Entity
@Table(name = "TBL_USER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id; // PK
    
    @Column(name = "USER_ID", nullable = false)
    private String userId; // UK - 자동 생성
    
    @Column(name = "EMAIL", nullable = false, unique = true)
    private String email; // UK
    
    @Column(name = "PASSWORD")
    private String password; // OAuth 사용자는 null 가능
    
    @Column(name = "NAME", nullable = false)
    private String name;
    
    @Column(name = "PHONE_NUMBER")
    private String phoneNumber;
    
    @Column(name = "BIRTH_DATE")
    private LocalDate birthDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "GENDER")
    private Gender gender;
    
    @Column(name = "ADDRESS")
    private String address;
    
    @Column(name = "LATITUDE")
    private Double latitude;
    
    @Column(name = "LONGITUDE")
    private Double longitude;

    /** 가구 소득 / 기준중위소득 (%). 실제 금액은 받지 않는다. null 이면 미입력. */
    @Column(name = "INCOME_PERCENT")
    private Integer incomePercent;

    @Column(name = "HOUSEHOLD_SIZE")
    private Integer householdSize;
    
    @Column(name = "PROFILE_IMAGE_URL")
    private String profileImageUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE", nullable = false)
    private UserRole role; // PARENT, CAREGIVER, ADMIN 등
    
    @Column(name = "PROVIDER")
    private String provider; // kakao, google, naver 등
    
    @Column(name = "PROVIDER_ID")
    private String providerId; // OAuth2 제공자의 사용자 ID
    
    @Column(name = "IS_ACTIVE", nullable = false)
    private Boolean isActive;
    
    @Column(name = "EMAIL_VERIFIED", nullable = false)
    private Boolean emailVerified;
    
    @Column(name = "REGISTRATION_COMPLETED", nullable = false)
    private Boolean registrationCompleted; // 카카오 사용자 가입 프로세스 완료 여부
    
    @Column(name = "DELETED_AT")
    private LocalDateTime deletedAt; // 소프트 삭제를 위한 삭제 시간
    
    @Column(name = "LAST_LOGIN_AT")
    private LocalDateTime lastLoginAt;
    
    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
    
    // JPA 관계 매핑
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Post> posts = new ArrayList<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<HealthRecord> healthRecords = new ArrayList<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Notification> notifications = new ArrayList<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Child> children = new ArrayList<>();
    
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private NotificationSettings notificationSettings;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) isActive = true;
        if (emailVerified == null) emailVerified = false;
        if (registrationCompleted == null) registrationCompleted = false; // 카카오 사용자는 기본적으로 가입 미완료
        if (userId == null) {
            userId = "user_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 탈퇴 처리. 개인 식별 정보를 지우고 계정을 비활성화한다.
     *
     * <p>물리 삭제하지 않는 이유: 게시글·댓글·건강기록 등 참조 데이터가 함께 사라지면 다른
     * 이용자의 대화 맥락이 깨진다. 그래서 행은 남기고 <b>식별 정보만</b> 파기한다.
     *
     * <p>엔티티에 두는 이유: "무엇을 지우는가" 는 이 테이블의 규칙이다. 한때 본인 탈퇴
     * ({@code PrivacyService})와 관리자 탈퇴 처리({@code AdminUserController})가 각자
     * 구현했고, 관리자 쪽은 {@code deletedAt}·{@code isActive} 만 바꿔 <b>이름과 이메일이
     * 그대로 남았다.</b> 이용자가 고객센터로 탈퇴를 요청하면 운영자가 그 경로를 누른다.
     *
     * <p>{@code userId}(가입 시 발급한 코드)는 남긴다. 게시글·알림이 그 값으로 작성자를
     * 가리키므로 지우면 참조가 끊긴다.
     */
    public void withdraw() {
        String anonymized = "deleted_" + id;
        this.name = "탈퇴한 사용자";
        this.email = anonymized + "@deleted.local";
        this.password = null;
        this.phoneNumber = null;
        this.address = null;
        this.profileImageUrl = null;
        this.providerId = null;
        this.latitude = null;
        this.longitude = null;
        this.isActive = false;
        this.deletedAt = LocalDateTime.now();
    }
} 