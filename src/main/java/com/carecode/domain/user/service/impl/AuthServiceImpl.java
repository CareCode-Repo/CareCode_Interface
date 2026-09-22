package com.carecode.domain.user.service.impl;

import com.carecode.core.exception.BusinessException;
import com.carecode.core.exception.CareCodeException;
import com.carecode.core.exception.CareServiceException;
import com.carecode.core.exception.ErrorCode;
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

            String providerId = kakaoProfile.getId().toString();
            String nickname = extractNickname(kakaoProfile);
            boolean emailTrusted = isKakaoEmailTrusted(kakaoProfile);
            // 검증되지 않은 이메일은 계정 식별에도, 새 계정의 주소로도 쓰지 않는다.
            // 새 계정에 그대로 넣으면 이메일 유니크 제약과 부딪히고, 나중에 검증이 끝났을 때
            // 그 주소의 진짜 주인 계정과 연결 근거가 되어 버린다.
            String email = emailTrusted ? kakaoProfile.getKakao_account().getEmail() : fallbackEmail(kakaoProfile);

            log.debug("카카오 사용자 식별 완료: providerId={}, emailTrusted={}", providerId, emailTrusted);

            // 1) 카카오 계정 자체(providerId)로 먼저 찾는다. 이것이 흔들리지 않는 식별자다.
            //    예전에는 이메일로 먼저 찾아서, 카카오 이메일을 바꾸면 다른 계정으로 로그인됐다.
            Optional<User> queryUser = userRepository.findByProviderAndProviderIdAndDeletedAtIsNull("kakao", providerId);

            // 2) 처음 보는 카카오 계정이면, 카카오가 검증한 이메일일 때만 기존 계정과 연결한다.
            //
            //    이메일로 연결한다는 건 "그 이메일의 계정 소유권을 넘긴다" 는 뜻이다.
            //    카카오 계정 이메일은 검증되지 않았을 수 있고, 카카오는 응답에 is_email_valid /
            //    is_email_verified 를 주면서 연결 전에 둘 다 확인하라고 안내한다. 이 코드는 두 값을
            //    DTO 로 받아 놓고 보지 않았다. 그래서 검증되지 않은 이메일을 단 카카오 계정으로
            //    같은 주소의 기존 계정 — 관리자 포함 — 의 토큰을 받을 수 있었다.
            boolean linkingExistingAccount = false;
            if (queryUser.isEmpty() && emailTrusted) {
                queryUser = userRepository.findByEmailAndDeletedAtIsNull(email);
                linkingExistingAccount = queryUser.isPresent();
            }

            User user;
            boolean isNewUser = false;

            if (queryUser.isPresent()) {
                user = queryUser.get();

                // 정지된 계정은 로그인할 수 없다. 이메일 로그인은 막는데 여기만 비어 있어서,
                // 관리자가 정지한 계정도 카카오로 들어오면 그대로 토큰을 받았다.
                // 아무것도 저장하기 전에 확인한다.
                if (!Boolean.TRUE.equals(user.getIsActive())) {
                    log.warn("비활성 계정의 카카오 로그인 거부: userId={}", user.getUserId());
                    throw new BusinessException(ErrorCode.USER_INACTIVE, "비활성화된 계정입니다.");
                }

                if (linkingExistingAccount) {
                    user.setProvider("kakao");
                    user.setProviderId(providerId);
                    // 이메일로 이미 가입을 마친 계정이다. 이메일 가입은 registrationCompleted 가
                    // 기본 false(@PrePersist)라 그대로 두면 provider=kakao 와 합쳐져 가입 완료 API 를
                    // 통과하고, 그 절차로 역할을 다시 고르게 된다(관리자가 PARENT 로 덮어쓰는 식).
                    user.setRegistrationCompleted(true);
                    log.info("기존 계정에 카카오 연결: userId={}", user.getUserId());
                }

                user.setLastLoginAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);

                // registrationCompleted 는 Boolean 이다. null 이면 !언박싱에서 NPE 로 로그인 전체가 500 이 된다.
                if (!linkingExistingAccount
                        && "kakao".equals(user.getProvider())
                        && !Boolean.TRUE.equals(user.getRegistrationCompleted())) {
                    isNewUser = true;
                }
            } else {
                user = createNewUser(email, emailTrusted, providerId, nickname);
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
        } catch (CareCodeException e) {
            // USER_INACTIVE 같은 의도된 거부를 "카카오 OAuth 처리 실패" 로 뭉개지 않는다.
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

    /**
     * 카카오가 이메일을 "유효하고 검증됐다" 고 명시했을 때만 믿는다.
     * 값이 없으면(null) 검증되지 않은 것으로 본다 — 모르면 믿지 않는다.
     */
    private boolean isKakaoEmailTrusted(KakaoProfile kakaoProfile) {
        KakaoAccount account = kakaoProfile.getKakao_account();
        return account != null
                && account.getEmail() != null
                && !account.getEmail().isBlank()
                && Boolean.TRUE.equals(account.getIs_email_valid())
                && Boolean.TRUE.equals(account.getIs_email_verified());
    }

    /** 카카오 ID 로 만드는 대체 주소. 실제로 메일을 받을 수 없으며 계정 식별에만 쓴다. */
    private String fallbackEmail(KakaoProfile kakaoProfile) {
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

    private User createNewUser(String email, boolean emailTrusted, String providerId, String nickname) {
        return User.builder()
                .name(nickname)
                .email(email)
                .role(UserRole.PARENT)
                .provider("kakao")
                .providerId(providerId)
                .password(null)
                .isActive(true)
                // 예전에는 무조건 true 였다. 카카오가 검증하지 않은 주소를 검증됐다고 적으면
                // 이메일 인증을 전제로 하는 기능이 그 주소를 믿게 된다.
                .emailVerified(emailTrusted)
                .registrationCompleted(false)
                .build();
    }
}
