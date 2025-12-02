package com.carecode.domain.health.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 아동 정보 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChildInfoResponse {
    private Long id;
    private Long userId;
    private String name;
    private String birthDate;
    private String gender;
    private String createdAt;
    private String updatedAt;
}

