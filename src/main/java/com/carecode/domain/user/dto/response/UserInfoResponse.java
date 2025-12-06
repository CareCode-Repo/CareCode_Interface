package com.carecode.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 사용자 정보 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoResponse {
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

