package com.carecode.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 사용자 정보 전송 객체 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    
    private Long id;
    private String userId;
    private String email;

    /** 회원가입/수정 요청에서만 사용한다. */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
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
    
} 