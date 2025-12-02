package com.carecode.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 프로필 누락 필드 정보
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileMissingFields {
    private boolean needsRealName;
    private boolean needsPhoneNumber;
    private boolean needsBirthDate;
    private boolean needsGender;
    private boolean needsAddress;
}
