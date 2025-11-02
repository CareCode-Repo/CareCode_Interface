package com.carecode.domain.user.service.impl;

import com.carecode.core.util.KakaoUtil;
import com.carecode.domain.user.dto.KakaoDto;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import com.carecode.domain.user.service.AuthService;
import com.carecode.domain.user.service.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;

    @Override
    public User oAuthLoginOrRegister(String accessCode, HttpServletResponse httpServletResponse) {
        log.info("카카오 OAuth 처리 시작: code={}", accessCode);
        
        try {
            // 입력값 검증
            if (accessCode == null || accessCode.trim().isEmpty()) {
                throw new IllegalArgumentException("인증 코드가 비어있습니다.");
            }
            
            // 인증 코드 형식 검증
            if (!accessCode.matches("^[A-Za-z0-9_-]+$")) {
                throw new IllegalArgumentException("유효하지 않은 인증 코드 형식입니다.");
            }
            
            // 1. 카카오 액세스 토큰 요청
            KakaoDto.OAuthToken oAuthToken = kakaoUtil.requestToken(accessCode);
            if (oAuthToken == null || oAuthToken.getAccess_token() == null) {
                throw new RuntimeException("카카오 액세스 토큰을 받지 못했습니다.");
            }
            
            // 2. 카카오 사용자 정보 요청
            KakaoDto.KakaoProfile kakaoProfile = kakaoUtil.requestProfile(oAuthToken);
            if (kakaoProfile == null || kakaoProfile.getKakao_account() == null) {
                throw new RuntimeException("카카오 사용자 정보를 받지 못했습니다.");
            }
            
            // 3. 사용자 식별 정보 추출
            String email = extractEmail(kakaoProfile);
            String providerId = kakaoProfile.getId().toString();
            String nickname = extractNickname(kakaoProfile);
            
            log.info("카카오 사용자 정보 추출: email={}, providerId={}, nickname={}", email, providerId, nickname);
            
            // 4. 사용자 조회 (이메일 또는 프로바이더 ID로) - 삭제되지 않은 사용자만
            Optional<User> queryUser = userRepository.findByEmailAndDeletedAtIsNull(email);
            if (queryUser.isEmpty()) {
                queryUser = userRepository.findByProviderAndProviderIdAndDeletedAtIsNull("kakao", providerId);
            }

            User user;
            boolean isNewUser = false;
            
            if (queryUser.isPresent()) {
                // 기존 사용자 로그인
                user = queryUser.get();
                
                // 프로바이더 정보 업데이트 (필요한 경우)
                if (!"kakao".equals(user.getProvider()) || !providerId.equals(user.getProviderId())) {
                    user.setProvider("kakao");
                    user.setProviderId(providerId);
                    log.info("프로바이더 정보 업데이트: userId={}, provider=kakao, providerId={}", user.getUserId(), providerId);
                }
                
                // 마지막 로그인 시간 업데이트
                user.setLastLoginAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);
                
                // 카카오 사용자의 경우 가입 프로세스 완료 여부로 isNewUser 결정
                if ("kakao".equals(user.getProvider()) && !user.getRegistrationCompleted()) {
                    isNewUser = true;
                    log.info("카카오 사용자 가입 미완료: userId={}, email={}, registrationCompleted={}", 
                        user.getUserId(), user.getEmail(), user.getRegistrationCompleted());
                } else {
                    log.info("카카오 사용자 가입 완료 또는 일반 사용자: userId={}, email={}, registrationCompleted={}", 
                        user.getUserId(), user.getEmail(), user.getRegistrationCompleted());
                }
                
                log.info("기존 사용자 로그인: userId={}, email={}, role={}, registrationCompleted={}", 
                    user.getUserId(), user.getEmail(), user.getRole(), user.getRegistrationCompleted());
            } else {
                // 신규 사용자 생성
                user = createNewUser(kakaoProfile, email, providerId, nickname);
                userRepository.save(user);
                isNewUser = true;
                
                log.info("신규 사용자 생성: userId={}, email={}, role={}, registrationCompleted={}", 
                    user.getUserId(), user.getEmail(), user.getRole(), user.getRegistrationCompleted());
            }
            
            // JWT 토큰 생성 (Access Token + Refresh Token)
            String accessToken = jwtService.generateAccessToken(
                    user.getUserId(),
                    user.getEmail(),
                    user.getRole().name(),
                    user.getName() // name 포함
            );
            
            String refreshToken = jwtService.generateRefreshToken(
                    user.getUserId(),
                    user.getEmail()
            );
            
            httpServletResponse.setHeader("Authorization", "Bearer " + accessToken);
            httpServletResponse.setHeader("X-Refresh-Token", refreshToken);
            httpServletResponse.setHeader("X-New-User", String.valueOf(isNewUser));
            
            return user;
            
        } catch (Exception e) {
            log.error("카카오 OAuth 처리 실패: {}", e.getMessage(), e);
            throw new RuntimeException("카카오 OAuth 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 이메일 추출 (null 안전)
     */
    private String extractEmail(KakaoDto.KakaoProfile kakaoProfile) {
        try {
            // kakao_account에서 이메일 추출
            KakaoDto.KakaoProfile.KakaoAccount kakaoAccount = kakaoProfile.getKakao_account();
            if (kakaoAccount != null && kakaoAccount.getEmail() != null) {
                return kakaoAccount.getEmail();
            }
        } catch (Exception e) {
            log.warn("kakao_account에서 이메일 추출 실패: {}", e.getMessage());
        }
        
        // 이메일이 없는 경우 기본 이메일 생성
        return "kakao_" + kakaoProfile.getId() + "@kakao.com";
    }
    
    /**
     * 닉네임 추출 (null 안전)
     */
    private String extractNickname(KakaoDto.KakaoProfile kakaoProfile) {
        try {
            // 1. kakao_account.profile에서 닉네임 추출
            KakaoDto.KakaoProfile.KakaoAccount kakaoAccount = kakaoProfile.getKakao_account();
            if (kakaoAccount != null && kakaoAccount.getProfile() != null) {
                String nickname = kakaoAccount.getProfile().getNickname();
                if (nickname != null && !nickname.trim().isEmpty()) {
                    return nickname;
                }
            }
            
            // 2. properties에서 닉네임 추출
            KakaoDto.KakaoProfile.Properties properties = kakaoProfile.getProperties();
            if (properties != null && properties.getNickname() != null) {
                String nickname = properties.getNickname();
                if (!nickname.trim().isEmpty()) {
                    return nickname;
                }
            }
        } catch (Exception e) {
            log.warn("닉네임 추출 실패: {}", e.getMessage());
        }
        
        // 닉네임이 없는 경우 기본값 사용
        return "카카오사용자_" + kakaoProfile.getId();
    }
    
    /**
     * 신규 사용자 생성
     */
    private User createNewUser(KakaoDto.KakaoProfile kakaoProfile, String email, String providerId, String nickname) {
        User newUser = User.builder()
                .name(nickname) // 카카오 닉네임 사용
                .email(email)
                .role(User.UserRole.PARENT) // 기본 역할 (나중에 변경 가능)
                .provider("kakao")
                .providerId(providerId)
                .password(null) // 카카오 사용자는 비밀번호 불필요
                .isActive(true)
                .emailVerified(true) // 카카오 사용자는 이메일 인증 완료
                .registrationCompleted(false) // 가입 프로세스 미완료 (기본값)
                .build();
        
        return newUser;
    }
}
