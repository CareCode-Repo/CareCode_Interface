package com.carecode.domain.user.service.impl;

import com.carecode.core.exception.CareCodeException;
import com.carecode.core.exception.ErrorCode;
import com.carecode.core.util.KakaoUtil;
import com.carecode.domain.user.dto.response.KakaoAccount;
import com.carecode.domain.user.dto.response.KakaoOAuthToken;
import com.carecode.domain.user.dto.response.KakaoProfile;
import com.carecode.domain.user.dto.response.KakaoProfileInfo;
import com.carecode.domain.user.dto.response.TokenDto;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.entity.UserRole;
import com.carecode.domain.user.repository.UserRepository;
import com.carecode.domain.user.service.JwtService;
import com.carecode.domain.user.service.UserService;
import com.carecode.domain.user.service.refreshtoken.RefreshTokenStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 카카오 로그인의 계정 연결 규칙.
 *
 * <p>이 경로는 "카카오가 알려준 이메일" 로 기존 계정을 찾아 그 계정의 토큰을 발급한다.
 * 즉 <b>카카오 이메일을 믿는 순간 그 이메일의 계정 소유권을 넘겨주는 것</b>과 같다.
 *
 * <p>카카오 계정의 이메일은 검증되지 않았을 수 있다. 카카오는 응답에
 * {@code is_email_valid}, {@code is_email_verified} 를 함께 주고, 계정 연결에 쓰기 전에
 * 둘 다 확인하라고 안내한다. 이 코드는 DTO 로 두 값을 받아 놓고 한 번도 보지 않았다.
 * 그래서 검증되지 않은 이메일로 남의 계정 — 관리자 계정 포함 — 에 로그인할 수 있었다.
 *
 * <p>또 이메일 로그인은 비활성 계정을 막는데 이 경로는 막지 않아,
 * 관리자가 정지한 계정도 카카오로 로그인하면 그대로 토큰을 받았다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthServiceImpl - 카카오 로그인 계정 연결")
class AuthServiceImplKakaoTest {

    private static final String CODE = "valid_code-123";
    private static final long KAKAO_ID = 4242L;
    private static final String VICTIM_EMAIL = "admin@carecode.example";

    @Mock private KakaoUtil kakaoUtil;
    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock private UserService userService;
    @Mock private RefreshTokenStore refreshTokenStore;

    @InjectMocks private AuthServiceImpl authService;

    private User existingAdmin;

