package com.carecode.domain.user.service;

import com.carecode.core.analytics.EventLogger;
import com.carecode.domain.user.dto.response.UserDto;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.entity.UserRole;
import com.carecode.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 회원가입에서 서버가 정해야 하는 값에 대한 회귀 테스트.
 *
 * <p>{@code POST /auth/register} 는 permitAll 이다. 그런데 예전 구현은 요청 본문의
 * {@code role} 을 그대로 엔티티에 넣었다. 즉 <b>로그인조차 없이</b>
 * {@code {"role":"ADMIN"}} 으로 가입하면 그 자리에서 관리자가 됐다.
 * {@code provider} 도 클라이언트가 붙일 수 있어, 비밀번호 없이 임의 이메일·임의 providerId 로
 * 계정을 미리 만들면서 이메일 인증까지 통과한 것으로 표시할 수 있었다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserService - 회원가입 시 서버 결정 값")
class UserServiceSignUpTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RestTemplate restTemplate;
    @Mock private EventLogger eventLogger;

    @InjectMocks private UserService userService;

    @BeforeEach
    void setUp() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            u.setUserId("user_generated");
            return u;
        });
    }

    private UserDto.UserDtoBuilder signupRequest() {
        return UserDto.builder()
                .email("new@example.com")
                .password("secret123")
                .name("신규회원");
    }

    private User captureSaved() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        return captor.getValue();
    }

    @ParameterizedTest(name = "role={0} 으로 가입해도 PARENT 로 저장된다")
    @ValueSource(strings = {"ADMIN", "CAREGIVER", "GUEST", "USER"})
    @DisplayName("클라이언트가 보낸 role 은 무시된다")
    void clientSuppliedRoleIsIgnored(String requestedRole) {
        userService.createUser(signupRequest().role(requestedRole).build());

        assertThat(captureSaved().getRole()).isEqualTo(UserRole.PARENT);
    }

    @Test
    @DisplayName("role 을 아예 보내지 않아도 가입에 성공한다")
    void missingRoleIsFine() {
        // 예전에는 UserRole.valueOf(null) 이 터져 500 이 났다.
        UserDto created = userService.createUser(signupRequest().build());

        assertThat(created).isNotNull();
        assertThat(captureSaved().getRole()).isEqualTo(UserRole.PARENT);
    }

    @Test
    @DisplayName("클라이언트가 provider 를 붙여도 소셜 계정으로 만들어지지 않는다")
    void clientSuppliedProviderIsIgnored() {
        userService.createUser(signupRequest()
                .provider("kakao")
                .providerId("999999")
                .build());

        User saved = captureSaved();
        assertThat(saved.getProvider()).isNull();
        assertThat(saved.getProviderId()).isNull();
    }

    @Test
    @DisplayName("이메일 인증 여부는 항상 false 로 시작한다")
    void emailVerifiedStartsFalse() {
        userService.createUser(signupRequest()
                .provider("kakao")          // 예전에는 이것만으로 인증 완료 처리됐다
                .emailVerified(true)
                .build());

        assertThat(captureSaved().getEmailVerified()).isFalse();
    }

    @Test
    @DisplayName("provider 를 붙여 비밀번호를 건너뛸 수 없다")
    void passwordIsAlwaysRequired() {
        assertThatThrownBy(() -> userService.createUser(
                UserDto.builder()
                        .email("new@example.com")
                        .name("신규회원")
                        .provider("kakao")   // 예전에는 이 경우 비밀번호 검사를 건너뛰었다
                        .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("비밀번호");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("비밀번호는 해시로 저장된다")
    void passwordIsHashed() {
        userService.createUser(signupRequest().build());

        assertThat(captureSaved().getPassword())
                .isEqualTo("$2a$10$encoded")
                .isNotEqualTo("secret123");
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 거부한다")
    void rejectsDuplicateEmail() {
        when(userRepository.findByEmail("new@example.com"))
                .thenReturn(Optional.of(User.builder().id(9L).email("new@example.com").build()));

        assertThatThrownBy(() -> userService.createUser(signupRequest().build()))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).save(any(User.class));
    }
}
