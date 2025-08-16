package com.carecode.core.security;

import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();
        
        // 닉네임과 고유 ID 추출
        final String userName = extractName(registrationId, attributes);
        final String kakaoId = extractKakaoId(registrationId, attributes);
        final String profileImageUrl = extractProfileImageUrl(registrationId, attributes);
        
        // 이메일 생성 (카카오 ID 기반)
        final String userEmail = kakaoId + "@kakao.temp";
        
        // 고유한 사용자명 생성 (닉네임 + 카카오ID)
        final String uniqueUserName = userName + "_" + kakaoId;
        
        // 사용자 조회만 수행 (자동 회원가입 제거)
        User user = userRepository.findByName(uniqueUserName)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. 회원가입이 필요합니다: " + uniqueUserName));
        
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole())),
                attributes,
                "id" // 카카오의 경우 id를 name attribute로 사용
        );
    }

    private String extractEmail(String registrationId, Map<String, Object> attributes) {
        if ("google".equals(registrationId)) {
            return (String) attributes.get("email");
        } else if ("kakao".equals(registrationId)) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            return (String) kakaoAccount.get("email");
        }
        return null;
    }

    private String extractName(String registrationId, Map<String, Object> attributes) {
        if ("google".equals(registrationId)) {
            return (String) attributes.get("name");
        } else if ("kakao".equals(registrationId)) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            if (kakaoAccount != null && kakaoAccount.containsKey("profile")) {
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                return (String) profile.get("nickname");
            }
        }
        return "";
    }

    private String extractProfileImageUrl(String registrationId, Map<String, Object> attributes) {
        if ("google".equals(registrationId)) {
            return (String) attributes.get("picture");
        } else if ("kakao".equals(registrationId)) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            if (kakaoAccount != null && kakaoAccount.containsKey("profile")) {
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                return (String) profile.get("profile_image_url");
            }
        }
        return null;
    }

    private String extractKakaoId(String registrationId, Map<String, Object> attributes) {
        if ("kakao".equals(registrationId)) {
            Object id = attributes.get("id");
            return id != null ? id.toString() : null;
        }
        return null;
    }
} 