package com.carecode.core.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 카카오 사용자 정보 추출 유틸리티
 */
@Slf4j
@Component
public class KakaoUserInfoExtractor {

    /**
     * 카카오 사용자 정보에서 닉네임 추출
     */
    public String extractNickname(Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        if (kakaoAccount != null && kakaoAccount.containsKey("profile")) {
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            if (profile != null) {
                return (String) profile.get("nickname");
            }
        }

        return null;
    }

    /**
     * 카카오 사용자 정보에서 프로필 이미지 URL 추출
     */
    public String extractProfileImageUrl(Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        if (kakaoAccount != null && kakaoAccount.containsKey("profile")) {
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            if (profile != null) {
                return (String) profile.get("profile_image_url");
            }
        }

        return null;
    }

    /**
     * 카카오 사용자 정보에서 ID 추출
     */
    public String extractKakaoId(Map<String, Object> attributes) {
        try {
            Object id = attributes.get("id");
            return id != null ? id.toString() : null;
        } catch (Exception e) {
            log.warn("카카오 ID 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 카카오 사용자 정보에서 이메일 추출
     */
    public String extractEmail(Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        if (kakaoAccount != null) {
            return (String) kakaoAccount.get("email");
        }

        return null;
    }

    /**
     * 고유한 사용자명 생성 (닉네임 + 카카오ID)
     */
    public String generateUniqueUserName(Map<String, Object> attributes) {
        String nickname = extractNickname(attributes);
        String kakaoId = extractKakaoId(attributes);
        
        if (nickname == null || kakaoId == null) {
            log.warn("카카오 사용자 정보 추출 실패 - nickname: {}, kakaoId: {}", nickname, kakaoId);
            return null;
        }
        
        return nickname + "_" + kakaoId;
    }
}
