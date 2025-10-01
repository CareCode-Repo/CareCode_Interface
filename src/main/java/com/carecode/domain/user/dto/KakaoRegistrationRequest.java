package com.carecode.domain.user.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 카카오 신규 사용자 역할 설정 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KakaoRegistrationRequest {
    
    @NotBlank(message = "이름은 필수입니다.")
    private String name; // 사용자 이름 (카카오 닉네임 또는 사용자가 설정한 이름)
    
    @NotBlank(message = "역할은 필수입니다.")
    @Pattern(regexp = "^(PARENT|CAREGIVER|ADMIN|GUEST)$", message = "유효한 역할을 선택해주세요.")
    private String role; // 선택한 역할 (PARENT, CAREGIVER 등)
    
}
