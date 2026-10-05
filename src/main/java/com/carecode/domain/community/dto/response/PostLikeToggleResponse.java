package com.carecode.domain.community.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 좋아요 토글 결과.
 *
 * <p>누른 뒤 상태와 총 개수를 함께 준다. 화면이 자기 쪽에서 +1 하면 다른 기기에서 누른 것과
 * 어긋난다.
 *
 * <p>{@code @JsonProperty} 를 붙인 이유. 레코드는 컴포넌트 이름을 그대로 키로 쓰므로 지금은
 * 없어도 {@code isLiked} 로 나간다(확인함). 하지만 Lombok {@code @Getter} 를 쓰는 클래스로
 * 바꾸면 접근자가 {@code isLiked()} 가 되고 Jackson 이 {@code is} 를 떼어 키가 {@code liked} 로
 * 바뀐다 — 이 프로젝트에서 이미 {@code isRead}·{@code isAnonymous} 로 겪은 일이다. 프런트는
 * {@code isLiked} 를 {@code z.boolean()} 으로 필수로 읽으므로 그 순간 토글이 깨진다.
 * 키를 적어 두면 어느 쪽으로 바꿔도 같다.
 */
public record PostLikeToggleResponse(
        @JsonProperty("isLiked") boolean isLiked,
        long likeCount) {
}
