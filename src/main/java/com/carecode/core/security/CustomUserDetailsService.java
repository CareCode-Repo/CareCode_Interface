package com.carecode.core.security;

import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * {@link SecurityConfig}에서 등록하는 {@link UserDetailsService}.
 * Stateless JWT API가 기본이며, 이 빈은 필터 체인의 {@code userDetailsService()} 연동 및
 * 향후 폼/세션 인증 확장 시를 위한 이메일·비밀번호 조회용이다.
 * 소셜 전용 계정({@code password == null})은 로드 시 비밀번호가 없을 수 있으므로
 * 폼 로그인 경로에서는 별도 검증이 필요하다.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));
        String password = user.getPassword();
        if (password == null || password.isBlank()) {
            throw new UsernameNotFoundException("이메일·비밀번호 로그인을 지원하지 않는 계정입니다. 소셜 로그인을 이용해 주세요.");
        }
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(password)
                .roles(user.getRole().name())
                .disabled(!user.getIsActive())
                .build();
    }
} 