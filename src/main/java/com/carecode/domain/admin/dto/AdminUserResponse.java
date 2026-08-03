package com.carecode.domain.admin.dto;

import com.carecode.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 어드민 사용자 목록/상세 응답.
 * <p>비밀번호 해시, OAuth provider ID 등 밖으로 나가면 안 되는 값은 담지 않는다.
 */
@Getter
@Builder
public class AdminUserResponse {

    private final Long id;
    private final String userId;
    private final String email;
    private final String name;
    private final String phoneNumber;
    private final String role;
    private final Boolean isActive;
    private final Boolean emailVerified;
    private final LocalDateTime lastLoginAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime deletedAt;

    public static AdminUserResponse from(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .isActive(user.getIsActive())
                .emailVerified(user.getEmailVerified())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .deletedAt(user.getDeletedAt())
                .build();
    }
}
