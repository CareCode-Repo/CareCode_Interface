package com.carecode.domain.user.service;

import com.carecode.core.analytics.EventLogger;
import com.carecode.domain.user.dto.request.SignUpRequest;
import com.carecode.domain.user.dto.response.UserDto;
import com.carecode.domain.user.entity.Gender;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.entity.UserRole;
import com.carecode.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
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
 * <p>{@code POST /auth/register} 는 permitAll 이다. 예전 구현은 응답 DTO 인 UserDto 를 요청으로 받아
 * 본문의 {@code role} 을 그대로 엔티티에 넣었다. 즉 <b>로그인조차 없이</b> {@code {"role":"ADMIN"}} 으로
 * 가입하면 관리자가 됐다(#82). 이제 요청 타입({@link SignUpRequest})에 그런 필드가 없다.
 *
 * <p>"요청에 role 을 넣어 보내도 무시된다" 는 실제 JSON 을 보내야 확인되므로
 * {@code SignUpContractTest}(통합)에 둔다. 여기서는 서비스가 고정하는 값을 확인한다.
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

    private SignUpRequest.SignUpRequestBuilder request() {
        return SignUpRequest.builder()
                .email("new@example.com")
                .password("secret123")
                .name("신규회원");
    }

    private User captureSaved() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("역할은 언제나 PARENT 로 저장된다")
    void roleIsAlwaysParent() {
        userService.createUser(request().build());

        assertThat(captureSaved().getRole()).isEqualTo(UserRole.PARENT);
    }

    @Test
    @DisplayName("소셜 계정으로 만들어지지 않는다")
    void notASocialAccount() {
        userService.createUser(request().build());

        User saved = captureSaved();
        assertThat(saved.getProvider()).isNull();
        assertThat(saved.getProviderId()).isNull();
    }

    @Test
    @DisplayName("이메일 인증 여부는 false 로 시작하고 계정은 활성이다")
    void startsUnverifiedAndActive() {
        userService.createUser(request().build());

        User saved = captureSaved();
        assertThat(saved.getEmailVerified()).isFalse();
        assertThat(saved.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("비밀번호는 해시로 저장된다")
    void passwordIsHashed() {
        userService.createUser(request().build());

        assertThat(captureSaved().getPassword())
                .isEqualTo("$2a$10$encoded")
                .isNotEqualTo("secret123");
    }

    @Test
    @DisplayName("비밀번호 없이 서비스를 직접 불러도 계정이 생기지 않는다")
    void passwordIsRequiredEvenWithoutControllerValidation() {
        assertThatThrownBy(() -> userService.createUser(request().password(" ").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("비밀번호");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 거부한다")
    void rejectsDuplicateEmail() {
        when(userRepository.findByEmail("new@example.com"))
                .thenReturn(Optional.of(User.builder().id(9L).email("new@example.com").build()));

        assertThatThrownBy(() -> userService.createUser(request().build()))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("선택 항목을 그대로 옮긴다")
    void mapsOptionalFields() {
        UserDto dto = userService.createUser(request()
                .phoneNumber("010-1234-5678")
                .birthDate(LocalDate.of(1990, 5, 1))
                .gender("FEMALE")
                .address("서울")
                .build());

        User saved = captureSaved();
        assertThat(saved.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(saved.getBirthDate()).isEqualTo(LocalDate.of(1990, 5, 1));
        assertThat(saved.getAddress()).isEqualTo("서울");
        assertThat(dto.getEmail()).isEqualTo("new@example.com");
    }
}
