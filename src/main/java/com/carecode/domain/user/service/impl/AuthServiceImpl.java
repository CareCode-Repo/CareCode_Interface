package com.carecode.domain.user.service.impl;

import com.carecode.core.exception.CareServiceException;
import com.carecode.core.util.KakaoUtil;
import com.carecode.domain.user.dto.response.KakaoOAuthToken;
import com.carecode.domain.user.dto.response.KakaoProfile;
import com.carecode.domain.user.dto.response.KakaoAccount;
import com.carecode.domain.user.dto.response.KakaoProperties;
import com.carecode.domain.user.dto.response.TokenDto;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.entity.UserRole;
import com.carecode.domain.user.repository.UserRepository;
import com.carecode.domain.user.service.AuthService;
import com.carecode.domain.user.service.JwtService;
import com.carecode.domain.user.service.UserService;
import com.carecode.domain.user.service.refreshtoken.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final KakaoUtil kakaoUtil;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserService userService;
    private final RefreshTokenStore refreshTokenStore;

    @Override
    public TokenDto oAuthLoginOrRegister(String accessCode) {
        log.debug("카카오 OAuth 처리 시작 (authorization code 수신, 값은 로그에 기록하지 않음)");

        try {
            if (accessCode == null || accessCode.trim().isEmpty()) {
                throw new IllegalArgumentException("인증 코드가 비어있습니다.");
            }

            if (!accessCode.matches("^[A-Za-z0-9_-]+$")) {
                throw new IllegalArgumentException("유효하지 않은 인증 코드 형식입니다.");
            }

            KakaoOAuthToken oAuthToken = kakaoUtil.requestToken(accessCode);
            if (oAuthToken == null || oAuthToken.getAccess_token() == null) {
                throw new CareServiceException("KAKAO_TOKEN", "카카오 액세스 토큰을 받지 못했습니다.");
            }

            KakaoProfile kakaoProfile = kakaoUtil.requestProfile(oAuthToken);
            if (kakaoProfile == null || kakaoProfile.getKakao_account() == null) {
                throw new CareServiceException("KAKAO_PROFILE", "카카오 사용자 정보를 받지 못했습니다.");
            }

            String email = extractEmail(kakaoProfile);
            String providerId = kakaoProfile.getId().toString();
            String nickname = extractNickname(kakaoProfile);

            log.debug("카카오 사용자 식별 완료: email={}, providerId={}", email, providerId);

            Optional<User> queryUser = userRepository.findByEmailAndDeletedAtIsNull(email);
            if (queryUser.isEmpty()) {
                queryUser = userRepository.findByProviderAndProviderIdAndDeletedAtIsNull("kakao", providerId);
            }

            User user;
            boolean isNewUser = false;

            if (queryUser.isPresent()) {
                user = queryUser.get();

                if (!"kakao".equals(user.getProvider()) || !providerId.equals(user.getProviderId())) {
                    user.setProvider("kakao");
                    user.setProviderId(providerId);
                    log.debug("프로바이더 정보 업데이트: userId={}", user.getUserId());
                }

                user.setLastLoginAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);

                if ("kakao".equals(user.getProvider()) && !user.getRegistrationCompleted()) {
                    isNewUser = true;
                }
            } else {
                user = createNewUser(kakaoProfile, email, providerId, nickname);
                userRepository.save(user);
                isNewUser = true;
            }

            String message = isNewUser ? "카카오 회원가입 성공!" : "카카오 로그인 성공!";
            TokenDto tokens = issueTokenForUser(user, message);
            tokens.setIsNewUser(isNewUser);
            log.info("카카오 OAuth 완료: userId={}, isNewUser={}", user.getUserId(), isNewUser);
            return tokens;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (CareServiceException e) {
            throw e;
        } catch (Exception e) {
            log.warn("카카오 OAuth 처리 실패: {}", e.getMessage());
            throw new CareServiceException("KAKAO_OAUTH", "카카오 OAuth 처리에 실패했습니다.", e);
        }
    }

    @Override
    public TokenDto issueTokenForUser(User user, String message) {
        String accessToken = jwtService.generateAccessToken(
                user.getUserId(),
                user.getEmail(),
                user.getRole().name(),
                user.getName()
        );

        String refreshToken = jwtService.generateRefreshToken(
                user.getUserId(),
                user.getEmail()
        );

        refreshTokenStore.register(user.getUserId(), refreshToken);

        return TokenDto.builder()
                .success(true)
                .message(message)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationMs())
                .refreshExpiresIn(jwtService.getRefreshTokenExpirationMs())
                .user(userService.convertToDto(user))
                .build();
    }

    private String extractEmail(KakaoProfile kakaoProfile) {
        try {
            KakaoAccount kakaoAccount = kakaoProfile.getKakao_account();
            if (kakaoAccount != null && kakaoAccount.getEmail() != null) {
                return kakaoAccount.getEmail();
            }
        } catch (Exception e) {
            log.debug("kakao_account에서 이메일 추출 실패: {}", e.getMessage());
        }

        return "kakao_" + kakaoProfile.getId() + "@kakao.com";
    }

    private String extractNickname(KakaoProfile kakaoProfile) {
        try {
            KakaoAccount kakaoAccount = kakaoProfile.getKakao_account();
            if (kakaoAccount != null && kakaoAccount.getProfile() != null) {
                String nickname = kakaoAccount.getProfile().getNickname();
                if (nickname != null && !nickname.trim().isEmpty()) {
                    return nickname;
                }
            }

            KakaoProperties properties = kakaoProfile.getProperties();
            if (properties != null && properties.getNickname() != null) {
                String nickname = properties.getNickname();
                if (!nickname.trim().isEmpty()) {
                    return nickname;
                }
            }
        } catch (Exception e) {
            log.debug("닉네임 추출 실패: {}", e.getMessage());
        }

        return "카카오사용자_" + kakaoProfile.getId();
    }

    private User createNewUser(KakaoProfile kakaoProfile, String email, String providerId, String nickname) {
        return User.builder()
                .name(nickname)
                .email(email)
                .role(UserRole.PARENT)
                .provider("kakao")
                .providerId(providerId)
                .password(null)
                .isActive(true)
                .emailVerified(true)
                .registrationCompleted(false)
                .build();
    }
}
