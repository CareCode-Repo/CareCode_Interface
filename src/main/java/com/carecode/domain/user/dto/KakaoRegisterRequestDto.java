package com.carecode.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 카카오 회원가입 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "카카오 회원가입 요청")
public class KakaoRegisterRequestDto {
    
    @Schema(description = "카카오 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c", required = true)
    private String kakaoAccessToken;
    
    @Schema(description = "이메일", example = "test@example.com", required = true)
    private String email;
    
    @Schema(description = "전화번호", example = "010-1234-5678", required = true)
    private String phoneNumber;
    
    @Schema(description = "생년월일", example = "1998-09-15", required = true)
    private LocalDate birthDate;
    
    @Schema(description = "성별", example = "MALE", allowableValues = {"MALE", "FEMALE"}, required = true)
    private String gender;
    
    @Schema(description = "주소", example = "서울시 강남구 테헤란로 123", required = true)
    private String address;
}
