package com.carecode.domain.health.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 아동 정보 생성 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthCreateChildRequest {
    @NotNull(message = "사용자 ID는 필수입니다")
    private Long userId;
    
    @NotBlank(message = "아동 이름은 필수입니다")
    private String name;
    
    @NotNull(message = "생년월일은 필수입니다")
    private LocalDate birthDate;
    
    @NotBlank(message = "성별은 필수입니다")
    private String gender;
}

