package com.carecode.core.security;

import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** SecurityConfig에서 등록하는 UserDetailsService. Stateless JWT API가 기본이며 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 탈퇴(soft delete)한 계정이 로그인되지 않도록 deletedAt 조건을 포함해 조회한다.
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
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