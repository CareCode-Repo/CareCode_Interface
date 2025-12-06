package com.carecode.domain.community.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 태그 생성 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityCreateTagRequest {
    @NotBlank(message = "태그 이름은 필수입니다")
    @Size(max = 20, message = "태그 이름은 20자 이하여야 합니다")
    private String name;
    
    private String description;
    private String color;
}

