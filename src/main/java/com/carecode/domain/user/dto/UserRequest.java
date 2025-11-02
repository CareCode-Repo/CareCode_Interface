package com.carecode.domain.user.dto;

import com.carecode.domain.user.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 사용자 관련 요청 DTO 통합 클래스
 */
public class UserRequest {

    /**
     * 로그인 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Login {
        @Email(message = "올바른 이메일 형식이 아닙니다")
        @NotBlank(message = "이메일은 필수입니다")
        private String email;
        
        @NotBlank(message = "비밀번호는 필수입니다")
        private String password;
    }

    /**
     * 회원가입 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Register {
        @Email(message = "올바른 이메일 형식이 아닙니다")
        @NotBlank(message = "이메일은 필수입니다")
        private String email;
        
        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
        private String password;
        
        @NotBlank(message = "이름은 필수입니다")
        @Size(min = 2, max = 10, message = "이름은 2-10자 사이여야 합니다")
        private String name;
        
        @Pattern(regexp = "^01[0-9]-[0-9]{3,4}-[0-9]{4}$", message = "올바른 휴대폰 번호 형식이 아닙니다 (예: 010-1234-5678)")
        private String phoneNumber;
        
        private LocalDate birthDate;
        private User.Gender gender;
        private String address;
        private Double latitude;
        private Double longitude;
    }

    /**
     * 프로필 업데이트 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateProfile {
        @NotBlank(message = "이름은 필수입니다")
        @Size(min = 2, max = 10, message = "이름은 2-10자 사이여야 합니다")
        private String realName;
        
        @Pattern(regexp = "^01[0-9]-[0-9]{3,4}-[0-9]{4}$", message = "올바른 휴대폰 번호 형식이 아닙니다 (예: 010-1234-5678)")
        private String phoneNumber;
        
        private LocalDate birthDate;
        private User.Gender gender;
        
        @Size(max = 200, message = "주소는 200자 이하여야 합니다")
        private String address;
        
        private Double latitude;
        private Double longitude;
    }

    /**
     * 비밀번호 변경 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangePassword {
        @NotBlank(message = "현재 비밀번호는 필수입니다")
        private String currentPassword;
        
        @NotBlank(message = "새 비밀번호는 필수입니다")
        @Size(min = 8, message = "새 비밀번호는 최소 8자 이상이어야 합니다")
        private String newPassword;
    }

    /**
     * 닉네임 변경 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateNickname {
        @NotBlank(message = "닉네임은 필수입니다")
        @Size(min = 2, max = 10, message = "닉네임은 2-10자 사이여야 합니다")
        private String nickname;
    }

    /**
     * 카카오 회원가입 완료 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KakaoRegistration {
        @NotBlank(message = "이름은 필수입니다")
        @Size(min = 2, max = 10, message = "이름은 2-10자 사이여야 합니다")
        private String name;
        
        @NotBlank(message = "역할은 필수입니다")
        private String role;
    }

    /**
     * 토큰 갱신 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshToken {
        @NotBlank(message = "리프레시 토큰은 필수입니다")
        private String refreshToken;
    }

    /**
     * 토큰 검증 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidateToken {
        @NotBlank(message = "토큰은 필수입니다")
        private String token;
    }
}
