package com.carecode.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 카카오 프로필
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoProfile {
    private Long id;
    private String connected_at;
    private KakaoAccount kakao_account;
    private KakaoProperties properties;
}

