package com.carecode.domain.user.entity;

import com.carecode.domain.careFacility.entity.Review;
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

/**
 * 사용자 엔티티
 */
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
    
    @Column(name = "PASSWORD", nullable = false)
    private String password;
    
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
    
    @Column(name = "PROFILE_IMAGE_URL")
    private String profileImageUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE", nullable = false)
    private UserRole role; // PARENT, CAREGIVER, ADMIN 등
    
    @Column(name = "IS_ACTIVE", nullable = false)
    private Boolean isActive;
    
    @Column(name = "EMAIL_VERIFIED", nullable = false)
    private Boolean emailVerified;
    
    @Column(name = "LAST_LOGIN_AT")
    private LocalDateTime lastLoginAt;
    
    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
    
    // JPA 관계 매핑
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Post> posts = new ArrayList<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<HealthRecord> healthRecords = new ArrayList<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Review> reviews = new ArrayList<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Notification> notifications = new ArrayList<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Child> children = new ArrayList<>();
    
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private NotificationSettings notificationSettings;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) isActive = true;
        if (emailVerified == null) emailVerified = false;
        if (userId == null) {
            userId = "user_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * 사용자 성별 Enum
     */
    public enum Gender {
        MALE("남성"),
        FEMALE("여성"),
        OTHER("기타");
        
        private final String displayName;
        
        Gender(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 사용자 역할 Enum
     */
    public enum UserRole {
        PARENT("부모"),
        CAREGIVER("보육사"),
        ADMIN("관리자"),
        GUEST("게스트");
        
        private final String displayName;
        
        UserRole(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
} 