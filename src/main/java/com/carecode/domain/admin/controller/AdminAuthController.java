package com.carecode.domain.admin.controller;

import com.carecode.core.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final CustomUserDetailsService customUserDetailsService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String adminLoginPage() {
        return "admin/login";
    }

    @PostMapping("/login")
    public String adminLogin(@RequestParam String username, 
                           @RequestParam String password,
                           RedirectAttributes redirectAttributes) {
        try {
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
            
            if (userDetails != null && passwordEncoder.matches(password, userDetails.getPassword())) {
                // ADMIN 권한 확인
                boolean isAdmin = userDetails.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
                
                if (!isAdmin) {
                    log.error("관리자 권한이 없는 사용자: {}", username);
                    redirectAttributes.addFlashAttribute("error", "관리자 권한이 필요합니다.");
                    return "redirect:/admin/login?error";
                }
                
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
                );
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("관리자 로그인 성공: {}", username);
                
                return "redirect:/admin/dashboard";
            } else {
                throw new RuntimeException("Invalid credentials");
            }
        } catch (Exception e) {
            log.error("관리자 로그인 실패: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "로그인에 실패했습니다.");
            return "redirect:/admin/login?error";
        }
    }
} 