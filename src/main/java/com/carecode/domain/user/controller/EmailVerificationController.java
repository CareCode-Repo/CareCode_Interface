package com.carecode.domain.user.controller;

import com.carecode.domain.user.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EmailVerificationController {
    private final EmailVerificationService emailVerificationService;

    @GetMapping("/users/verify")
    public String verifyEmail(@RequestParam String token) {
        boolean result = emailVerificationService.verifyEmail(token);
        return result ? "이메일 인증이 완료되었습니다." : "유효하지 않거나 만료된 토큰입니다.";
    }

    @PostMapping("/users/send-code")
    public void sendCode(@RequestParam String email) {
        emailVerificationService.sendVerificationCode(email);
    }

    @PostMapping("/users/verify-code")
    public boolean verifyCode(@RequestParam String email, @RequestParam String code) {
        return emailVerificationService.verifyCode(email, code);
    }
} 