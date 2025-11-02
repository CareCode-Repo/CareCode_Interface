package com.carecode.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 관련 응답 DTO 통합 클래스
 */
public class UserResponse {

    /**
     * 사용자 정보 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserInfo {
        private Long id;
        private String userId;
        private String email;
        private String name;
        private String phoneNumber;
        private LocalDate birthDate;
        private String gender;
        private String address;
        private Double latitude;
        private Double longitude;
        private String profileImageUrl;
        private String role;
        private String provider;
        private String providerId;
        private Boolean isActive;
        private Boolean emailVerified;
        private Boolean registrationCompleted;
        private LocalDateTime lastLoginAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /**
     * 로그인 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Login {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private Long expiresIn;
        private Long refreshExpiresIn;
        private UserInfo user;
        private boolean isNewUser;
    }

    /**
     * 토큰 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Token {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private Long expiresIn;
        private Long refreshExpiresIn;
    }

    /**
     * 토큰 검증 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TokenValidation {
        private boolean valid;
        private String email;
        private String role;
        private LocalDateTime expiresAt;
    }

    /**
     * 프로필 완성도 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProfileCompletion {
        private boolean isComplete;
        private int completionPercentage;
        private String message;
        private MissingFields missingFields;
        
        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class MissingFields {
            private boolean needsRealName;
            private boolean needsPhoneNumber;
            private boolean needsBirthDate;
            private boolean needsGender;
            private boolean needsAddress;
        }
    }

    /**
     * 사용자 통계 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserStats {
        private long totalUsers;
        private long activeUsers;
        private long verifiedUsers;
        private long newUsersToday;
        private long newUsersThisWeek;
        private long newUsersThisMonth;
    }

    /**
     * 사용자 목록 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserList {
        private List<UserInfo> users;
        private long totalCount;
        private int currentPage;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
    }

    /**
     * 사용자 검색 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserSearch {
        private List<UserInfo> users;
        private long totalCount;
        private String searchKeyword;
        private List<String> searchFilters;
    }

    /**
     * 위치 기반 사용자 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NearbyUsers {
        private List<UserInfo> users;
        private double centerLatitude;
        private double centerLongitude;
        private double radius;
        private int count;
    }

    /**
     * 사용자 활동 통계 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserActivity {
        private String userId;
        private String userName;
        private LocalDateTime lastLoginAt;
        private int loginCount;
        private int postCount;
        private int commentCount;
        private int bookingCount;
        private LocalDateTime createdAt;
    }
}
