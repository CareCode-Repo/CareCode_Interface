package com.carecode.domain.user.service;

import com.carecode.domain.user.entity.EmailVerificationToken;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.EmailVerificationTokenRepository;
import com.carecode.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.concurrent.TimeUnit;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${spring.mail.username}")
    private String fromEmail;

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
        String text = "아래 링크를 클릭하여 이메일 인증을 완료해 주세요.\n" +
                "http://localhost:8080/users/verify?token=" + token;

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
        String code = String.valueOf((int)(Math.random() * 900000) + 100000); // 6자리 숫자
        redisTemplate.opsForValue().set("email:verify:" + email, code, 5, TimeUnit.MINUTES);
        // HTML 이메일로 code 발송
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
            throw new RuntimeException("이메일 발송 중 오류가 발생했습니다.", e);
        }
    }

    public boolean verifyCode(String email, String code) {
        String key = "email:verify:" + email;
        String savedCode = redisTemplate.opsForValue().get(key);
        if (savedCode != null && savedCode.equals(code)) {
            redisTemplate.delete(key);
            // userRepository에서 emailVerified=true 처리
            userRepository.findByEmail(email).ifPresent(user -> {
                user.setEmailVerified(true);
                userRepository.save(user);
            });
            return true;
        }
        return false;
    }
} 