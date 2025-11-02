package com.carecode.domain.community.entity;

/**
 * 게시글 카테고리 Enum
 */
public enum PostCategory {
    GENERAL("일반"),
    QUESTION("질문"),
    SHARE("공유"),
    REVIEW("후기"),
    NEWS("뉴스"),
    EVENT("이벤트"),
    NOTICE("공지사항");

    private final String displayName;

    PostCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
