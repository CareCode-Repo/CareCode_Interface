package com.carecode.domain.community.entity;

/**
 * 게시글 상태 Enum
 */
public enum PostStatus {
    DRAFT("임시저장"),
    PUBLISHED("발행"),
    HIDDEN("숨김"),
    DELETED("삭제");

    private final String displayName;

    PostStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
