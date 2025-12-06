package com.carecode.domain.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 게시글 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityPostResponse {
    private Long postId;
    private String title;
    private String content;
    private String category;
    private String authorName;
    private String authorId;
    private Boolean isAnonymous;
    private String createdAt;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private List<String> tags;
    private Boolean isLiked;
    private Boolean isBookmarked;
}

