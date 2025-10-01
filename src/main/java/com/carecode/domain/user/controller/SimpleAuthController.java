package com.carecode.domain.user.controller;

import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", allowedHeaders = "*") // CORS 허용
public class SimpleAuthController {

    private final AuthService authService;

    // 카카오 관련 API는 KakaoAuthController로 이동됨

}
