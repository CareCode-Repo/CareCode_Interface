package com.carecode.domain.community.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 북마크 토글 결과.
 *
 * <p>{@code @JsonProperty} 가 필요한 이유는 {@link PostLikeToggleResponse} 와 같다.
 */
public record PostBookmarkToggleResponse(
        @JsonProperty("isBookmarked") boolean isBookmarked,
        long bookmarkCount) {
}
