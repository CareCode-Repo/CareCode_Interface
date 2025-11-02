package com.carecode.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
public class KakaoDto {
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OAuthToken {
        private String access_token;
        private String token_type;
        private String refresh_token;
        private String id_token;
        private Integer expires_in;
        private Integer refresh_token_expires_in;
        private String scope;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KakaoProfile {
        private Long id;
        private String connected_at;
        private KakaoAccount kakao_account;
        private Properties properties;
        
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class KakaoAccount {
            private String email;
            private Profile profile;
            private Boolean email_needs_agreement;
            private Boolean is_email_valid;
            private Boolean is_email_verified;
            
            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Profile {
                private String nickname;
                private String thumbnail_image_url;
                private String profile_image_url;
                private Boolean is_default_image;
            }
        }
        
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Properties {
            private String nickname;
            private String profile_image;
            private String thumbnail_image;
        }
    }
}
