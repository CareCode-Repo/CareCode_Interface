package com.carecode.domain.community.dto.response;

/** 게시글 좋아요 수. 내가 눌렀는지는 포함하지 않는다(비로그인도 볼 수 있는 값이다). */
public record PostLikeCountResponse(long likeCount) {
}
