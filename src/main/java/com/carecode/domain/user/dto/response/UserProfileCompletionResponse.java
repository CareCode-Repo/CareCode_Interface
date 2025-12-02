package com.carecode.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 프로필 완성도 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileCompletionResponse {
    private boolean isComplete;
    private int completionPercentage;
    private String message;
    private UserProfileMissingFields missingFields;
    private int completedFields;
    private int totalFields;
}
