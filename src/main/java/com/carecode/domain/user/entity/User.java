package com.carecode.domain.user.entity;

import com.carecode.domain.careFacility.entity.Review;
import com.carecode.domain.chatbot.entity.ChatMessage;
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
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK
    
    @Column(name = "user_id", nullable = false, unique = true)
    private String userId; // UK
    
    @Column(name = "email", nullable = false, unique = true)
    private String email; // UK
    
    @Column(name = "password")
    private String password;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "phone_number")
    private String phoneNumber;
    
    @Column(name = "birth_date")
    private LocalDate birthDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;
    
    @Column(name = "address")
    private String address;
    
    @Column(name = "latitude")
    private Double latitude;
    
    @Column(name = "longitude")
    private Double longitude;
    
    @Column(name = "profile_image_url")
    private String profileImageUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role; // PARENT, CAREGIVER, ADMIN 등
    
    @Column(name = "is_active")
    private Boolean isActive;
    
    @Column(name = "email_verified")
    private Boolean emailVerified;
    
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // JPA 관계 매핑
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChatMessage> chatMessages = new ArrayList<>();
    
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Post> posts = new ArrayList<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<HealthRecord> healthRecords = new ArrayList<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Review> reviews = new ArrayList<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Notification> notifications = new ArrayList<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Like> likes = new ArrayList<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Bookmark> bookmarks = new ArrayList<>();
    
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