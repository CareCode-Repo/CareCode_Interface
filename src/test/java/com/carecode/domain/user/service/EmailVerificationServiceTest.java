package com.carecode.domain.user.service;

import com.carecode.domain.user.repository.EmailVerificationTokenRepository;
import com.carecode.domain.user.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 이메일 인증번호에 대한 회귀 테스트.
 *
 * <p>기존 구현의 문제는 세 가지였다.
 * <ul>
 *   <li>코드를 {@code Math.random()} 으로 만들었다. 예측 가능한 난수라 인증 수단으로 쓸 수 없다.</li>
 *   <li>검증 시도 횟수를 세지 않았다. 6자리 = 후보 90만 개라 유효시간 5분 안에 전수 조회가 가능했다.</li>
 *   <li>재발송 간격이 없었다. 같은 주소로 메일을 무한정 보낼 수 있었다.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EmailVerificationService - 인증번호")
class EmailVerificationServiceTest {

    private static final String EMAIL = "user@example.com";
    private static final String CODE_KEY = "email:verify:" + EMAIL;
    private static final String ATTEMPT_KEY = "email:verify:attempt:" + EMAIL;
    private static final String COOLDOWN_KEY = "email:verify:cooldown:" + EMAIL;

    @Mock private EmailVerificationTokenRepository tokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private JavaMailSender mailSender;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(mailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));
        ReflectionTestUtils.setField(service, "fromEmail", "noreply@example.com");
        ReflectionTestUtils.setField(service, "verificationBaseUrl", "https://carecode.example.com");
    }

    private void cooldownFree() {
        when(valueOperations.setIfAbsent(eq(COOLDOWN_KEY), anyString(), any(Duration.class))).thenReturn(true);
    }

    @Nested
    @DisplayName("발송")
    class Sending {

        @Test
        @DisplayName("인증번호는 항상 6자리 숫자다")
        void codeIsSixDigits() {
            cooldownFree();

            service.sendVerificationCode(EMAIL);

            ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
            verify(valueOperations).set(eq(CODE_KEY), code.capture(), anyLong(), eq(TimeUnit.MINUTES));

            assertThat(code.getValue()).matches("\\d{6}");
        }

        @Test
        @DisplayName("연속으로 뽑은 인증번호가 서로 다르다")
        void codesVary() {
            cooldownFree();

            java.util.Set<String> codes = new java.util.HashSet<>();
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

            for (int i = 0; i < 20; i++) {
                service.sendVerificationCode(EMAIL);
            }
            verify(valueOperations, times(20)).set(eq(CODE_KEY), captor.capture(), anyLong(), eq(TimeUnit.MINUTES));
            codes.addAll(captor.getAllValues());

            // 20번 뽑아서 전부 같은 값이 나오면 난수원이 고장난 것이다.
            assertThat(codes).hasSizeGreaterThan(1);
        }

        @Test
        @DisplayName("쿨다운 중이면 재발송하지 않는다")
        void respectsCooldown() {
            when(valueOperations.setIfAbsent(eq(COOLDOWN_KEY), anyString(), any(Duration.class))).thenReturn(false);

            assertThatThrownBy(() -> service.sendVerificationCode(EMAIL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("1분에 한 번");

            verify(mailSender, never()).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("새 코드를 내면 이전 코드의 시도 횟수는 초기화된다")
        void resetsAttemptCounterOnNewCode() {
            cooldownFree();

            service.sendVerificationCode(EMAIL);

            verify(redisTemplate).delete(ATTEMPT_KEY);
        }

        @Test
        @DisplayName("발송이 실패하면 쿨다운을 풀어 사용자가 다시 시도할 수 있게 한다")
        void releasesCooldownWhenSendFails() {
            cooldownFree();
            org.mockito.Mockito.doThrow(new RuntimeException("SMTP down"))
                    .when(mailSender).send(any(MimeMessage.class));

            assertThatThrownBy(() -> service.sendVerificationCode(EMAIL))
                    .isInstanceOf(RuntimeException.class);

            verify(redisTemplate).delete(COOLDOWN_KEY);
        }
    }

    @Nested
    @DisplayName("검증")
    class Verifying {

        @Test
        @DisplayName("일치하면 통과하고 코드를 소모한다")
        void acceptsMatchingCode() {
            when(valueOperations.get(CODE_KEY)).thenReturn("123456");
            when(valueOperations.increment(ATTEMPT_KEY)).thenReturn(1L);

            assertThat(service.verifyCode(EMAIL, "123456")).isTrue();

            verify(redisTemplate).delete(CODE_KEY);
            verify(redisTemplate).delete(ATTEMPT_KEY);
        }

        @Test
        @DisplayName("불일치하면 실패하되 코드는 남겨 사용자가 재시도할 수 있다")
        void rejectsWrongCodeButKeepsIt() {
            when(valueOperations.get(CODE_KEY)).thenReturn("123456");
            when(valueOperations.increment(ATTEMPT_KEY)).thenReturn(2L);

            assertThat(service.verifyCode(EMAIL, "000000")).isFalse();

            verify(redisTemplate, never()).delete(CODE_KEY);
        }

        @Test
        @DisplayName("시도 횟수를 넘기면 코드를 폐기한다")
        void discardsCodeAfterTooManyAttempts() {
            when(valueOperations.get(CODE_KEY)).thenReturn("123456");
            when(valueOperations.increment(ATTEMPT_KEY)).thenReturn(6L);

            // 정답을 넣어도 통과하지 않는다. 무차별 대입 중이라고 봐야 한다.
            assertThat(service.verifyCode(EMAIL, "123456")).isFalse();

            verify(redisTemplate).delete(CODE_KEY);
            verify(redisTemplate).delete(ATTEMPT_KEY);
        }

        @Test
        @DisplayName("발급된 코드가 없으면 시도 횟수도 세지 않는다")
        void noCodeMeansNoAttemptCount() {
            when(valueOperations.get(CODE_KEY)).thenReturn(null);

            assertThat(service.verifyCode(EMAIL, "123456")).isFalse();

            verify(valueOperations, never()).increment(anyString());
        }

        @Test
        @DisplayName("null 코드를 넣어도 예외 없이 실패한다")
        void handlesNullInput() {
            when(valueOperations.get(CODE_KEY)).thenReturn("123456");
            when(valueOperations.increment(ATTEMPT_KEY)).thenReturn(1L);

            assertThatCode(() -> assertThat(service.verifyCode(EMAIL, null)).isFalse())
                    .doesNotThrowAnyException();
        }
    }
}
