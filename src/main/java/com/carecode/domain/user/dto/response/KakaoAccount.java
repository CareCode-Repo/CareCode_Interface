package com.carecode.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 카카오 계정 정보
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoAccount {
    private String email;
    private KakaoProfileInfo profile;
    private Boolean email_needs_agreement;
    private Boolean is_email_valid;
    private Boolean is_email_verified;
}

