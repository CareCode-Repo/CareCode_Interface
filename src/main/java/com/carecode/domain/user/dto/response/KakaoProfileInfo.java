package com.carecode.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 카카오 프로필 정보
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoProfileInfo {
    private String nickname;
    private String thumbnail_image_url;
    private String profile_image_url;
    private Boolean is_default_image;
}

