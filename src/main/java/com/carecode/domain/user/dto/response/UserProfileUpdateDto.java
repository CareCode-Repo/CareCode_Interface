package com.carecode.domain.user.dto.response;

import com.carecode.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;

/**
 * 사용자 추가 정보 입력/업데이트 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileUpdateDto {
    
    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 2, max = 10, message = "이름은 2-10자 사이여야 합니다")
    private String realName; // 실제 이름 (카카오 닉네임과 구분)
    
    @Pattern(regexp = "^01[0-9]-[0-9]{3,4}-[0-9]{4}$", message = "올바른 휴대폰 번호 형식이 아닙니다 (예: 010-1234-5678)")
    private String phoneNumber;
    
    private LocalDate birthDate;
    
    private User.Gender gender;
    
    @Size(max = 200, message = "주소는 200자 이하여야 합니다")
    private String address;
    
    private Double latitude;
    
    private Double longitude;
}
