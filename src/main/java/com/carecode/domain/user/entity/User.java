package com.carecode.domain.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * 사용자 엔티티
 * 육아 플랫폼 사용자 정보를 관리
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
    private Long id;
    
    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;
    
    @Column(name = "email", nullable = false, unique = true)
    private String email;
    
    @Column(name = "password")
    private String password;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "phone_number")
    private String phoneNumber;
    
    @Column(name = "birth_date")
    private LocalDate birthDate;
    
    @Column(name = "gender")
    private String gender;
    
    @Column(name = "address")
    private String address;
    
    @Column(name = "latitude")
    private Double latitude;
    
    @Column(name = "longitude")
    private Double longitude;
    
    @Column(name = "profile_image_url")
    private String profileImageUrl;
    
    @Column(name = "role")
    private String role; // PARENT, CAREGIVER, ADMIN 등
    
    @Column(name = "is_active")
    private Boolean isActive;
    
    @Column(name = "email_verified")
    private Boolean emailVerified;
    
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // JPA 관계 매핑
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<com.carecode.domain.chatbot.entity.ChatMessage> chatMessages = new ArrayList<>();
    
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<com.carecode.domain.community.entity.Post> posts = new ArrayList<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<com.carecode.domain.health.entity.HealthRecord> healthRecords = new ArrayList<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<com.carecode.domain.notification.entity.Notification> notifications = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
        if (emailVerified == null) {
            emailVerified = false;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // 비즈니스 로직 메서드들
    public boolean isActive() {
        return isActive != null && isActive;
    }
    
    public boolean isEmailVerified() {
        return emailVerified != null && emailVerified;
    }
    
    public String getDisplayName() {
        return name != null ? name : "사용자";
    }
    
    public boolean isRecentlyActive(int daysThreshold) {
        if (lastLoginAt == null) return false;
        
        LocalDateTime threshold = LocalDateTime.now().minusDays(daysThreshold);
        return lastLoginAt.isAfter(threshold);
    }
    
    public boolean isInRegion(String targetRegion) {
        if (address == null) return false;
        return address.contains(targetRegion);
    }
    
    public boolean isNearby(double targetLatitude, double targetLongitude, double maxDistanceKm) {
        if (latitude == null || longitude == null) return false;
        
        // 간단한 거리 계산 (실제로는 Haversine 공식 사용)
        double distance = Math.sqrt(
            Math.pow(latitude - targetLatitude, 2) + 
            Math.pow(longitude - targetLongitude, 2)
        ) * 111; // 대략적인 km 변환
        
        return distance <= maxDistanceKm;
    }
    
    public boolean hasRole(String targetRole) {
        return role != null && role.equals(targetRole);
    }
    
    public boolean isParent() {
        return "PARENT".equals(role);
    }
    
    public boolean isCaregiver() {
        return "CAREGIVER".equals(role);
    }
    
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
    
    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }
    
    public int getChildrenCount() {
        return children != null ? children.size() : 0;
    }
    
    public boolean hasChildInAgeRange(int minAge, int maxAge) {
        if (!hasChildren()) return false;
        
        return children.stream()
            .anyMatch(child -> child.getAge() >= minAge && child.getAge() <= maxAge);
    }
    
    public List<Child> getChildrenInAgeRange(int minAge, int maxAge) {
        if (!hasChildren()) return List.of();
        
        return children.stream()
            .filter(child -> child.getAge() >= minAge && child.getAge() <= maxAge)
            .collect(java.util.stream.Collectors.toList());
    }
    
    public boolean isNotificationEnabled(String notificationType) {
        if (notificationSettings == null) return false;
        
        switch (notificationType) {
            case "EMAIL":
                return notificationSettings.isEmailNotification();
            case "PUSH":
                return notificationSettings.isPushNotification();
            case "SMS":
                return notificationSettings.isSmsNotification();
            default:
                return false;
        }
    }
    
    public boolean isPolicyNotificationEnabled() {
        return notificationSettings != null && notificationSettings.isPolicyNotification();
    }
    
    public boolean isFacilityNotificationEnabled() {
        return notificationSettings != null && notificationSettings.isFacilityNotification();
    }
    
    public boolean isCommunityNotificationEnabled() {
        return notificationSettings != null && notificationSettings.isCommunityNotification();
    }
    
    public boolean isChatbotNotificationEnabled() {
        return notificationSettings != null && notificationSettings.isChatbotNotification();
    }
    
    // 내부 클래스들
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Child {
        private Long childId;
        private String name;
        private int age;
        private String gender;
        private String birthDate;
        private String specialNeeds;
        
        public Child(String name, int age, String gender) {
            this.name = name;
            this.age = age;
            this.gender = gender;
        }
    }
    
    @Getter
    @Setter
    @NoArgsConstructor
    public static class NotificationSettings {
        private boolean policyNotification;
        private boolean facilityNotification;
        private boolean communityNotification;
        private boolean chatbotNotification;
        private boolean emailNotification;
        private boolean pushNotification;
        private boolean smsNotification;
        private String quietHoursStart;
        private String quietHoursEnd;
        private boolean quietHoursEnabled;
    }
} 