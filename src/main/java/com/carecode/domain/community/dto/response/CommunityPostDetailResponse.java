package com.carecode.domain.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 게시글 상세 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommunityPostDetailResponse extends CommunityPostResponse {
    private List<CommunityCommentResponse> comments;
    private List<CommunityRelatedPostResponse> relatedPosts;
}

