package com.carecode.domain.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 댓글 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityCommentResponse {
    private Long commentId;
    private String content;
    private String authorName;
    private String authorId;
    private String createdAt;
    private Integer likeCount;
    private Boolean isLiked;
    private Long parentCommentId;
    private List<CommunityCommentResponse> replies;
}

