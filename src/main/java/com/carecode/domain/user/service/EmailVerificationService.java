package com.carecode.domain.user.service;

import com.carecode.domain.user.entity.EmailVerificationToken;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.EmailVerificationTokenRepository;
import com.carecode.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final String CODE_KEY_PREFIX = "email:verify:";
    private static final String ATTEMPT_KEY_PREFIX = "email:verify:attempt:";
    private static final String COOLDOWN_KEY_PREFIX = "email:verify:cooldown:";

    /** 코드 유효 시간. 짧을수록 안전하지만 사용자가 메일함을 여는 시간은 줘야 한다. */
    private static final Duration CODE_TTL = Duration.ofMinutes(5);

    /** 한 코드에 허용하는 검증 시도 횟수. 6자리 코드를 무제한으로 맞춰볼 수 없게 한다. */
    private static final int MAX_VERIFY_ATTEMPTS = 5;

    /** 재발송 최소 간격. 같은 주소로 메일을 연타 발송하지 못하게 한다. */
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);

    /**
     * 인증번호는 보안 값이다. Math.random() 은 선형 합동 생성기라 이전 출력에서 다음 값을
     * 예측할 수 있어 인증 수단으로 쓰면 안 된다.
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * 인증 링크의 기준 주소.
     *
     * <p>예전에는 운영 서버 IP 와 포트가 소스에 박혀 있었고, 경로도 실제 매핑에 없는
     * {@code /users/verify} 였다. 즉 메일의 링크를 눌러도 인증이 되지 않았다.
     */
    @Value("${app.auth.email-verification.base-url:http://localhost:8082}")
    private String verificationBaseUrl;

    public void sendVerificationEmail(User user) {
        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .user(user)
                .token(token)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();
        tokenRepository.save(verificationToken);

        String subject = "[CareCode] 이메일 인증 안내";
        String text = "아래 링크를 클릭하여 이메일 인증을 완료해 주세요.\n"
                + trimTrailingSlash(verificationBaseUrl) + "/auth/verify?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setFrom(fromEmail);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    @Transactional
    public boolean verifyEmail(String token) {
        Optional<EmailVerificationToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) return false;
        EmailVerificationToken verificationToken = tokenOpt.get();
        if (verificationToken.isUsed() || verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return false;
        }
        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        verificationToken.setUsed(true);
        userRepository.save(user);
        tokenRepository.save(verificationToken);
        return true;
    }

    public void sendVerificationCode(String email) {
        // 재발송 쿨다운. 메일 발송은 비용이 들고, 수신자 입장에서는 그대로 스팸이 된다.
        String cooldownKey = COOLDOWN_KEY_PREFIX + email;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(cooldownKey, "1", RESEND_COOLDOWN);
        if (Boolean.FALSE.equals(acquired)) {
            throw new IllegalArgumentException("인증번호는 1분에 한 번만 요청할 수 있습니다.");
        }

        String code = generateCode();
        redisTemplate.opsForValue().set(CODE_KEY_PREFIX + email, code, CODE_TTL.toMinutes(), TimeUnit.MINUTES);
        // 새 코드를 냈으니 이전 코드에 쌓인 시도 횟수는 의미가 없다.
        redisTemplate.delete(ATTEMPT_KEY_PREFIX + email);

        String subject = "[CareCode] 이메일 인증번호 안내";
        String htmlContent =
            "<html><body style='font-family: Arial, sans-serif; background: #f9f9f9; padding: 24px;'>" +
            "<div style='max-width: 480px; margin: auto; background: #fff; border-radius: 8px; box-shadow: 0 2px 8px #eee; padding: 32px;'>" +
            "<h2 style='color: #4a90e2;'>CareCode 이메일 인증</h2>" +
            "<p>안녕하세요!<br>CareCode 서비스 이용을 위한 <b>이메일 인증번호</b>를 안내드립니다.</p>" +
            "<div style='margin: 24px 0; text-align: center;'>" +
            "<span style='display: inline-block; font-size: 2rem; letter-spacing: 8px; color: #222; background: #f3f6fa; padding: 12px 32px; border-radius: 6px; border: 1px solid #e0e0e0;'>" + code + "</span>" +
            "</div>" +
            "<ul style='color: #888; font-size: 0.95rem;'>" +
            "<li>인증번호는 <b>5분간</b>만 유효합니다.</li>" +
            "<li>타인에게 인증번호를 절대 알려주지 마세요.</li>" +
            "</ul>" +
            "<p style='margin-top: 32px; color: #aaa; font-size: 0.9rem;'>감사합니다.<br>CareCode 팀 드림</p>" +
            "</div></body></html>";
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setTo(email);
            helper.setFrom(fromEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            // 발송이 실패했는데 쿨다운만 남으면 사용자가 1분간 재시도조차 못 한다.
            redisTemplate.delete(cooldownKey);
            throw new RuntimeException("이메일 발송 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 인증번호 검증.
     *
     * <p>코드가 6자리 숫자라 후보가 90만 개뿐이다. 시도 횟수를 세지 않으면 유효 시간 5분 안에
     * 전수 조회가 가능하므로, 코드당 {@value #MAX_VERIFY_ATTEMPTS} 회를 넘기면 코드를 폐기한다.
     */
    public boolean verifyCode(String email, String code) {
        String codeKey = CODE_KEY_PREFIX + email;
        String attemptKey = ATTEMPT_KEY_PREFIX + email;

        String savedCode = redisTemplate.opsForValue().get(codeKey);
        if (savedCode == null) {
            return false;
        }

        Long attempts = redisTemplate.opsForValue().increment(attemptKey);
        if (attempts != null && attempts == 1L) {
            // 코드와 수명을 맞춰 둬야 카운터만 남아 다음 코드까지 막는 일이 없다.
            redisTemplate.expire(attemptKey, CODE_TTL);
        }
        if (attempts != null && attempts > MAX_VERIFY_ATTEMPTS) {
            log.warn("이메일 인증번호 시도 횟수 초과 - 코드를 폐기합니다. email={}", email);
            redisTemplate.delete(codeKey);
            redisTemplate.delete(attemptKey);
            return false;
        }

        // 코드 길이가 노출되지 않도록 상수 시간 비교를 쓴다.
        boolean matched = MessageDigest.isEqual(
                savedCode.getBytes(StandardCharsets.UTF_8),
                code == null ? new byte[0] : code.getBytes(StandardCharsets.UTF_8));

        if (matched) {
            redisTemplate.delete(codeKey);
            redisTemplate.delete(attemptKey);
            return true;
        }
        return false;
    }

    /** 000000~999999 를 균등하게 뽑는다. 앞자리가 0 이어도 6자리를 유지한다. */
    private String generateCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
