package com.carecode.domain.user.entity;

/**
 * 사용자 역할 Enum
 */
public enum UserRole {
    PARENT("부모"),
    CAREGIVER("보육사"),
    ADMIN("관리자"),
    GUEST("게스트");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