    @BeforeEach
    void setUp() {
        // 이메일로 가입한 기존 관리자 계정. 카카오와는 아무 관계가 없다.
        existingAdmin = User.builder()
                .id(1L).userId("user_admin").email(VICTIM_EMAIL).password("$2a$hash")
                .name("관리자").role(UserRole.ADMIN)
                .isActive(true).emailVerified(true).registrationCompleted(false)
                .build();

        KakaoOAuthToken token = new KakaoOAuthToken();
        token.setAccess_token("kakao-access");
        when(kakaoUtil.requestToken(CODE)).thenReturn(token);

        when(jwtService.generateAccessToken(anyString(), anyString(), anyString(), any())).thenReturn("access");
        when(jwtService.generateRefreshToken(anyString(), anyString())).thenReturn("refresh");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getUserId() == null) u.setUserId("user_new");
            return u;
        });
        when(userRepository.findByProviderAndProviderIdAndDeletedAtIsNull(anyString(), anyString()))
                .thenReturn(Optional.empty());
    }

    private void kakaoSays(String email, Boolean valid, Boolean verified) {
        KakaoAccount account = new KakaoAccount();
        account.setEmail(email);
        account.setIs_email_valid(valid);
        account.setIs_email_verified(verified);
        KakaoProfileInfo info = new KakaoProfileInfo();
        info.setNickname("카카오닉");
        account.setProfile(info);

        KakaoProfile profile = new KakaoProfile();
        profile.setId(KAKAO_ID);
        profile.setKakao_account(account);
        when(kakaoUtil.requestProfile(any())).thenReturn(profile);
    }

    private User savedUser() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("이메일로 기존 계정에 연결")
    class EmailLinking {

        @Test
        @DisplayName("검증되지 않은 카카오 이메일로는 남의 계정에 연결되지 않는다")
        void unverifiedEmailDoesNotTakeOverExistingAccount() {
            kakaoSays(VICTIM_EMAIL, true, false);
            when(userRepository.findByEmailAndDeletedAtIsNull(VICTIM_EMAIL)).thenReturn(Optional.of(existingAdmin));

            authService.oAuthLoginOrRegister(CODE);

            // 관리자 계정의 토큰이 나가면 안 된다.
            verify(jwtService, never()).generateAccessToken(eq("user_admin"), anyString(), anyString(), any());
            assertThat(existingAdmin.getProvider())
                    .as("기존 계정에 카카오가 붙으면 이후로도 계속 그 계정으로 로그인된다")
                    .isNull();

            User created = savedUser();
            assertThat(created).isNotSameAs(existingAdmin);
            assertThat(created.getRole()).isEqualTo(UserRole.PARENT);
            assertThat(created.getEmail())
                    .as("검증되지 않은 이메일을 새 계정에 쓰면 이메일 유니크 제약과 충돌하고, 나중에 다시 연결 근거가 된다")
                    .isNotEqualTo(VICTIM_EMAIL);
        }

        @Test
        @DisplayName("유효하지 않은 이메일도 마찬가지다")
        void invalidEmailDoesNotLink() {
            kakaoSays(VICTIM_EMAIL, false, true);
            when(userRepository.findByEmailAndDeletedAtIsNull(VICTIM_EMAIL)).thenReturn(Optional.of(existingAdmin));

            authService.oAuthLoginOrRegister(CODE);

            assertThat(existingAdmin.getProvider()).isNull();
        }

        @Test
        @DisplayName("검증 여부를 카카오가 안 알려주면 검증되지 않은 것으로 본다")
        void missingVerificationFlagsMeanUnverified() {
            kakaoSays(VICTIM_EMAIL, null, null);
            when(userRepository.findByEmailAndDeletedAtIsNull(VICTIM_EMAIL)).thenReturn(Optional.of(existingAdmin));

            authService.oAuthLoginOrRegister(CODE);

            assertThat(existingAdmin.getProvider()).isNull();
        }

        @Test
        @DisplayName("검증된 이메일이면 기존 계정에 카카오를 연결하고 그 계정으로 로그인한다")
        void verifiedEmailLinksExistingAccount() {
            kakaoSays(VICTIM_EMAIL, true, true);
            when(userRepository.findByEmailAndDeletedAtIsNull(VICTIM_EMAIL)).thenReturn(Optional.of(existingAdmin));

            TokenDto tokens = authService.oAuthLoginOrRegister(CODE);

            assertThat(existingAdmin.getProvider()).isEqualTo("kakao");
            assertThat(existingAdmin.getProviderId()).isEqualTo(String.valueOf(KAKAO_ID));
            assertThat(existingAdmin.getRole()).isEqualTo(UserRole.ADMIN);
            assertThat(tokens.getIsNewUser())
                    .as("이메일로 이미 가입을 마친 계정이다. 가입 완료 화면으로 보내면 역할을 다시 고르게 된다")
                    .isFalse();
        }

        @Test
        @DisplayName("기존 계정에 연결하면 가입 완료로 표시해 가입 완료 절차로 역할을 바꿀 수 없게 한다")
        void linkingMarksRegistrationCompleted() {
            // 이메일 가입은 registrationCompleted 가 기본 false 다(@PrePersist).
            // 연결 뒤에도 false 로 두면 provider=kakao 가 되어 가입 완료 API 를 통과하고,
            // 관리자가 그 절차로 자기 역할을 PARENT 로 덮어쓸 수 있다.
            kakaoSays(VICTIM_EMAIL, true, true);
            when(userRepository.findByEmailAndDeletedAtIsNull(VICTIM_EMAIL)).thenReturn(Optional.of(existingAdmin));

            authService.oAuthLoginOrRegister(CODE);

            assertThat(existingAdmin.getRegistrationCompleted()).isTrue();
        }
    }

    @Nested
    @DisplayName("조회 순서")
    class LookupOrder {

        @Test
        @DisplayName("같은 카카오 계정이면 이메일이 바뀌어도 원래 계정으로 로그인한다")
        void providerIdWinsOverEmail() {
            User myKakaoAccount = User.builder()
                    .id(7L).userId("user_mine").email("kakao_old@example.com").name("나")
                    .role(UserRole.PARENT).provider("kakao").providerId(String.valueOf(KAKAO_ID))
                    .isActive(true).registrationCompleted(true).build();
            when(userRepository.findByProviderAndProviderIdAndDeletedAtIsNull("kakao", String.valueOf(KAKAO_ID)))
                    .thenReturn(Optional.of(myKakaoAccount));
            // 카카오 이메일을 남의 (검증된) 이메일로 바꿨다고 해도
            kakaoSays(VICTIM_EMAIL, true, true);
            when(userRepository.findByEmailAndDeletedAtIsNull(VICTIM_EMAIL)).thenReturn(Optional.of(existingAdmin));

            authService.oAuthLoginOrRegister(CODE);

            verify(jwtService).generateAccessToken(eq("user_mine"), anyString(), anyString(), any());
            assertThat(existingAdmin.getProvider()).isNull();
        }
    }

    @Nested
    @DisplayName("비활성 계정")
    class InactiveAccount {

        @Test
        @DisplayName("정지된 계정은 카카오로도 로그인할 수 없다")
        void deactivatedAccountIsRejected() {
            User banned = User.builder()
                    .id(9L).userId("user_banned").email("banned@example.com").name("정지")
                    .role(UserRole.PARENT).provider("kakao").providerId(String.valueOf(KAKAO_ID))
                    .isActive(false).registrationCompleted(true).build();
            when(userRepository.findByProviderAndProviderIdAndDeletedAtIsNull("kakao", String.valueOf(KAKAO_ID)))
                    .thenReturn(Optional.of(banned));
            kakaoSays("banned@example.com", true, true);

            assertThatThrownBy(() -> authService.oAuthLoginOrRegister(CODE))
                    .isInstanceOf(CareCodeException.class)
                    .satisfies(e -> assertThat(((CareCodeException) e).getErrorCode()).isEqualTo(ErrorCode.USER_INACTIVE));

            verify(jwtService, never()).generateAccessToken(anyString(), anyString(), anyString(), any());
            verify(refreshTokenStore, never()).register(anyString(), anyString());
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("신규 가입")
    class NewUser {

        @Test
        @DisplayName("처음 보는 카카오 계정은 일반 사용자로, 가입 미완료로 만든다")
        void createsParentPendingRegistration() {
            kakaoSays("new@example.com", true, true);
            when(userRepository.findByEmailAndDeletedAtIsNull("new@example.com")).thenReturn(Optional.empty());

            TokenDto tokens = authService.oAuthLoginOrRegister(CODE);

            User created = savedUser();
            assertThat(created.getRole()).isEqualTo(UserRole.PARENT);
            assertThat(created.getProvider()).isEqualTo("kakao");
            assertThat(created.getRegistrationCompleted()).isFalse();
            assertThat(created.getEmail()).isEqualTo("new@example.com");
            assertThat(created.getEmailVerified()).isTrue();
            assertThat(tokens.getIsNewUser()).isTrue();
        }

        @Test
        @DisplayName("이메일이 검증되지 않았으면 인증 완료로 표시하지 않는다")
        void unverifiedEmailIsNotMarkedVerified() {
            kakaoSays("maybe@example.com", true, false);
            when(userRepository.findByEmailAndDeletedAtIsNull(anyString())).thenReturn(Optional.empty());

            authService.oAuthLoginOrRegister(CODE);

            assertThat(savedUser().getEmailVerified()).isFalse();
        }

        @Test
        @DisplayName("카카오가 이메일을 주지 않으면 카카오 ID 로 대체 주소를 만든다")
        void fallbackEmail() {
            kakaoSays(null, null, null);
            when(userRepository.findByEmailAndDeletedAtIsNull(anyString())).thenReturn(Optional.empty());

            authService.oAuthLoginOrRegister(CODE);

            assertThat(savedUser().getEmail()).isEqualTo("kakao_" + KAKAO_ID + "@kakao.com");
        }
    }

    @Nested
    @DisplayName("입력 검증")
    class InputValidation {

        @Test
        @DisplayName("빈 인증 코드나 이상한 문자가 섞인 코드는 카카오에 보내지 않는다")
        void rejectsBadCode() {
            assertThatThrownBy(() -> authService.oAuthLoginOrRegister(" ")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> authService.oAuthLoginOrRegister("a b;c")).isInstanceOf(IllegalArgumentException.class);
            verify(kakaoUtil, never()).requestToken(anyString());
        }
    }
}
