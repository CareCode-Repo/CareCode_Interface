package com.carecode.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 카카오 OAuth 토큰
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoOAuthToken {
    private String access_token;
    private String token_type;
    private String refresh_token;
    private String id_token;
    private Integer expires_in;
    private Integer refresh_token_expires_in;
    private String scope;
}

