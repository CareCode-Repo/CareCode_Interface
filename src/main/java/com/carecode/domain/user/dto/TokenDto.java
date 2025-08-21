package com.carecode.domain.user.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * JWT 토큰 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenDto {
    
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private Long refreshExpiresIn;
    private String userId;
    private String email;
    private String role;
    private Boolean success;
    private String message;
    private UserDto user;
    

    
    /**
     * 토큰 검증 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenValidationRequest {
        private String accessToken;
    }
    
    /**
     * 토큰 검증 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TokenValidationResponse {
        private boolean valid;
        private String userId;
        private String email;
        private String role;
        private String message;
    }
} 