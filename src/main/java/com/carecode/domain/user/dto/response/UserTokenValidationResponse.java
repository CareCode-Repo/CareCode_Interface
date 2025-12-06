package com.carecode.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 토큰 검증 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTokenValidationResponse {
    private boolean valid;
    private String email;
    private String role;
    private LocalDateTime expiresAt;
}

