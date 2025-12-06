package com.carecode.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 카카오 프로퍼티
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoProperties {
    private String nickname;
    private String profile_image;
    private String thumbnail_image;
}

