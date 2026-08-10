package com.carecode.domain.user.dto.request;

import com.carecode.domain.user.entity.ConsentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 동의/철회 요청. */
@Getter
@Setter
@NoArgsConstructor
public class ConsentUpdateRequest {

    @NotNull(message = "동의 항목은 필수입니다")
    private ConsentType consentType;

    @NotBlank(message = "약관 버전은 필수입니다")
    private String policyVersion;

    private boolean granted;
}
