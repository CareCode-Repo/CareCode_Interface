package com.carecode.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 토큰 검증 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserValidateTokenRequest {
    @NotBlank(message = "토큰은 필수입니다")
    private String token;
}

