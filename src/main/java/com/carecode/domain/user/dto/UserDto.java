package com.carecode.domain.user.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 사용자 정보 전송 객체
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    
    private Long id;
    private String userId;
    private String email;
    private String password; // 생성 시에만 사용
    private String name;
    private String phoneNumber;
    private LocalDate birthDate;
    private String gender;
    private String address;
    private Double latitude;
    private Double longitude;
    private String profileImageUrl;
    private String role;
    private String provider; // OAuth 제공자 (kakao, google, naver 등)
    private String providerId; // OAuth 제공자의 사용자 ID
    private Boolean isActive;
    private Boolean emailVerified;
    private Boolean registrationCompleted; // 카카오 사용자 가입 프로세스 완료 여부
    private LocalDateTime deletedAt; // 소프트 삭제를 위한 삭제 시간
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * 사용자 통계 정보
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
    }
} 